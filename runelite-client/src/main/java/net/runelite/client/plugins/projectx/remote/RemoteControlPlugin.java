package net.runelite.client.plugins.projectx.remote;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerState;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Config;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2State;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXEntitlements;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXInstances;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSessionTracker;
import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueueFinished;
import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueueStepStarted;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Website control: the Bots page on the website shows this client live and can
 * start and stop its scripts. Only the account this client is signed in to sees
 * it there.
 *
 * <p>Rides on the instance heartbeat ({@link ProjectXInstances}): the state goes
 * up with each beat, commands come back in the answer. Two switches in the
 * settings, both on by default: control, and live details. With control off,
 * every command is answered "remote control is off in this client"; shutting
 * the client down from the website still works, as it always has.
 *
 * <p>The game is read on the client thread every few ticks into a snapshot; the
 * heartbeat thread only ever reads that snapshot. Commands run on a thread of
 * their own.
 */
@PluginDescriptor(
    name = "Website control",
    description = "See this client on the website's Bots page, and start and stop its scripts from there",
    tags = {"projectx", "remote", "website", "dashboard", "control", "bots"},
    enabledByDefault = true,
    alwaysOn = true
)
@Slf4j
public class RemoteControlPlugin extends Plugin
{
    /** Read the game this often: often enough for a 12-second heartbeat, cheap enough to forget. */
    private static final int SNAPSHOT_TICKS = 5;
    private static final long PLAN_NAMES_MS = 30_000;
    private static final String QUEUE_GROUP = "projectxscriptqueue";

    @Inject
    private RemoteConfig config;
    @Inject
    private Client client;
    @Inject
    private ConfigManager configManager;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private ProjectXPluginManager projectxPluginManager;
    @Inject
    private ProjectXEntitlements entitlements;
    @Inject
    private ProjectXInstances instances;
    @Inject
    private Gson gson;

    private ExecutorService commandThread;
    private volatile GameSnapshot game;
    private int ticks;

    // The queue, as its events describe it.
    private volatile boolean queueRunning;
    private volatile String queuePlan;
    private volatile int queueStep;
    private volatile int queueSteps;
    private volatile String queueStepText;
    private volatile List<String> planNames = Collections.emptyList();
    private volatile long planNamesAt;

    @Provides
    RemoteConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(RemoteConfig.class);
    }

    @Override
    protected void startUp()
    {
        commandThread = Executors.newSingleThreadExecutor(r ->
        {
            Thread t = new Thread(r, "projectx-remote");
            t.setDaemon(true);
            return t;
        });
        LiveRemoteActions actions = new LiveRemoteActions(pluginManager, projectxPluginManager, entitlements,
            configManager, () -> queueRunning);
        RemoteDispatcher dispatcher = new RemoteDispatcher(config::allowControl, actions);
        instances.setExtension(new RemoteLink(config::allowControl, config::allowMonitoring, this::state,
            dispatcher, commandThread, instances::beatSoon));
    }

    @Override
    protected void shutDown()
    {
        instances.setExtension(null);
        if (commandThread != null)
        {
            commandThread.shutdownNow();
            commandThread = null;
        }
    }

    // ---- the game, on the client thread

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (++ticks % SNAPSHOT_TICKS == 1 || game == null)
        {
            game = readGame();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() != GameState.LOGGED_IN)
        {
            game = GameSnapshot.LOGGED_OUT;
        }
        ticks = 0;
    }

    private GameSnapshot readGame()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return GameSnapshot.LOGGED_OUT;
        }
        Player me = client.getLocalPlayer();
        return new GameSnapshot(
            true,
            client.getWorld(),
            client.getWorldType().contains(WorldType.MEMBERS),
            me == null ? null : me.getName(),
            client.getTotalLevel(),
            GameSnapshot.percent(client.getBoostedSkillLevel(Skill.HITPOINTS), client.getRealSkillLevel(Skill.HITPOINTS)),
            GameSnapshot.percent(client.getBoostedSkillLevel(Skill.PRAYER), client.getRealSkillLevel(Skill.PRAYER)));
    }

    // ---- the queue, from its events

    @Subscribe
    public void onScriptQueueStepStarted(ScriptQueueStepStarted event)
    {
        queueRunning = true;
        queuePlan = event.getPlan();
        queueStep = event.getStepIndex() + 1;
        queueSteps = event.getStepCount();
        queueStepText = event.getDescription();
    }

    @Subscribe
    public void onScriptQueueFinished(ScriptQueueFinished event)
    {
        queueRunning = false;
        queuePlan = null;
        queueStepText = null;
        queueStep = 0;
        queueSteps = 0;
    }

    /** Turning a switch tells the website straight away rather than at the next beat. */
    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (RemoteConfig.GROUP.equals(event.getGroup()))
        {
            log.info("Website control: control {}, live details {}", config.allowControl() ? "on" : "off",
                config.allowMonitoring() ? "on" : "off");
            instances.beatSoon();
        }
    }

    // ---- the state, on the heartbeat thread

    private JsonObject state()
    {
        long now = System.currentTimeMillis();
        List<ProjectXSessionTracker.ActiveRun> active = ProjectXSessionTracker.activeRuns();
        RemoteState.Inputs.InputsBuilder in = RemoteState.Inputs.builder()
            .clientVersion(RuneLiteProperties.getProjectXVersion())
            .game(game)
            .shareName(config.shareName());

        for (ProjectXSessionTracker.ActiveRun run : active)
        {
            RemoteState.Run.RunBuilder r = RemoteState.Run.builder()
                .name(run.getName())
                .internalName(run.getInternalName())
                .version(run.getVersion())
                .startedAt(run.getStartedAt())
                .status(status(run.getName(), active.size(), now));
            for (Map.Entry<Skill, Integer> e : run.getXpGained().entrySet())
            {
                r.xp(e.getKey().name(), e.getValue());
            }
            in.run(r.build());
        }

        breakState(in, now);

        RemoteState.Queue.QueueBuilder q = RemoteState.Queue.builder()
            .running(queueRunning)
            .plan(queuePlan)
            .step(queueStep)
            .steps(queueSteps)
            .description(queueStepText);
        planNames(now).forEach(q::planName);
        in.queue(q.build());

        return RemoteState.build(in.build(), now);
    }

    /** The script's own panel's status line, or with one script running, the client's status. */
    private static String status(String scriptName, int running, long now)
    {
        String line = ScriptStatusBoard.statusFor(scriptName, running, now);
        if (line == null && running == 1 && ProjectX.status != null && !"IDLE".equalsIgnoreCase(ProjectX.status.trim()))
        {
            line = ProjectX.status;
        }
        return line;
    }

    private void breakState(RemoteState.Inputs.InputsBuilder in, long now)
    {
        if (isActive(BreakHandlerPlugin.class))
        {
            in.breakHandler("v1").breakActive(BreakHandlerScript.isBreakActive());
            BreakHandlerState state = BreakHandlerScript.getCurrentState();
            int left = BreakHandlerScript.breakDuration;
            if ((state == BreakHandlerState.LOGGED_OUT || state == BreakHandlerState.INGAME_BREAK_ACTIVE) && left > 0)
            {
                in.breakUntil(now + left * 1000L);
            }
        }
        else if (isActive(BreakHandlerV2Plugin.class))
        {
            boolean active = BreakHandlerV2State.isBreakActive();
            in.breakHandler("v2").breakActive(active);
            if (active)
            {
                // V2 keeps the end of a logout break in its settings, so it survives a restart.
                Long end = configManager.getConfiguration(BreakHandlerV2Config.configGroup, "persistedBreakEnd", Long.class);
                if (end != null)
                {
                    in.breakUntil(end);
                }
            }
        }
    }

    private boolean isActive(Class<? extends Plugin> type)
    {
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (plugin.getClass() == type)
            {
                return pluginManager.isPluginActive(plugin);
            }
        }
        return false;
    }

    /** The saved queue plans' names, re-read now and then from the queue's settings. */
    private List<String> planNames(long now)
    {
        if (now - planNamesAt < PLAN_NAMES_MS)
        {
            return planNames;
        }
        planNamesAt = now;
        List<String> names = new ArrayList<>();
        try
        {
            String json = configManager.getConfiguration(QUEUE_GROUP, "plans");
            if (json != null && !json.isBlank())
            {
                JsonArray plans = gson.fromJson(json, JsonArray.class);
                for (JsonElement p : plans)
                {
                    if (p.isJsonObject() && p.getAsJsonObject().has("name"))
                    {
                        names.add(p.getAsJsonObject().get("name").getAsString());
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            log.debug("Queue plans unreadable: {}", e.toString());
        }
        planNames = names;
        return names;
    }
}
