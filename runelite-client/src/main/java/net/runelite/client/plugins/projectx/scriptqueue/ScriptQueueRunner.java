package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerConfig;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSessionTracker;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Works through the queue on its own thread: start a script, watch for its step to
 * end, stop it, pause briefly (or take a BreakHandler break), start the next. Polls
 * once a second; never touches the client thread except through the helpers that
 * hop onto it.
 *
 * <p>BreakHandler breaks are respected whoever starts them: while one is on, a step's
 * clock stops and no script is started, so a script never starts logged out and an
 * hour of a script is an hour of it running.
 */
@Slf4j
final class ScriptQueueRunner implements Runnable
{
    /** A script that is gone again this soon after starting never really started. */
    private static final long FAILED_START_MS = 15_000;
    /** How long to wait for a requested break to begin: it waits until you are out of combat. */
    private static final long BREAK_START_TIMEOUT_MS = 3 * 60_000;

    private final List<QueueStep> steps;
    private final boolean loop;
    private final boolean logoutWhenDone;
    private final boolean breakBetween;
    private final int pauseMinSeconds;
    private final int pauseMaxSeconds;
    private final Consumer<String> status;

    private volatile boolean stopRequested;
    private volatile Plugin current;

    ScriptQueueRunner(List<QueueStep> steps, boolean loop, boolean logoutWhenDone, boolean breakBetween,
                      int pauseMinSeconds, int pauseMaxSeconds, Consumer<String> status)
    {
        this.steps = List.copyOf(steps);
        this.loop = loop;
        this.logoutWhenDone = logoutWhenDone;
        this.breakBetween = breakBetween;
        this.pauseMinSeconds = Math.max(0, Math.min(pauseMinSeconds, pauseMaxSeconds));
        this.pauseMaxSeconds = Math.max(pauseMinSeconds, pauseMaxSeconds);
        this.status = status;
    }

    void requestStop()
    {
        stopRequested = true;
    }

    @Override
    public void run()
    {
        try
        {
            do
            {
                for (int i = 0; i < steps.size() && !stopRequested; i++)
                {
                    runStep(i, steps.get(i));
                    if (!stopRequested && i < steps.size() - 1)
                    {
                        betweenScripts();
                    }
                }
                if (loop && !stopRequested)
                {
                    betweenScripts();
                }
            }
            while (loop && !stopRequested);

            if (stopRequested)
            {
                status.accept("Stopped.");
                return;
            }
            if (logoutWhenDone)
            {
                status.accept("Queue finished. Logging out.");
                Rs2Player.logout();
            }
            status.accept("Queue finished.");
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            status.accept("Stopped.");
        }
        catch (Exception e)
        {
            log.warn("Script queue stopped on an error", e);
            status.accept("Stopped: " + e.getClass().getSimpleName());
        }
        finally
        {
            Plugin running = current;
            if (running != null && isActive(running))
            {
                ProjectXSessionTracker.markStop(running, ProjectXSessionTracker.End.MANUAL);
                ProjectX.stopPlugin(running);
            }
            current = null;
        }
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
            .filter(p -> p.getClass().getName().startsWith("net.runelite.client.plugins.projectx."))
            .filter(p -> name.equals(ScriptQueuePanel.nameOf(p)))
            .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private void runStep(int index, QueueStep step) throws InterruptedException
    {
        String prefix = "Step " + (index + 1) + "/" + steps.size() + ": " + step.getPluginName();
        Plugin plugin = ProjectX.getPlugin(step.getPluginClass());
        if (plugin == null)
        {
            plugin = byName(step.getPluginName());
        }
        if (plugin == null)
        {
            status.accept(prefix + " isn't installed. Skipping.");
            log.info("Script queue: {} not installed, skipped", step.getPluginClass());
            Thread.sleep(3000);
            return;
        }

        Skill skill = step.getUntil() == QueueStep.Until.LEVEL ? skillOf(step.getSkill()) : null;
        if (skill != null && realLevel(skill) >= step.getLevel())
        {
            status.accept(prefix + ": " + step.getSkill() + " is already " + step.getLevel() + ". Skipping.");
            Thread.sleep(2000);
            return;
        }

        // A break the BreakHandler began on its own during the pause: starting a
        // script now would start it logged out, and it would stop straight away.
        waitOutBreak(prefix + ": waiting for the break to end");
        if (stopRequested)
        {
            return;
        }

        status.accept(prefix + ": starting");
        current = plugin;
        if (!isActive(plugin))
        {
            ProjectX.startPlugin(plugin);
        }
        long started = System.currentTimeMillis();
        long deadline = step.getUntil() == QueueStep.Until.MINUTES ? started + step.getMinutes() * 60_000L : Long.MAX_VALUE;
        boolean seenActive = false;
        long lastTick = started;

        while (!stopRequested)
        {
            Thread.sleep(1000);
            long tick = System.currentTimeMillis();
            long elapsed = tick - lastTick;
            lastTick = tick;

            if (onBreak())
            {
                // The script is paused, not finished: hold its clock until the break is over.
                deadline = deadline == Long.MAX_VALUE ? deadline : deadline + elapsed;
                started += elapsed;
                status.accept(prefix + ": " + breakStatus());
                continue;
            }

            boolean active = isActive(plugin);
            if (active)
            {
                seenActive = true;
            }
            long now = System.currentTimeMillis();

            if (!active && (seenActive || now - started > 5000))
            {
                boolean failed = now - started < FAILED_START_MS;
                status.accept(prefix + (failed ? " didn't start (not owned, or it stopped straight away)." : " stopped by itself."));
                log.info("Script queue: {} {}", step.getPluginName(), failed ? "failed to start" : "stopped by itself");
                Thread.sleep(2000);
                break;
            }
            if (now >= deadline)
            {
                status.accept(prefix + ": time's up.");
                break;
            }
            if (skill != null && realLevel(skill) >= step.getLevel())
            {
                status.accept(prefix + ": reached " + step.getSkill() + " " + step.getLevel() + ".");
                break;
            }

            String left = step.getUntil() == QueueStep.Until.MINUTES
                ? QueueStep.formatMinutes((int) Math.max(1, (deadline - now) / 60_000)) + " left"
                : step.getUntil() == QueueStep.Until.LEVEL
                ? step.getSkill() + " " + realLevel(skill) + "/" + step.getLevel()
                : "running " + QueueStep.formatMinutes((int) ((now - started) / 60_000));
            status.accept(prefix + ": " + left);
        }

        if (isActive(plugin))
        {
            ProjectXSessionTracker.markStop(plugin, stopRequested ? ProjectXSessionTracker.End.MANUAL : ProjectXSessionTracker.End.QUEUE);
            ProjectX.stopPlugin(plugin);
            // Let it wind down (bank, put the hook away) before the next one starts.
            for (int i = 0; i < 20 && isActive(plugin); i++)
            {
                Thread.sleep(500);
            }
        }
        current = null;
    }

    private void betweenScripts() throws InterruptedException
    {
        if (!breakBetween || !takeBreak())
        {
            pause();
        }
    }

    /**
     * Asks the BreakHandler for a break and waits for it to end. It is the BreakHandler's
     * own break, so its settings decide how long, whether to log out, and which world to
     * log back in to. Turns the BreakHandler on if it is off.
     *
     * @return false if no break happened, so the caller can fall back to the short pause
     */
    private boolean takeBreak() throws InterruptedException
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
            for (int i = 0; i < 20 && !isActive(breakHandler) && !stopRequested; i++)
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
            while (!onBreak() && !stopRequested)
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
        waitOutBreak(null);
        return true;
    }

    /** Waits while a BreakHandler break is on, showing how long is left. */
    private void waitOutBreak(String prefix) throws InterruptedException
    {
        while (onBreak() && !stopRequested)
        {
            status.accept(prefix == null ? breakStatus() : prefix + " (" + breakStatus() + ")");
            Thread.sleep(1000);
        }
    }

    /**
     * A BreakHandler break is on. Only while the BreakHandler runs: turned off part-way
     * through a break, its state is left where it was and would otherwise read as a
     * break that never ends.
     */
    private static boolean onBreak()
    {
        BreakHandlerPlugin breakHandler = ProjectX.getPlugin(BreakHandlerPlugin.class);
        return breakHandler != null && isActive(breakHandler) && BreakHandlerScript.isBreakActive();
    }

    private static String breakStatus()
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

    private void pause() throws InterruptedException
    {
        if (pauseMaxSeconds <= 0)
        {
            return;
        }
        int seconds = ThreadLocalRandom.current().nextInt(pauseMinSeconds, pauseMaxSeconds + 1);
        for (int s = seconds; s > 0 && !stopRequested; s--)
        {
            status.accept("Short pause before the next script: " + s + "s");
            Thread.sleep(1000);
        }
    }

    private static boolean isActive(Plugin plugin)
    {
        return ProjectX.getPluginManager().isPluginActive(plugin);
    }

    private static int realLevel(Skill skill)
    {
        return ProjectX.isLoggedIn() ? Rs2Player.getRealSkillLevel(skill) : 0;
    }

    static Skill skillOf(String name)
    {
        if (name == null)
        {
            return null;
        }
        try
        {
            return Skill.valueOf(name.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }
}
