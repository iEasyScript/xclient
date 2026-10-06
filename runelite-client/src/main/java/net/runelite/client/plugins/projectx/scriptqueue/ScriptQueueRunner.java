package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Works through the queue on its own thread: start a script, watch for its step to
 * end, stop it, pause briefly, start the next. Polls once a second; never touches
 * the client thread except through the helpers that hop onto it.
 */
@Slf4j
final class ScriptQueueRunner implements Runnable
{
    /** A script that is gone again this soon after starting never really started. */
    private static final long FAILED_START_MS = 15_000;

    private final List<QueueStep> steps;
    private final boolean loop;
    private final boolean logoutWhenDone;
    private final int pauseMinSeconds;
    private final int pauseMaxSeconds;
    private final Consumer<String> status;

    private volatile boolean stopRequested;
    private volatile Plugin current;

    ScriptQueueRunner(List<QueueStep> steps, boolean loop, boolean logoutWhenDone, int pauseMinSeconds, int pauseMaxSeconds,
                      Consumer<String> status)
    {
        this.steps = List.copyOf(steps);
        this.loop = loop;
        this.logoutWhenDone = logoutWhenDone;
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
                        pause();
                    }
                }
                if (loop && !stopRequested)
                {
                    pause();
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
                ProjectX.stopPlugin(running);
            }
            current = null;
        }
    }

    private void runStep(int index, QueueStep step) throws InterruptedException
    {
        String prefix = "Step " + (index + 1) + "/" + steps.size() + ": " + step.getPluginName();
        Plugin plugin = ProjectX.getPlugin(step.getPluginClass());
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

        status.accept(prefix + ": starting");
        current = plugin;
        if (!isActive(plugin))
        {
            ProjectX.startPlugin(plugin);
        }
        long started = System.currentTimeMillis();
        long deadline = step.getUntil() == QueueStep.Until.MINUTES ? started + step.getMinutes() * 60_000L : Long.MAX_VALUE;
        boolean seenActive = false;

        while (!stopRequested)
        {
            Thread.sleep(1000);
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
            ProjectX.stopPlugin(plugin);
            // Let it wind down (bank, put the hook away) before the next one starts.
            for (int i = 0; i < 20 && isActive(plugin); i++)
            {
                Thread.sleep(500);
            }
        }
        current = null;
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
