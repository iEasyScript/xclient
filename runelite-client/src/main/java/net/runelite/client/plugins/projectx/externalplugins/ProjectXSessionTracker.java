package net.runelite.client.plugins.projectx.externalplugins;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ClientShutdown;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Follows each store script from start to stop and reports the run to the site:
 * what ran, for how long, why it ended, and the experience it gained. That feeds
 * the player's session history, and a run that ended by itself (out of supplies,
 * an error, a goal reached) or a death becomes a Discord DM.
 *
 * Never reads or sends the in-game account name.
 *
 * Why a run ended is told to it by whoever ended it: the plugin list and config
 * panel (the player), the Script Queue, and the entitlement check. Anything else
 * that stops a script is the script stopping itself.
 */
@Slf4j
@Singleton
public class ProjectXSessionTracker
{
    public enum End
    {
        MANUAL, QUEUE, SELF_STOP, ACCESS_ENDED, CLIENT_CLOSED
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    /** A message shown this recently before a stop is taken to be why it stopped. */
    private static final long MESSAGE_WINDOW_MS = 60_000;
    /** A stop marked by the player or the queue within this long counts as theirs. */
    private static final long MARK_WINDOW_MS = 10_000;
    /** After a script's loop shuts down, how long to wait before deciding it stopped by itself. */
    private static final long SELF_STOP_CHECK_MS = 4_000;

    private static volatile ProjectXSessionTracker instance;

    private final Client client;
    private final ClientThread clientThread;
    private final EventBus eventBus;
    private final OkHttpClient http;
    private final PluginManager pluginManager;
    private final ProjectXPluginManager projectxPluginManager;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r ->
    {
        Thread t = new Thread(r, "projectx-sessions");
        t.setDaemon(true);
        return t;
    });

    private static final class Run
    {
        final Plugin plugin;
        final String internalName;
        final String name;
        final String version;
        final long startedAt = System.currentTimeMillis();
        final Map<Skill, Integer> baseline = new EnumMap<>(Skill.class);
        volatile boolean died;

        Run(Plugin plugin, String internalName, String name, String version)
        {
            this.plugin = plugin;
            this.internalName = internalName;
            this.name = name;
            this.version = version;
        }
    }

    private static final class Mark
    {
        final End end;
        final long at = System.currentTimeMillis();

        Mark(End end)
        {
            this.end = end;
        }
    }

    private final Map<Plugin, Run> runs = new ConcurrentHashMap<>();
    private final Map<Plugin, Mark> marks = new ConcurrentHashMap<>();
    /** The latest experience per skill, kept from StatChanged so a logged-out end still has numbers. */
    private final Map<Skill, Integer> latestXp = new ConcurrentHashMap<>();
    private volatile String lastMessage;
    private volatile long lastMessageAt;

    @Inject
    ProjectXSessionTracker(Client client, ClientThread clientThread, EventBus eventBus, OkHttpClient http,
                           PluginManager pluginManager, ProjectXPluginManager projectxPluginManager)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.eventBus = eventBus;
        this.http = http;
        this.pluginManager = pluginManager;
        this.projectxPluginManager = projectxPluginManager;
        instance = this;
    }

    public void start()
    {
        eventBus.register(this);
    }

    public void stop()
    {
        eventBus.unregister(this);
    }

    // ---- told by whoever stops a script

    /** The player, the queue or the entitlement check is about to stop this plugin. */
    public static void markStop(Plugin plugin, End end)
    {
        ProjectXSessionTracker t = instance;
        if (t != null && plugin != null)
        {
            t.marks.put(plugin, new Mark(end));
        }
    }

    /** A message box a script showed: the likeliest explanation of a stop that follows. */
    public static void noteMessage(String message)
    {
        ProjectXSessionTracker t = instance;
        if (t != null && message != null)
        {
            t.lastMessage = message.trim();
            t.lastMessageAt = System.currentTimeMillis();
        }
    }

    /**
     * A script's loop has shut down. If its plugin is still switched on a moment
     * later and the loop has not started again, the script stopped by itself.
     */
    public static void onScriptShutdown(Object script, java.util.function.BooleanSupplier runningAgain)
    {
        ProjectXSessionTracker t = instance;
        if (t == null || script == null)
        {
            return;
        }
        String pkg = script.getClass().getPackage() == null ? "" : script.getClass().getPackage().getName();
        t.executor.schedule(() ->
        {
            for (Run run : t.runs.values())
            {
                String runPkg = run.plugin.getClass().getPackage().getName();
                if (!pkg.startsWith(runPkg))
                {
                    continue;
                }
                if (runningAgain.getAsBoolean() || !t.pluginManager.isPluginActive(run.plugin))
                {
                    return;
                }
                t.finish(run, End.SELF_STOP);
                return;
            }
        }, SELF_STOP_CHECK_MS, TimeUnit.MILLISECONDS);
    }

    // ---- events

    @Subscribe
    public void onPluginChanged(PluginChanged event)
    {
        Plugin plugin = event.getPlugin();
        if (event.isLoaded())
        {
            String internalName = plugin.getClass().getSimpleName();
            if (!isStoreScript(plugin) || runs.containsKey(plugin))
            {
                return;
            }
            PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
            String name = d == null ? internalName : d.name().replaceAll("<[^>]*>", "").trim();
            Run run = new Run(plugin, internalName, name, d == null ? null : d.version());
            runs.put(plugin, run);
            marks.remove(plugin);
            clientThread.invokeLater(() -> snapshot(run));
            return;
        }
        Run run = runs.get(plugin);
        if (run == null)
        {
            return;
        }
        Mark mark = marks.remove(plugin);
        End end = mark != null && System.currentTimeMillis() - mark.at < MARK_WINDOW_MS ? mark.end : End.SELF_STOP;
        finish(run, end);
    }

    /**
     * A script from the store: its classes came out of a downloaded store jar.
     *
     * <p>Not by name. Matching the class name against the catalogue also matched
     * RuneLite's own Slayer, Pest Control, Herbiboar, Tears of Guthix, Daily Tasks,
     * Discord and Barrows plugins, which share names with store scripts and are on
     * all the time -- so every dashboard showed them, each "run" lasting the whole
     * client session and credited with everything gained in it. And a script that
     * started with the client, before the catalogue had loaded, was never tracked.
     */
    static boolean isStoreScript(Plugin plugin)
    {
        return plugin != null && plugin.getClass().getClassLoader() instanceof PluginJarClassLoader;
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        latestXp.put(event.getSkill(), event.getXp());
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            // A run started while logged out has no baseline yet: take it now.
            for (Run run : runs.values())
            {
                if (run.baseline.isEmpty())
                {
                    clientThread.invokeLater(() -> snapshot(run));
                }
            }
        }
    }

    /** Retries a run's starting point until the game has sent the stats. */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        for (Run run : runs.values())
        {
            if (run.baseline.isEmpty())
            {
                snapshot(run);
            }
        }
    }

    @Subscribe
    public void onActorDeath(ActorDeath event)
    {
        Player me = client.getLocalPlayer();
        if (me == null || event.getActor() != me)
        {
            return;
        }
        for (Run run : runs.values())
        {
            if (run.died)
            {
                continue;
            }
            run.died = true;
            JsonObject json = new JsonObject();
            json.addProperty("kind", "death");
            json.addProperty("source", "store");
            json.addProperty("scriptInternalName", run.internalName);
            json.addProperty("scriptName", run.name);
            executor.execute(() -> post(json));
        }
    }

    @Subscribe
    public void onClientShutdown(ClientShutdown event)
    {
        for (Run run : runs.values())
        {
            CompletableFuture<Void> sent = new CompletableFuture<>();
            finish(run, End.CLIENT_CLOSED, sent);
            event.waitFor(sent);
        }
    }

    // ---- reporting

    /** Called on the client thread. */
    private void snapshot(Run run)
    {
        if (client.getGameState() != GameState.LOGGED_IN || !run.baseline.isEmpty())
        {
            return;
        }
        // Just after login the game has not sent the stats yet and every skill reads zero. A starting point
        // taken then counts the whole account's experience as gained by the run.
        if (client.getOverallExperience() <= 0)
        {
            return;
        }
        for (Skill skill : Skill.values())
        {
            int xp = client.getSkillExperience(skill);
            run.baseline.put(skill, xp);
            latestXp.put(skill, xp);
        }
    }

    private void finish(Run run, End end)
    {
        finish(run, end, null);
    }

    private void finish(Run run, End end, CompletableFuture<Void> done)
    {
        if (runs.remove(run.plugin) == null)
        {
            if (done != null)
            {
                done.complete(null);
            }
            return;
        }
        long endedAt = System.currentTimeMillis();
        JsonObject xp = new JsonObject();
        for (Map.Entry<Skill, Integer> e : run.baseline.entrySet())
        {
            Integer now = latestXp.get(e.getKey());
            if (now != null && now > e.getValue())
            {
                xp.addProperty(e.getKey().name(), now - e.getValue());
            }
        }
        JsonObject json = new JsonObject();
        // Clients before this one also reported RuneLite's own plugins that share a store
        // script's name; the site ignores those names unless the report says it is a store run.
        json.addProperty("source", "store");
        json.addProperty("clientVersion", net.runelite.client.RuneLiteProperties.getProjectXVersion());
        json.addProperty("scriptInternalName", run.internalName);
        json.addProperty("scriptName", run.name);
        json.addProperty("scriptVersion", run.version);
        json.addProperty("startedAt", run.startedAt);
        json.addProperty("endedAt", endedAt);
        json.addProperty("endReason", end.name());
        json.addProperty("died", run.died);
        json.add("xpGained", xp);
        if (end == End.SELF_STOP && lastMessage != null && endedAt - lastMessageAt < MESSAGE_WINDOW_MS)
        {
            json.addProperty("stopMessage", lastMessage);
        }
        log.debug("Session ended: {} after {}s ({})", run.name, (endedAt - run.startedAt) / 1000, end);
        executor.execute(() ->
        {
            try
            {
                post(json);
            }
            finally
            {
                if (done != null)
                {
                    done.complete(null);
                }
            }
        });
    }

    private void post(JsonObject json)
    {
        String token = ProjectXAccount.token();
        if (token == null)
        {
            return;
        }
        Request request = new Request.Builder()
            .url(ProjectXSite.api("sessions"))
            .header("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON, json.toString()))
            .build();
        try (Response response = http.newBuilder().callTimeout(5, TimeUnit.SECONDS).build().newCall(request).execute())
        {
            if (!response.isSuccessful())
            {
                log.debug("Session report refused: {}", response.code());
            }
        }
        catch (Exception e)
        {
            log.debug("Session report not sent: {}", e.getMessage());
        }
    }
}
