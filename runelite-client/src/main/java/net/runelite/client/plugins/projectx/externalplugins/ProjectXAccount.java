/*
 * Copyright (c) 2026 ProjectX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.projectx.externalplugins;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Who is signed in, and what they are allowed to see.
 *
 * <p>The API token is the client's whole notion of identity. It normally arrives
 * from the launcher, which obtained it by sending the user through a Discord
 * sign-in on the website; a user running the jar directly can still paste one
 * into the Project X settings.
 *
 * <p>Resolved in order: {@code -Dprojectx.accountToken}, the
 * {@code PROJECTX_ACCOUNT_TOKEN} environment variable, then the "API token"
 * setting. The environment comes before the setting because the launcher owns
 * the session: signing out there must not leave a stale token behind in a config
 * file that would keep working.
 *
 * <p>The token is deliberately <em>not</em> read from the command line. Arguments
 * are visible to every other process on the machine; the environment of a
 * process is not.
 */
@Slf4j
@Singleton
public class ProjectXAccount
{
    /** Roles the site can report. Anything unrecognised is treated as USER. */
    public enum Role
    {
        USER,
        DEVELOPER,
        ADMIN
    }

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ConfigManager configManager;

    /**
     * How long an answer from the site is trusted before asking again.
     *
     * <p>Matches the cache header {@code /api/v1/me} sets, so re-asking at this
     * interval costs nothing when the answer has not changed.
     */
    private static final long REFRESH_INTERVAL_SECONDS = 60;

    /** What was last said about the sign-in state, so it is said once and not every minute. */
    private enum Reported
    {
        NOTHING,
        NO_TOKEN,
        REJECTED,
        SIGNED_IN
    }

    /** Null until the site has answered at least once. */
    @Getter
    private volatile Identity identity;

    private volatile long lastRefreshMillis;
    private volatile Reported reported = Reported.NOTHING;

    /**
     * The token the site has actually accepted, once one has been.
     *
     * <p>Static because {@link #token(ConfigManager)} is what every other caller
     * -- entitlements, jar downloads -- uses to build its Authorization header,
     * and they must all send the token this class signed in with. Otherwise the
     * client can be signed in on one token while asking for a customer's paid
     * scripts with another.
     */
    private static volatile String provenToken;

    @Inject
    private ProjectXAccount(OkHttpClient okHttpClient, Gson gson, ConfigManager configManager)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
        this.configManager = configManager;
    }

    /**
     * The token to authenticate with, or null when nobody is signed in.
     *
     * <p>Static so the pieces that run before injection -- and the ones that only
     * need a header -- do not each have to reimplement the precedence above.
     */
    public static String token(ConfigManager configManager)
    {
        List<String> candidates = candidates(configManager);
        if (candidates.isEmpty())
        {
            return null;
        }

        /*
         * A token that has been accepted beats one that merely ranks higher.
         *
         * The launcher's token normally wins, and should: it owns the session,
         * and signing out there must not fall back to a stale token in a config
         * file. But when the launcher hands over a token the site refuses --
         * revoked, or belonging to an account that has since been replaced --
         * ranking alone leaves the client signed out while a perfectly good
         * token sits in the settings, and the customer's paid scripts will not
         * run. The proven token still has to be one of the current candidates,
         * so removing it really does sign the user out.
         */
        String proven = provenToken;
        if (proven != null && candidates.contains(proven))
        {
            return proven;
        }
        return candidates.get(0);
    }

    /** Every token this machine offers, best-ranked first, without duplicates. */
    private static List<String> candidates(ConfigManager configManager)
    {
        Set<String> ordered = new LinkedHashSet<>();
        add(ordered, System.getProperty("projectx.accountToken"));
        add(ordered, System.getenv("PROJECTX_ACCOUNT_TOKEN"));

        if (configManager != null)
        {
            try
            {
                add(ordered, configManager.getConfiguration(
                    ProjectXConfig.configGroup, ProjectXConfig.keyAccountToken));
            }
            catch (Exception e)
            {
                // The config is not loaded during early start-up.
            }
        }
        return new ArrayList<>(ordered);
    }

    private static void add(Set<String> into, String token)
    {
        if (!Strings.isNullOrEmpty(token))
        {
            into.add(token.trim());
        }
    }

    /** As {@link #token(ConfigManager)}, for callers with no ConfigManager to hand. */
    public static String token()
    {
        return token(ProjectX.getConfigManager());
    }

    public String currentToken()
    {
        return token(configManager);
    }

    /**
     * Asks the site who this token belongs to and caches the answer.
     *
     * <p>A failure leaves the previous identity in place: losing the network
     * should not silently demote a developer mid-session. A token the site
     * actively rejects does clear it, because that token is no longer anyone.
     */
    /**
     * Refreshes only when the cached answer has gone stale, or when there is a
     * token but no identity yet.
     *
     * <p>The identity used to be fetched exactly once, at start-up. That made a
     * single failed request permanent for the session: a customer who opened the
     * client while the site was restarting, or before their network was up, was
     * treated as signed out until they restarted -- and the paid plugins they
     * had bought would not run. The second condition is what fixes that, because
     * an unknown identity is retried rather than accepted.
     */
    public Identity refreshIfStale()
    {
        boolean stale = System.currentTimeMillis() - lastRefreshMillis
            >= TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS);
        boolean unresolved = identity == null && currentToken() != null;

        if (stale || unresolved)
        {
            return refresh();
        }
        return identity;
    }

    /**
     * Says what changed about the sign-in state, once per change.
     *
     * <p>Called on every refresh, so it has to stay quiet while nothing moves:
     * the point is that a customer whose token stops working gets a line in the
     * log saying so, not that the log fills up with the same line every minute.
     */
    private void report(Reported now)
    {
        if (reported == now)
        {
            return;
        }
        reported = now;

        switch (now)
        {
            case NO_TOKEN:
                log.info("Not signed in to Project X. Sign in through the launcher, or paste an "
                    + "API token into the Project X settings. Paid scripts will not run until then.");
                break;
            case REJECTED:
                log.warn("The Project X API token was rejected, so paid scripts cannot run. "
                    + "It was probably revoked -- sign in again in the launcher to get a new one.");
                break;
            case SIGNED_IN:
                Identity current = identity;
                log.info("Signed in to Project X as {} ({})",
                    current == null ? "?" : current.displayName(),
                    current == null ? Role.USER : current.role());
                break;
            default:
                break;
        }
    }

    public Identity refresh()
    {
        lastRefreshMillis = System.currentTimeMillis();

        List<String> candidates = candidates(configManager);
        if (candidates.isEmpty())
        {
            identity = null;
            provenToken = null;
            report(Reported.NO_TOKEN);
            return null;
        }

        for (int i = 0; i < candidates.size(); i++)
        {
            String candidate = candidates.get(i);
            switch (ask(candidate))
            {
                case ACCEPTED:
                    if (i > 0)
                    {
                        log.warn("The preferred Project X token was rejected; signed in with a "
                            + "lower-ranked one instead. Sign in again in the launcher to "
                            + "replace the dead one.");
                    }
                    provenToken = candidate;
                    report(Reported.SIGNED_IN);
                    return identity;

                case REJECTED:
                    continue;

                default:
                    // Unreachable rather than refused: nothing has been learned
                    // about any token, so the previous answer stands. Losing the
                    // network must not sign a paying customer out.
                    return identity;
            }
        }

        identity = null;
        provenToken = null;
        report(Reported.REJECTED);
        return null;
    }

    private enum Outcome
    {
        ACCEPTED,
        REJECTED,
        UNREACHABLE
    }

    /** Asks the site about one token, setting {@link #identity} if it is accepted. */
    private Outcome ask(String token)
    {
        Request request = new Request.Builder()
            .url(ProjectXSite.api("me"))
            .header("Authorization", "Bearer " + token)
            .build();

        try (Response response = okHttpClient.newCall(request).execute())
        {
            if (response.code() == 401 || response.code() == 403)
            {
                return Outcome.REJECTED;
            }

            if (!response.isSuccessful() || response.body() == null)
            {
                log.debug("Could not read the Project X account: HTTP {}", response.code());
                return Outcome.UNREACHABLE;
            }

            Identity parsed = gson.fromJson(response.body().charStream(), Identity.class);
            if (parsed == null)
            {
                return Outcome.UNREACHABLE;
            }
            identity = parsed;
            return Outcome.ACCEPTED;
        }
        catch (IOException | JsonSyntaxException e)
        {
            log.debug("Could not reach the Project X site for the account: {}", e.getMessage());
            return Outcome.UNREACHABLE;
        }
    }

    public Role role()
    {
        Identity current = identity;
        return current == null ? Role.USER : current.role();
    }

    /**
     * Whether developer-only tooling should be offered.
     *
     * <p>Not a security boundary. It runs on a machine the user controls, so it
     * decides what the client <em>shows</em>, nothing more -- anything that must
     * actually be enforced is enforced by the site.
     */
    public boolean isDeveloper()
    {
        Role role = role();
        return role == Role.DEVELOPER || role == Role.ADMIN;
    }

    /** The subset of {@code GET /api/v1/me} the client has any use for. */
    public static class Identity
    {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("discordUsername")
        private String discordUsername;

        @SerializedName("role")
        private String role;

        @SerializedName("tokenBalance")
        private Integer tokenBalance;

        /** Spendable X Tokens, or null if the site did not say. */
        public Integer tokenBalance()
        {
            return tokenBalance;
        }

        public String id()
        {
            return id;
        }

        public String displayName()
        {
            if (!Strings.isNullOrEmpty(discordUsername))
            {
                return discordUsername;
            }
            return Strings.isNullOrEmpty(name) ? "Signed in" : name;
        }

        /** An unknown role is the least privileged one, never the most. */
        public Role role()
        {
            if (role == null)
            {
                return Role.USER;
            }
            try
            {
                return Role.valueOf(role.trim().toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                log.debug("Unknown Project X role '{}'; treating as USER", role);
                return Role.USER;
            }
        }
    }
}
