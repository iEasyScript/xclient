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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    // Remembered across restarts, so a paid jar cannot run just because the hub
    // happened to be unreachable when the client started.
    private static final String PAID_PLUGINS_KEY = "paidPlugins";

    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final ConfigManager configManager;
    private final PluginManager pluginManager;
    private final ScheduledExecutorService executor;
    private final Notifier notifier;

    private final Map<String, Instant> expiries = new ConcurrentHashMap<>();
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

    public boolean isEntitled(String internalName)
    {
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
            refresh();
            if (isEntitled(internalName))
            {
                SwingUtilities.invokeLater(() -> startNow(plugin));
            }
            else
            {
                tellUser(plugin, internalName, "needs active access to run.");
            }
        });
        return false;
    }

    /** Stops any running paid plugin whose access has ended. */
    private void enforce()
    {
        try
        {
            if (System.currentTimeMillis() - lastRefreshMillis >= TimeUnit.SECONDS.toMillis(REFRESH_INTERVAL_SECONDS))
            {
                refresh();
            }

            List<Plugin> expired = new ArrayList<>();
            for (Plugin plugin : pluginManager.getPlugins())
            {
                String internalName = internalName(plugin);
                if (internalName != null && isPaid(internalName) && !isEntitled(internalName)
                        && pluginManager.isPluginActive(plugin))
                {
                    expired.add(plugin);
                }
            }

            for (Plugin plugin : expired)
            {
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
                tellUser(plugin, internalName(plugin), "has been stopped: your access has ended.");
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
    public void refresh()
    {
        if (paid.isEmpty())
        {
            return;
        }

        String token = ProjectXAccount.token(configManager);
        if (Strings.isNullOrEmpty(token))
        {
            expiries.clear();
            lastRefreshMillis = System.currentTimeMillis();
            return;
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
                return;
            }
            if (!response.isSuccessful() || response.body() == null)
            {
                log.warn("Entitlement check failed: HTTP {}", response.code());
                return;
            }

            JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
            for (JsonElement element : json.getAsJsonArray("results"))
            {
                JsonObject result = element.getAsJsonObject();
                String internalName = result.get("internalName").getAsString();
                JsonElement expiresAt = result.get("expiresAt");
                if (result.get("active").getAsBoolean() && expiresAt != null && !expiresAt.isJsonNull())
                {
                    expiries.put(internalName, Instant.parse(expiresAt.getAsString()));
                }
                else
                {
                    expiries.remove(internalName);
                }
            }
        }
        catch (IOException | JsonParseException | DateTimeParseException | IllegalStateException | NullPointerException e)
        {
            log.warn("Entitlement check failed: {}", e.getMessage());
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
                : " Get access at " + storeUrls.getOrDefault(internalName, ProjectXSite.baseUrl().toString());

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
