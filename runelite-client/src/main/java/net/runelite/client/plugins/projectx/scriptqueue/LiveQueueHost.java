package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerConfig;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSessionTracker;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.time.Clock;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** The queue's dealings with the real client. Never touches the client thread except through helpers that hop onto it. */
@Slf4j
final class LiveQueueHost implements QueueHost
{
    /** How long to wait for a requested break to begin: it waits until you are out of combat. */
    private static final long BREAK_START_TIMEOUT_MS = 3 * 60_000;
    private static final String PROJECTX_PACKAGE = "net.runelite.client.plugins.projectx";

    private final EventBus eventBus;
    private final Clock clock = Clock.systemDefaultZone();

    LiveQueueHost(EventBus eventBus)
    {
        this.eventBus = eventBus;
        ScriptErrorWatch.install();
    }

    @Override
    public Plugin find(QueueStep step)
    {
        Plugin plugin = step.getPluginClass() == null ? null : ProjectX.getPlugin(step.getPluginClass());
        return plugin != null ? plugin : byName(step.getPluginName());
    }

    /**
     * The script a step names, when its class is not loaded: a store script can be renamed
     * (a new class name, the same script), and a queue saved before that still names the
     * old class. Only an unambiguous match counts.
     */
    private static Plugin byName(String name)
    {
        if (name == null)
        {
            return null;
        }
        List<Plugin> matches = ProjectX.getPluginManager().getPlugins().stream()
            .filter(p -> p.getClass().getName().startsWith(PROJECTX_PACKAGE + "."))
            .filter(p -> name.equals(ScriptQueuePanel.nameOf(p)))
            .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    @Override
    public boolean isActive(Plugin plugin)
    {
        return ProjectX.getPluginManager().isPluginActive(plugin);
    }

    @Override
    public void start(Plugin plugin)
    {
        ProjectX.startPlugin(plugin);
    }

    @Override
    public void stop(Plugin plugin, boolean manual)
    {
        ProjectXSessionTracker.markStop(plugin, manual ? ProjectXSessionTracker.End.MANUAL : ProjectXSessionTracker.End.QUEUE);
        ProjectX.stopPlugin(plugin);
    }

    /**
     * A BreakHandler break is on. Only while the BreakHandler runs: turned off part-way
     * through a break, its state is left where it was and would otherwise read as a
     * break that never ends.
     */
    @Override
    public boolean onBreak()
    {
        BreakHandlerPlugin breakHandler = ProjectX.getPlugin(BreakHandlerPlugin.class);
        return breakHandler != null && isActive(breakHandler) && BreakHandlerScript.isBreakActive();
    }

    @Override
    public String breakStatus()
    {
        switch (BreakHandlerScript.getCurrentState())
        {
            case LOGGED_OUT:
            case INGAME_BREAK_ACTIVE:
                return "On a break, " + BreakHandlerScript.formatDuration(
                    java.time.Duration.ofSeconds(Math.max(0, BreakHandlerScript.breakDuration))) + " left";
            case LOGIN_REQUESTED:
            case LOGGING_IN:
            case LOGIN_EXTENDED_SLEEP:
                return "Break over, logging back in";
            default:
                return "Starting a break";
        }
    }

    /**
     * Asks the BreakHandler for a break and waits for it to end. It is the BreakHandler's
     * own break, so its settings decide how long, whether to log out, and which world to
     * log back in to. Turns the BreakHandler on if it is off.
     */
    @Override
    public boolean takeBreak(BooleanSupplier stopRequested, Consumer<String> status) throws InterruptedException
    {
        BreakHandlerPlugin breakHandler = ProjectX.getPlugin(BreakHandlerPlugin.class);
        if (breakHandler == null)
        {
            status.accept("BreakHandler isn't available. Taking a short pause instead.");
            Thread.sleep(2000);
            return false;
        }
        if (!isActive(breakHandler))
        {
            status.accept("Turning on the BreakHandler.");
            ProjectX.startPlugin(breakHandler);
            for (int i = 0; i < 20 && !isActive(breakHandler) && !stopRequested.getAsBoolean(); i++)
            {
                Thread.sleep(500);
            }
            if (!isActive(breakHandler))
            {
                status.accept("Couldn't turn on the BreakHandler. Taking a short pause instead.");
                Thread.sleep(2000);
                return false;
            }
        }

        if (!onBreak())
        {
            // The script before was stopped, so a lock it left behind protects nothing,
            // and the BreakHandler won't start a break while it is held.
            BreakHandlerScript.setLockState(false);
            ProjectX.getConfigManager().setConfiguration(BreakHandlerConfig.configGroup, "breakNow", true);

            long asked = System.currentTimeMillis();
            while (!onBreak() && !stopRequested.getAsBoolean())
            {
                if (System.currentTimeMillis() - asked > BREAK_START_TIMEOUT_MS)
                {
                    // Don't leave the request set: it would fire whenever the BreakHandler next runs.
                    ProjectX.getConfigManager().setConfiguration(BreakHandlerConfig.configGroup, "breakNow", false);
                    status.accept("The break didn't start. Carrying on.");
                    log.info("Script queue: BreakHandler didn't start a break within {}s", BREAK_START_TIMEOUT_MS / 1000);
                    Thread.sleep(2000);
                    return false;
                }
                status.accept("Waiting to start a break (out of combat first).");
                Thread.sleep(1000);
            }
        }

        log.info("Script queue: on a BreakHandler break before the next script");
        while (onBreak() && !stopRequested.getAsBoolean())
        {
            status.accept(breakStatus());
            Thread.sleep(1000);
        }
        return true;
    }

    @Override
    public void logout()
    {
        Rs2Player.logout();
    }

    @Override
    public void post(Object event)
    {
        if (eventBus == null)
        {
            return;
        }
        try
        {
            eventBus.post(event);
        }
        catch (RuntimeException e)
        {
            // A listener's mistake must not stop the queue.
            log.warn("Script queue: a listener failed on {}", event.getClass().getSimpleName(), e);
        }
    }

    @Override
    public String errorSince(Plugin plugin, long sinceMillis)
    {
        Package pkg = plugin.getClass().getPackage();
        String name = pkg == null ? null : pkg.getName();
        // A plugin straight in the projectx package would match every script's errors.
        if (name == null || name.equals(PROJECTX_PACKAGE))
        {
            return null;
        }
        return ScriptErrorWatch.latest(name, sinceMillis);
    }

    @Override
    public void sleep(long millis) throws InterruptedException
    {
        Thread.sleep(millis);
    }

    @Override
    public Clock clock()
    {
        return clock;
    }
}
