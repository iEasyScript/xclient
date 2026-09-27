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

    /** Null until the site has answered at least once. */
    @Getter
    private volatile Identity identity;

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
        String token = System.getProperty("projectx.accountToken");
        if (Strings.isNullOrEmpty(token))
        {
            token = System.getenv("PROJECTX_ACCOUNT_TOKEN");
        }
        if (Strings.isNullOrEmpty(token) && configManager != null)
        {
            try
            {
                token = configManager.getConfiguration(ProjectXConfig.configGroup, ProjectXConfig.keyAccountToken);
            }
            catch (Exception e)
            {
                // The config is not loaded during early start-up.
                return null;
            }
        }
        return Strings.isNullOrEmpty(token) ? null : token.trim();
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
    public Identity refresh()
    {
        String token = currentToken();
        if (token == null)
        {
            identity = null;
            return null;
        }

        Request request = new Request.Builder()
            .url(ProjectXSite.api("me"))
            .header("Authorization", "Bearer " + token)
            .build();

        try (Response response = okHttpClient.newCall(request).execute())
        {
            if (response.code() == 401 || response.code() == 403)
            {
                log.warn("Project X API token was rejected; signed out");
                identity = null;
                return null;
            }

            if (!response.isSuccessful() || response.body() == null)
            {
                log.debug("Could not read the Project X account: HTTP {}", response.code());
                return identity;
            }

            Identity parsed = gson.fromJson(response.body().charStream(), Identity.class);
            if (parsed != null)
            {
                identity = parsed;
            }
            return identity;
        }
        catch (IOException | JsonSyntaxException e)
        {
            log.debug("Could not reach the Project X site for the account: {}", e.getMessage());
            return identity;
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
