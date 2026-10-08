package net.runelite.client.plugins.projectx.externalplugins;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Decides whether a marketplace (paid) plugin may run.
 *
 * Only plugins the website marks {@code paid} are gated; everything else in the
 * hub is free. A paid plugin may start only while the user holds a live
 * subscription, and is stopped the moment that subscription runs out.
 *
 * <p>The site's answer is cached as an expiry instant per plugin. That lets
 * {@link #mayStart} answer on the EDT without touching the network, keeps a
 * running script alive through a brief outage, and still ends access on time
 * offline because expiry is compared against the local clock.
 *
 * <p>A free trial is stricter: it lasts only while the site keeps answering (see
 * {@link #TRIAL_OFFLINE_GRACE_NANOS}) and is timed on the monotonic clock, so
 * neither blocking the site nor setting the PC clock back stretches the hour.
 *
 * <p>This raises the cost of piracy; it cannot prevent it. Anyone can patch the
 * check out of a jar they already have.
 */
@Slf4j
@Singleton
public class ProjectXEntitlements
{
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** How often running paid plugins are re-verified against the site. */
    private static final long CHECK_INTERVAL_SECONDS = 60;
    /** How often the site is actually asked; checks in between use the cached expiry. */
    private static final long REFRESH_INTERVAL_SECONDS = 5 * 60;
    /**
     * A free trial lasts only while the site keeps confirming it. Paid access rides
     * out an outage on its cached expiry; a trial stops once the site has not answered
     * for this long. Two missed one-minute checks plus some slack, so one dropped
     * request does not end somebody's hour.
     */
    private static final long TRIAL_OFFLINE_GRACE_NANOS = TimeUnit.SECONDS.toNanos(150);

    // Remembered across restarts, so a paid jar cannot run just because the hub
    // happened to be unreachable when the client started.
    private static final String PAID_PLUGINS_KEY = "paidPlugins";

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ConfigManager configManager;
    private final PluginManager pluginManager;
    private final ScheduledExecutorService executor;
    private final Notifier notifier;

    /**
     * What the last question to the site produced.
     *
     * Needed because two very different things used to look identical from here: a
     * customer who has not subscribed, and a customer whose check did not happen.
     * Both left the expiry cache empty, so both were told to go and buy a script --
     * which, for somebody who had already bought it, is the client calling them a
     * liar because it could not reach us.
     */
    private enum Check
    {
        /** The site answered, and the cache is now what it says. */
        ANSWERED,
        /** The site rejected our token. Nothing is proven, so nothing may run. */
        REJECTED,
        /** We could not get an answer. What is cached is all we know. */
        UNREACHABLE,
    }

    private final Map<String, Instant> expiries = new ConcurrentHashMap<>();
    /**
     * Plugins held on a free trial, and when each trial ends on {@link System#nanoTime()}.
     *
     * Timed on the monotonic clock from the site's own reckoning of how long is left,
     * not by comparing the end against the wall clock: setting the PC clock back
     * would otherwise stretch the hour for as long as the site could not be reached.
     */
    private final Map<String, Long> trialDeadlines = new ConcurrentHashMap<>();
    /** When the site last gave a real answer, on {@link System#nanoTime()}. */
    private volatile long lastAnsweredNanos;
    /** {@link System#nanoTime()}; replaced in tests so a trial's hour can pass in a millisecond. */
    LongSupplier nanoClock = System::nanoTime;
    private final Map<String, String> storeUrls = new ConcurrentHashMap<>();
    private final Set<String> paid = ConcurrentHashMap.newKeySet();
    private volatile long lastRefreshMillis;

    @Inject
    ProjectXEntitlements(
            OkHttpClient okHttpClient,
            Gson gson,
            ConfigManager configManager,
            PluginManager pluginManager,
            ScheduledExecutorService executor,
            Notifier notifier)
    {
        this.okHttpClient = okHttpClient;
        this.gson = gson;
        this.configManager = configManager;
        this.pluginManager = pluginManager;
        this.executor = executor;
        this.notifier = notifier;
    }

    /**
     * Loads what is known, asks the site once, and installs the start gate. Call
     * after the manifest has loaded and before plugins start.
     */
    public void start(Collection<ProjectXPluginManifest> manifests)
    {
        String stored = configManager.getConfiguration(ProjectXConfig.configGroup, PAID_PLUGINS_KEY);
        if (!Strings.isNullOrEmpty(stored))
        {
            Splitter.on(',').omitEmptyStrings().trimResults().split(stored).forEach(paid::add);
        }
        updatePaidPlugins(manifests);

        refresh();

        pluginManager.setStartGate(this::mayStart);
        executor.scheduleWithFixedDelay(this::enforce, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Records which plugins are sold on the marketplace. Paid is sticky: the site can add, not silently remove. */
    public void updatePaidPlugins(Collection<ProjectXPluginManifest> manifests)
    {
        for (ProjectXPluginManifest manifest : manifests)
        {
            if (manifest.isPaid())
            {
                paid.add(manifest.getInternalName());
                if (!Strings.isNullOrEmpty(manifest.getStoreUrl()))
                {
                    storeUrls.put(manifest.getInternalName(), manifest.getStoreUrl());
                }
            }
        }
        configManager.setConfiguration(ProjectXConfig.configGroup, PAID_PLUGINS_KEY, String.join(",", new TreeSet<>(paid)));
    }

    public boolean isPaid(String internalName)
    {
        return paid.contains(internalName);
    }

    /** When access to a paid plugin ends, as last heard from the site; empty if none is known. */
    public Optional<Instant> accessEndsAt(String internalName)
    {
        return Optional.ofNullable(expiries.get(internalName));
    }

    /** Whether the access {@link #accessEndsAt} reports is a free trial. */
    public boolean isOnTrial(String internalName)
    {
        return trialDeadlines.containsKey(internalName);
    }

    public boolean isEntitled(String internalName)
    {
        Long trialEnd = trialDeadlines.get(internalName);
        if (trialEnd != null)
        {
            long now = nanoClock.getAsLong();
            return now - trialEnd < 0 && now - lastAnsweredNanos <= TRIAL_OFFLINE_GRACE_NANOS;
        }
        Instant expiry = expiries.get(internalName);
        return expiry != null && Instant.now().isBefore(expiry);
    }

    /**
     * Start gate for {@link PluginManager#startPlugin}. Runs on the EDT, so it
     * only reads the cache. A denied plugin is disabled and re-checked in the
     * background, so a purchase made a moment ago starts it without a restart.
     */
    boolean mayStart(Plugin plugin)
    {
        String internalName = internalName(plugin);
        if (internalName == null || !isPaid(internalName) || isEntitled(internalName))
        {
            return true;
        }

        pluginManager.setPluginEnabled(plugin, false);
        executor.submit(() ->
        {
            Check check = refresh();
            if (isEntitled(internalName))
            {
                SwingUtilities.invokeLater(() -> startNow(plugin));
                return;
            }

            if (check == Check.UNREACHABLE)
            {
                // Do not send somebody to a shop to buy what they may already own.
                tellUser(plugin, internalName,
                        "could not start: we could not reach Project X to check your access."
                                + " It will start on its own once we can.");
                return;
            }

            tellUser(plugin, internalName, "needs active access to run.");
        });
        return false;
    }

    /** Stops any running paid plugin whose access has ended. */
    private void enforce()
    {
        try
        {
            // A trial is confirmed every check, which is what lets it end soon after
            // the site stops answering; paid access is re-asked every few minutes.
            boolean onTrial = !trialDeadlines.isEmpty();
            boolean unreachable = false;
            if (onTrial || System.currentTimeMillis() - lastRefreshMillis >= TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS))
            {
                unreachable = refresh() == Check.UNREACHABLE;
            }

            List<Plugin> expired = new ArrayList<>();
            for (Plugin plugin : pluginManager.getPlugins())
            {
                String internalName = internalName(plugin);
                if (internalName == null || !isPaid(internalName) || isEntitled(internalName)
                        || !pluginManager.isPluginActive(plugin))
                {
                    continue;
                }
                // Stopping somebody's paid script because we could not reach ourselves
                // is the one outcome worse than briefly letting a lapsed one run, so an
                // outage stops only trials -- which need the site by design.
                if (unreachable && !trialDeadlines.containsKey(internalName))
                {
                    continue;
                }
                expired.add(plugin);
            }

            for (Plugin plugin : expired)
            {
                ProjectXSessionTracker.markStop(plugin, ProjectXSessionTracker.End.ACCESS_ENDED);
                SwingUtilities.invokeLater(() ->
                {
                    try
                    {
                        pluginManager.setPluginEnabled(plugin, false);
                        pluginManager.stopPlugin(plugin);
                    }
                    catch (PluginInstantiationException e)
                    {
                        log.warn("Unable to stop expired plugin {}", plugin.getClass().getSimpleName(), e);
                    }
                });
                String internalName = internalName(plugin);
                Long trialEnd = trialDeadlines.get(internalName);
                tellUser(plugin, internalName, trialEnd == null
                        ? "has been stopped: your access has ended."
                        : nanoClock.getAsLong() - trialEnd >= 0
                                ? "has been stopped: your free trial has ended."
                                : "has been stopped: a free trial needs a connection to Project X, and we could not reach it.");
            }
        }
        catch (Exception e)
        {
            log.warn("Entitlement check failed", e);
        }
    }

    /**
     * Asks the site which paid plugins this user may run. A failure keeps the
     * cached expiries, which still lapse on their own.
     */
    public Check refresh()
    {
        if (paid.isEmpty())
        {
            return Check.ANSWERED;
        }

        String token = ProjectXAccount.token(configManager);
        if (Strings.isNullOrEmpty(token))
        {
            expiries.clear();
            trialDeadlines.clear();
            lastRefreshMillis = System.currentTimeMillis();
            return Check.REJECTED;
        }

        JsonObject body = new JsonObject();
        JsonArray names = new JsonArray();
        new TreeSet<>(paid).forEach(names::add);
        body.add("internalNames", names);

        Request request = new Request.Builder()
                .url(ProjectXSite.api("entitlements"))
                .header("Authorization", "Bearer " + token.trim())
                .post(RequestBody.create(JSON, gson.toJson(body)))
                .build();

        try (Response response = okHttpClient.newCall(request).execute())
        {
            lastRefreshMillis = System.currentTimeMillis();
            if (response.code() == 401)
            {
                // The token was revoked or mistyped: nothing is proven, so nothing runs.
                log.warn("Project X API token was rejected; paid scripts are unavailable");
                expiries.clear();
                trialDeadlines.clear();
                return Check.REJECTED;
            }
            if (!response.isSuccessful() || response.body() == null)
            {
                // Anything else is our fault, not the user's. Keep what is cached:
                // an access period that is still running should survive us having a
                // bad minute, and one that has genuinely ended still lapses on time.
                log.warn("Entitlement check failed: HTTP {}; keeping {} cached entitlements",
                        response.code(), expiries.size());
                return Check.UNREACHABLE;
            }

            long answeredNanos = nanoClock.getAsLong();
            JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
            JsonElement checkedAtElement = json.get("checkedAt");
            Instant checkedAt = checkedAtElement != null && !checkedAtElement.isJsonNull()
                    ? Instant.parse(checkedAtElement.getAsString())
                    : null;
            for (JsonElement element : json.getAsJsonArray("results"))
            {
                JsonObject result = element.getAsJsonObject();
                String internalName = result.get("internalName").getAsString();
                JsonElement expiresAt = result.get("expiresAt");
                JsonElement trial = result.get("trial");
                if (result.get("active").getAsBoolean() && expiresAt != null && !expiresAt.isJsonNull())
                {
                    Instant expiry = Instant.parse(expiresAt.getAsString());
                    expiries.put(internalName, expiry);
                    if (trial != null && !trial.isJsonNull() && trial.getAsBoolean() && checkedAt != null)
                    {
                        // How long the site says is left, counted from now on our own
                        // monotonic clock.
                        long remaining = Math.max(0L, Duration.between(checkedAt, expiry).toNanos());
                        trialDeadlines.put(internalName, answeredNanos + remaining);
                    }
                    else
                    {
                        trialDeadlines.remove(internalName);
                    }
                }
                else
                {
                    expiries.remove(internalName);
                    trialDeadlines.remove(internalName);
                }
            }

            lastAnsweredNanos = answeredNanos;
            return Check.ANSWERED;
        }
        catch (IOException | JsonParseException | DateTimeParseException | IllegalStateException | NullPointerException e)
        {
            log.warn("Entitlement check failed: {}; keeping {} cached entitlements",
                    e.getMessage(), expiries.size());
            return Check.UNREACHABLE;
        }
    }

    private void startNow(Plugin plugin)
    {
        try
        {
            pluginManager.setPluginEnabled(plugin, true);
            pluginManager.startPlugin(plugin);
        }
        catch (PluginInstantiationException e)
        {
            log.warn("Unable to start plugin {}", plugin.getClass().getSimpleName(), e);
        }
    }

    private void tellUser(Plugin plugin, String internalName, String problem)
    {
        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
        String name = descriptor != null ? descriptor.name().replaceAll("<[^>]*>", "") : internalName;

        String token = ProjectXAccount.token(configManager);
        String fix = Strings.isNullOrEmpty(token)
                ? " Sign in to Project X in the launcher, or add your API token in the Project X settings."
                // "Get", not "Buy": most of the store is free now, and
                // telling somebody to buy a free script sends them looking
                // for a price that is not there.
                : " Get access at " + storeUrls.getOrDefault(internalName, ProjectXSite.store().toString());

        log.info("{} {}{}", name, problem, fix);
        notifier.notify(name + " " + problem + fix);
    }

    /** Plugins are keyed by class name, which is also the hub's internalName. Core plugins are never gated. */
    private static String internalName(Plugin plugin)
    {
        PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
        if (descriptor == null || !descriptor.isExternal())
        {
            return null;
        }
        return plugin.getClass().getSimpleName();
    }
}
