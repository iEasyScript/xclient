package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.client.plugins.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Works through a plan on its own thread: start a script, watch for its step to end,
 * stop it, pause briefly (or take a BreakHandler break), start the next. Polls once a
 * second; never touches the client thread except through the helpers that hop onto it.
 *
 * <p>A step ends at the first of its conditions to be met, or when its script stops by
 * itself. A step whose script can't start, or stops with an error, has failed: its
 * {@link QueueStep.OnFailure} decides whether to skip it, retry it once or stop the queue.
 *
 * <p>BreakHandler breaks are respected whoever starts them: while one is on, a step's
 * clock stops and no script is started, so a script never starts logged out and an
 * hour of a script is an hour of it running.
 *
 * <p>Posts {@link ScriptQueueStepStarted}, {@link ScriptQueueStepEnded} and
 * {@link ScriptQueueFinished} on the event bus as it goes.
 */
@Slf4j
final class ScriptQueueRunner implements Runnable
{
    /** A script that is gone again this soon after starting never really started. */
    static final long FAILED_START_MS = 15_000;
    /** An error the script logged this long before it stopped by itself counts as why it stopped. */
    static final long ERROR_WINDOW_MS = 60_000;
    /** The wait before a failed step's one retry. */
    static final long RETRY_WAIT_MS = 5_000;

    /** What the panel is told as the queue goes. Called on the queue's thread. */
    interface Listener
    {
        void status(String text);

        /** Progress or the result of a step, shown under it in the list. */
        void step(int index, StepState state, String text);
    }

    enum StepState
    {
        RUNNING, DONE, FAILED
    }

    /** How a step went. */
    private enum StepResult
    {
        DONE, FAILED, ABORT, STOPPED
    }

    /** How one attempt at a step ended. */
    private static final class Attempt
    {
        final ScriptQueueStepEnded.Reason reason;
        final String detail;
        final long runMillis;

        Attempt(ScriptQueueStepEnded.Reason reason, String detail, long runMillis)
        {
            this.reason = reason;
            this.detail = detail;
            this.runMillis = runMillis;
        }
    }

    private final String planName;
    private final List<QueueStep> steps;
    /** Times through the steps; 0 is no limit. */
    private final int totalRounds;
    private final boolean logoutWhenDone;
    private final boolean breakBetween;
    private final int pauseMinSeconds;
    private final int pauseMaxSeconds;
    private final QueueHost host;
    private final QueueGameState game;
    /**
     * ---- EXTENSION POINT: pre-start requirements check ----
     * Asked before each step's script starts (and before a retry). A non-null answer is
     * why the step can't start, and fails the step under its "if it fails" setting.
     * Defaults to {@link ScriptQueuePreStartCheck#ALWAYS_OK}; set from outside with
     * {@link ScriptQueuePlugin#setPreStartCheck}. Called in {@link #attempt}.
     */
    private final ScriptQueuePreStartCheck preStartCheck;
    private final Listener listener;

    private volatile boolean stopRequested;
    private volatile Plugin current;

    private int completed;
    private int failed;
    /** The latest failure, for the summary: "AIO Magic didn't start (...)". */
    private String lastFailure;
    /** The step being worked on has had its script started at least once. */
    private boolean ran;

    ScriptQueueRunner(QueuePlan plan, boolean logoutWhenDone, boolean breakBetween,
                      int pauseMinSeconds, int pauseMaxSeconds,
                      QueueHost host, QueueGameState game, ScriptQueuePreStartCheck preStartCheck, Listener listener)
    {
        this.planName = plan.getName();
        this.steps = List.copyOf(plan.steps());
        this.totalRounds = plan.totalRounds();
        this.logoutWhenDone = logoutWhenDone;
        this.breakBetween = breakBetween;
        this.pauseMinSeconds = Math.max(0, Math.min(pauseMinSeconds, pauseMaxSeconds));
        this.pauseMaxSeconds = Math.max(pauseMinSeconds, pauseMaxSeconds);
        this.host = host;
        this.game = game;
        this.preStartCheck = preStartCheck == null ? ScriptQueuePreStartCheck.ALWAYS_OK : preStartCheck;
        this.listener = listener;
    }

    void requestStop()
    {
        stopRequested = true;
    }

    private long now()
    {
        return host.clock().millis();
    }

    private void status(String text)
    {
        listener.status(text);
    }

    @Override
    public void run()
    {
        long queueStarted = now();
        ScriptQueueFinished.Outcome outcome = ScriptQueueFinished.Outcome.COMPLETED;
        String reason = null;
        int round = 0;
        try
        {
            rounds:
            while (!stopRequested)
            {
                round++;
                int ranThisRound = 0;
                for (int i = 0; i < steps.size() && !stopRequested; i++)
                {
                    StepResult result = runStep(i, steps.get(i), round);
                    if (result == StepResult.ABORT)
                    {
                        outcome = ScriptQueueFinished.Outcome.ABORTED;
                        reason = "step " + (i + 1) + " failed: " + lastFailure;
                        break rounds;
                    }
                    if (ran)
                    {
                        ranThisRound++;
                    }
                    if (!stopRequested && i < steps.size() - 1)
                    {
                        betweenScripts();
                    }
                }
                if (stopRequested || (totalRounds > 0 && round >= totalRounds))
                {
                    break;
                }
                if (ranThisRound == 0)
                {
                    // Every step was already done or failed: going round again would only spin.
                    outcome = ScriptQueueFinished.Outcome.ABORTED;
                    reason = "nothing in the plan could run, so it stopped looping";
                    break;
                }
                betweenScripts();
            }

            if (stopRequested)
            {
                outcome = ScriptQueueFinished.Outcome.STOPPED;
                reason = "stopped by the player";
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            outcome = ScriptQueueFinished.Outcome.STOPPED;
            reason = "stopped by the player";
        }
        catch (Exception e)
        {
            log.warn("Script queue stopped on an error", e);
            outcome = ScriptQueueFinished.Outcome.ERROR;
            reason = "the queue hit an error (" + e.getClass().getSimpleName() + ")";
        }
        finally
        {
            Plugin running = current;
            if (running != null && host.isActive(running))
            {
                host.stop(running, true);
            }
            current = null;
        }

        String summary = summary(outcome, reason, round, now() - queueStarted);
        status(summary);
        host.post(new ScriptQueueFinished(planName, outcome, reason, round, completed, failed, now() - queueStarted, summary));
        if (logoutWhenDone && (outcome == ScriptQueueFinished.Outcome.COMPLETED || outcome == ScriptQueueFinished.Outcome.ABORTED))
        {
            log.info("Script queue: finished, logging out");
            host.logout();
        }
    }

    private String summary(ScriptQueueFinished.Outcome outcome, String reason, int rounds, long millis)
    {
        StringBuilder s = new StringBuilder();
        switch (outcome)
        {
            case COMPLETED:
                s.append("Queue finished");
                break;
            case STOPPED:
                s.append("Stopped");
                break;
            default:
                s.append("Queue stopped: ").append(reason);
                break;
        }
        s.append(". ").append(completed).append(completed == 1 ? " step" : " steps").append(" done");
        if (failed > 0)
        {
            s.append(", ").append(failed).append(" failed");
            if (outcome == ScriptQueueFinished.Outcome.COMPLETED && lastFailure != null)
            {
                s.append(" (last: ").append(lastFailure).append(")");
            }
        }
        if (rounds > 1)
        {
            s.append(", ").append(rounds).append(" rounds");
        }
        s.append(", in ").append(QueueStep.formatMinutes((int) (millis / 60_000))).append(".");
        return s.toString();
    }

    private StepResult runStep(int index, QueueStep step, int round) throws InterruptedException
    {
        ran = false;
        String prefix = "Step " + (index + 1) + "/" + steps.size() + ": " + step.getPluginName();
        StepGoals goals = new StepGoals(step.conditions(), game, host.clock());
        for (int attempt = 1; ; attempt++)
        {
            Attempt a = attempt(index, step, round, attempt, goals, prefix);
            boolean failure = a.reason == ScriptQueueStepEnded.Reason.FAILED;
            boolean retrying = failure && attempt == 1 && step.onFailure() == QueueStep.OnFailure.RETRY && !stopRequested;
            host.post(new ScriptQueueStepEnded(planName, index, steps.size(), round, attempt,
                step.getPluginClass(), step.getPluginName(), a.reason, a.detail, retrying, a.runMillis));

            if (a.reason == ScriptQueueStepEnded.Reason.QUEUE_STOPPED)
            {
                return StepResult.STOPPED;
            }
            if (!failure)
            {
                completed++;
                listener.step(index, StepState.DONE, a.detail);
                return StepResult.DONE;
            }

            log.info("Script queue: {} failed: {}", step.getPluginName(), a.detail);
            lastFailure = step.getPluginName() + " " + a.detail;
            if (retrying)
            {
                status(prefix + " " + a.detail + ". Retrying once.");
                listener.step(index, StepState.FAILED, a.detail + "; retrying");
                host.sleep(RETRY_WAIT_MS);
                if (stopRequested)
                {
                    return StepResult.STOPPED;
                }
                continue;
            }
            failed++;
            listener.step(index, StepState.FAILED, a.detail);
            if (step.onFailure() == QueueStep.OnFailure.STOP)
            {
                status(prefix + " " + a.detail + ". Stopping the queue.");
                host.sleep(2000);
                return StepResult.ABORT;
            }
            status(prefix + " " + a.detail + ". Skipping.");
            host.sleep(3000);
            return StepResult.FAILED;
        }
    }

    private Attempt attempt(int index, QueueStep step, int round, int attempt, StepGoals goals, String prefix)
        throws InterruptedException
    {
        Plugin plugin = host.find(step);
        if (plugin == null)
        {
            return failed("isn't installed");
        }

        // A break the BreakHandler began on its own during the pause: starting a
        // script now would start it logged out, and it would stop straight away.
        while (host.onBreak() && !stopRequested)
        {
            status(prefix + ": waiting for the break to end (" + host.breakStatus() + ")");
            host.sleep(1000);
        }
        if (stopRequested)
        {
            return new Attempt(ScriptQueueStepEnded.Reason.QUEUE_STOPPED, "the queue was stopped", 0);
        }

        StepGoals.Check before = goals.check();
        if (before.met)
        {
            String detail = ran ? before.reason : "already met (" + before.reason + ")";
            status(prefix + ": " + detail + ". " + (ran ? "Moving on." : "Skipping."));
            host.sleep(2000);
            return new Attempt(ran ? ScriptQueueStepEnded.Reason.CONDITION_MET : ScriptQueueStepEnded.Reason.ALREADY_MET,
                detail, 0);
        }

        // ---- EXTENSION POINT: pre-start requirements check (see the preStartCheck field)
        String notReady;
        try
        {
            notReady = preStartCheck.check(step);
        }
        catch (RuntimeException e)
        {
            log.warn("Script queue: the pre-start check failed on {}", step.getPluginName(), e);
            notReady = "the requirements check hit an error (" + e.getClass().getSimpleName() + ")";
        }
        if (notReady != null)
        {
            return failed("can't start: " + notReady);
        }

        status(prefix + ": starting");
        current = plugin;
        if (!host.isActive(plugin))
        {
            host.start(plugin);
        }
        ran = true;
        host.post(new ScriptQueueStepStarted(planName, index, steps.size(), round, attempt,
            step.getPluginClass(), step.getPluginName(), step.describe()));

        long started = now();
        long ranFor = 0;
        boolean seenActive = false;
        long lastTick = started;
        Attempt result = null;

        while (!stopRequested)
        {
            host.sleep(1000);
            long tick = now();
            long elapsed = tick - lastTick;
            lastTick = tick;

            if (host.onBreak())
            {
                // The script is paused, not finished: hold its clock until the break is over.
                started += elapsed;
                status(prefix + ": " + host.breakStatus());
                continue;
            }
            goals.addRunning(elapsed);
            ranFor += elapsed;

            boolean active = host.isActive(plugin);
            if (active)
            {
                seenActive = true;
            }
            long now = now();

            if (!active && (seenActive || now - started > 5000))
            {
                if (now - started < FAILED_START_MS)
                {
                    result = failed("didn't start (not owned, or it stopped straight away)", ranFor);
                }
                else
                {
                    String error = host.errorSince(plugin, Math.max(started, now - ERROR_WINDOW_MS));
                    result = error != null
                        ? failed("stopped with an error: " + error, ranFor)
                        : new Attempt(ScriptQueueStepEnded.Reason.STOPPED_BY_ITSELF, "stopped by itself", ranFor);
                }
                status(prefix + " " + result.detail + ".");
                log.info("Script queue: {} {}", step.getPluginName(), result.detail);
                host.sleep(2000);
                break;
            }

            StepGoals.Check check = goals.check();
            if (check.met)
            {
                result = new Attempt(ScriptQueueStepEnded.Reason.CONDITION_MET, check.reason, ranFor);
                status(prefix + ": " + check.reason + ".");
                break;
            }
            status(prefix + ": " + check.progress);
            listener.step(index, StepState.RUNNING, check.progress);
        }

        if (host.isActive(plugin))
        {
            host.stop(plugin, stopRequested);
            // Let it wind down (bank, put the hook away) before the next one starts.
            for (int i = 0; i < 20 && host.isActive(plugin); i++)
            {
                host.sleep(500);
            }
        }
        current = null;
        return result != null ? result : new Attempt(ScriptQueueStepEnded.Reason.QUEUE_STOPPED, "the queue was stopped", ranFor);
    }

    private static Attempt failed(String detail)
    {
        return failed(detail, 0);
    }

    private static Attempt failed(String detail, long runMillis)
    {
        return new Attempt(ScriptQueueStepEnded.Reason.FAILED, detail, runMillis);
    }

    private void betweenScripts() throws InterruptedException
    {
        if (!breakBetween || !host.takeBreak(() -> stopRequested, this::status))
        {
            pause();
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
            status("Short pause before the next script: " + s + "s");
            host.sleep(1000);
        }
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
