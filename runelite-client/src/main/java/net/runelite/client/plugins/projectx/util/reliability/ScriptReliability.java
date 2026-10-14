package net.runelite.client.plugins.projectx.util.reliability;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2State;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.nio.channels.ClosedByInterruptException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Script reliability and recovery: helps a script notice when it is failing or stuck and
 * either get back on track or stop cleanly, instead of hanging, looping forever or
 * carrying on in a broken state. It only observes, checks, retries and stops; it never
 * changes how the script plays.
 *
 * <h2>Using it from a script</h2>
 * Every {@code Script} has one, from {@code reliability()}. Nothing changes for a script
 * that never calls it.
 * <ol>
 *   <li>Wrap actions that can fail in a {@link #step guarded step}: preconditions, the
 *       action, how to confirm it worked. A failed attempt is retried with a growing wait,
 *       up to the player's "Retries per action".</li>
 *   <li>Give a {@link #onRecover recovery handler} for when a step still fails: walk back
 *       to the last {@link #checkpoint checkpoint}, re-bank, close an interface.</li>
 *   <li>Call {@link #tick()} once per loop, after {@code super.run()}, so a loop that runs
 *       but gets nowhere is noticed. Mark real progress with {@link #checkpoint}, or give
 *       a {@link #trackProgress progress signal}.</li>
 *   <li>When a result says {@link ActionResult#isScriptStopping()}, return from the loop.</li>
 * </ol>
 *
 * <pre>{@code
 * public boolean run(MyConfig config) {
 *     reliability()
 *         .trackProgress(() -> Rs2Inventory.count("Logs"))
 *         .onRecover(ctx -> {
 *             Checkpoint cp = ctx.getCheckpoint();
 *             return cp != null && cp.getLocation() != null && Rs2Walker.walkTo(cp.getLocation());
 *         });
 *
 *     mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
 *         if (!ProjectX.isLoggedIn() || !super.run() || !reliability().tick()) return;
 *
 *         if (Rs2Inventory.isFull()) {
 *             ActionResult open = reliability().step("Open bank")
 *                 .action(Rs2Bank::openBank)
 *                 .verify(Rs2Bank::isOpen, 5_000)
 *                 .run();
 *             if (!open.isSuccess()) return; // retried, recovered or stopped already
 *
 *             ActionResult deposit = reliability().step("Deposit logs")
 *                 .require(Preconditions.bankOpen())
 *                 .action(() -> Rs2Bank.depositAll("Logs"))
 *                 .verify(() -> !Rs2Inventory.hasItem("Logs"), 3_000)
 *                 .run();
 *             if (deposit.isSuccess()) reliability().checkpointAtPlayer("Banked");
 *         }
 *     }, 0, 600, TimeUnit.MILLISECONDS);
 *     return true;
 * }
 * }</pre>
 *
 * <h2>Escalation</h2>
 * A failed attempt is retried. A step that still fails is counted, logged, and handed to
 * the recovery handler. When the player's "Failures before stopping" is reached (failed
 * steps in a row, or the same step failing that many times without once succeeding, which
 * catches a loop that alternates between a working and a broken step), or a step is
 * {@link Outcome#UNRECOVERABLE}, the script is stopped safely: the player gets a message
 * saying why and the reason is logged. No progress for the player's "No-progress timeout"
 * calls the recovery handler once, and stops the script if a second timeout passes
 * without progress (straight away when there is no handler).
 *
 * <h2>Progress</h2>
 * Without a progress signal, a successful step or a checkpoint counts as progress. With
 * one, only a change in the signal or a checkpoint does, so a script that "succeeds" at
 * opening and closing the bank forever is still caught. Time spent logged out, paused,
 * on a break, or anywhere the loop did not tick for more than {@value #PAUSE_GAP_MS} ms
 * is not counted.
 *
 * <h2>Threads</h2>
 * Steps, ticks and recovery run on the calling script thread. Guarded steps refuse to run
 * on the client thread, because they wait between attempts. All state is safe to read from
 * any thread.
 *
 * <p>With "Enable script reliability" off, steps still run, once, and still return their
 * result, but nothing is retried, recovered, or stopped automatically.
 */
@Slf4j
public final class ScriptReliability
{
    /** A loop that has not ticked for this long was paused or blocked; that time is not "no progress". */
    public static final long PAUSE_GAP_MS = 30_000;

    private final String scriptName;
    private final Supplier<ReliabilitySettings> settings;
    private final ReliabilityClock clock;
    private final BooleanSupplier scriptActive;
    private final BooleanSupplier clientThreadCheck;
    private final BooleanSupplier pausedCheck;
    private final SafeStopHandler safeStopHandler;

    private final Object lock = new Object();
    private final AtomicBoolean inRecovery = new AtomicBoolean();
    private volatile String displayName;
    private volatile RecoveryHandler recoveryHandler;

    // Guarded by lock.
    private int consecutiveFailures;
    private final Map<String, Integer> stepFailures = new HashMap<>();
    private Checkpoint checkpoint;
    private long idleMs;
    private long lastObservedAt = -1;
    private boolean stallRecoveryTried;
    private Supplier<?> progressSignal;
    private boolean signalSampled;
    private Object lastSignal;
    private boolean stopped;
    private String stopReason;

    private ScriptReliability(Builder b)
    {
        this.scriptName = b.scriptName;
        this.displayName = b.scriptName;
        this.settings = b.settings;
        this.clock = b.clock;
        this.scriptActive = b.scriptActive;
        this.clientThreadCheck = b.clientThreadCheck;
        this.pausedCheck = b.pausedCheck;
        this.safeStopHandler = b.safeStopHandler;
    }

    public static Builder builder(String scriptName)
    {
        return new Builder(scriptName);
    }

    // ---- configuration by the script

    /** The name the player sees if the script is stopped. Defaults to the script's class name. */
    public ScriptReliability named(String name)
    {
        if (name != null && !name.trim().isEmpty())
        {
            this.displayName = name.trim();
        }
        return this;
    }

    /** What to do when a step fails after its retries, or nothing progresses. Null removes it. */
    public ScriptReliability onRecover(RecoveryHandler handler)
    {
        this.recoveryHandler = handler;
        return this;
    }

    /**
     * Something that changes when the script gets somewhere: an item count, total XP, a
     * quest step. Read on the script thread at every tick and step. Null removes it.
     */
    public ScriptReliability trackProgress(Supplier<?> signal)
    {
        synchronized (lock)
        {
            progressSignal = signal;
            signalSampled = false;
            lastSignal = null;
        }
        return this;
    }

    // ---- use in the loop

    /** Describes a guarded step; call {@link GuardedStep#run()} to run it. */
    public GuardedStep step(String name)
    {
        return new GuardedStep(this, name);
    }

    /**
     * Once per loop, after {@code super.run()}: counts time without progress and escalates
     * when the no-progress timeout passes. Returns false when the script has been stopped
     * or is being switched off, and the loop should return.
     */
    public boolean tick()
    {
        if (isOnClientThread())
        {
            return !isStopping();
        }
        return !observe(settings()) && !isStopping();
    }

    /** Marks that the script got somewhere, restarting the no-progress timer. */
    public void progress()
    {
        synchronized (lock)
        {
            markProgressLocked();
        }
    }

    /** Marks the last known-good step. Counts as progress. */
    public void checkpoint(String name)
    {
        checkpoint(name, null);
    }

    /** Marks the last known-good step and where it happened. Counts as progress. */
    public void checkpoint(String name, WorldPoint location)
    {
        Checkpoint cp = new Checkpoint(name == null ? "checkpoint" : name, location, clock.nowMillis());
        synchronized (lock)
        {
            checkpoint = cp;
            markProgressLocked();
        }
        ReliabilitySettings s = settings();
        detail(s, ReliabilityLog.line(scriptName, null, "checkpoint", "name", cp.toString()));
    }

    /** {@link #checkpoint(String, WorldPoint)} at the player's current position. */
    public void checkpointAtPlayer(String name)
    {
        checkpoint(name, Rs2Player.getWorldLocation());
    }

    /** The last checkpoint, or null. */
    public Checkpoint getCheckpoint()
    {
        synchronized (lock)
        {
            return checkpoint;
        }
    }

    /**
     * Stops the script now, telling the player why: for a script that knows it cannot go
     * on (out of supplies, goal reached). Works whether or not the master toggle is on.
     */
    public void stop(String reason)
    {
        safeStop(reason == null || reason.trim().isEmpty() ? "stopped by the script" : reason);
    }

    public boolean isStopped()
    {
        synchronized (lock)
        {
            return stopped;
        }
    }

    /** Why the script was stopped, or null. */
    public String getStopReason()
    {
        synchronized (lock)
        {
            return stopReason;
        }
    }

    public int getConsecutiveFailures()
    {
        synchronized (lock)
        {
            return consecutiveFailures;
        }
    }

    /** Times {@code step} has failed since it last succeeded. */
    public int getStepFailures(String step)
    {
        synchronized (lock)
        {
            return stepFailures.getOrDefault(step, 0);
        }
    }

    public ReliabilitySettings getSettings()
    {
        return settings();
    }

    /**
     * Forgets failures, checkpoint, no-progress time and a previous stop, so the script can
     * start again. {@code Script.shutdown()} calls it; the recovery handler and progress
     * signal are kept, since scripts usually set those once.
     */
    public void reset()
    {
        synchronized (lock)
        {
            consecutiveFailures = 0;
            stepFailures.clear();
            checkpoint = null;
            idleMs = 0;
            lastObservedAt = -1;
            stallRecoveryTried = false;
            signalSampled = false;
            lastSignal = null;
            stopped = false;
            stopReason = null;
        }
    }

    // ---- the guarded step

    ActionResult execute(GuardedStep step)
    {
        String name = step.getName();
        if (isOnClientThread())
        {
            log.error(ReliabilityLog.line(scriptName, name, "refused", "reason", "guarded steps cannot run on the client thread"));
            return new ActionResult(name, Outcome.UNRECOVERABLE, 0, "called on the client thread", null, false);
        }
        if (isStopping())
        {
            return stoppingResult(name, 0);
        }

        ReliabilitySettings s = settings();
        if (observe(s))
        {
            return stoppingResult(name, 0);
        }

        int allowedRetries = 0;
        if (s.isEnabled())
        {
            allowedRetries = step.getMaxRetries() < 0 ? s.getMaxRetries() : Math.min(step.getMaxRetries(), s.getMaxRetries());
        }
        int maxAttempts = 1 + allowedRetries;

        int attempt = 0;
        String reason = null;
        Throwable lastError = null;
        try
        {
            while (true)
            {
                Precondition unmet = firstUnmet(step);
                if (unmet != null)
                {
                    Outcome outcome = unmet.isFatal() ? Outcome.UNRECOVERABLE : Outcome.RECOVERABLE;
                    return fail(s, name, outcome, attempt, "precondition not met: " + unmet.getDescription(), null);
                }

                attempt++;
                Throwable attemptError = null;
                boolean issued;
                try
                {
                    issued = step.getAction().getAsBoolean();
                }
                catch (RuntimeException e)
                {
                    if (causedByInterrupt(e))
                    {
                        Thread.currentThread().interrupt();
                        return stoppingResult(name, attempt);
                    }
                    if (step.classify(e) == Outcome.UNRECOVERABLE)
                    {
                        return fail(s, name, Outcome.UNRECOVERABLE, attempt, unrecoverableReason(e), e);
                    }
                    issued = false;
                    attemptError = e;
                }

                if (issued)
                {
                    if (step.getVerify() == null || awaitVerify(step))
                    {
                        return succeed(s, name, attempt);
                    }
                    reason = "not confirmed within " + step.getVerifyTimeoutMs() + "ms";
                }
                else if (attemptError != null)
                {
                    reason = "action failed: " + ReliabilityLog.describe(attemptError);
                }
                else
                {
                    reason = "action could not be carried out";
                }
                lastError = attemptError;

                if (isStopping())
                {
                    return stoppingResult(name, attempt);
                }
                if (attempt >= maxAttempts)
                {
                    break;
                }

                long wait = Backoff.delayAfterAttempt(attempt, step.getBackoffBaseMs(), step.getBackoffMaxMs());
                detail(s, ReliabilityLog.line(scriptName, name, "retry",
                    "attempt", attempt + "/" + maxAttempts, "waitMs", wait, "reason", reason));
                clock.sleep(wait);
                if (isStopping())
                {
                    return stoppingResult(name, attempt);
                }
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return stoppingResult(name, attempt);
        }
        catch (UnrecoverableActionException e)
        {
            // From a precondition or the verify check.
            return fail(s, name, Outcome.UNRECOVERABLE, attempt, unrecoverableReason(e), e);
        }
        return fail(s, name, Outcome.RECOVERABLE, attempt,
            reason + (attempt > 1 ? " (" + attempt + " attempts)" : ""), lastError);
    }

    private Precondition firstUnmet(GuardedStep step)
    {
        for (Precondition p : step.getPreconditions())
        {
            if (!p.test())
            {
                return p;
            }
        }
        return null;
    }

    private boolean awaitVerify(GuardedStep step) throws InterruptedException
    {
        BooleanSupplier condition = step.getVerify();
        long deadline = clock.nowMillis() + step.getVerifyTimeoutMs();
        while (true)
        {
            if (Thread.currentThread().isInterrupted())
            {
                throw new InterruptedException();
            }
            if (testVerify(condition))
            {
                return true;
            }
            long now = clock.nowMillis();
            if (now >= deadline)
            {
                return false;
            }
            clock.sleep(Math.min(step.getPollMs(), deadline - now));
        }
    }

    private static boolean testVerify(BooleanSupplier condition) throws InterruptedException
    {
        try
        {
            return condition.getAsBoolean();
        }
        catch (UnrecoverableActionException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            if (causedByInterrupt(e))
            {
                throw new InterruptedException();
            }
            return false;
        }
    }

    private ActionResult succeed(ReliabilitySettings s, String name, int attempts)
    {
        synchronized (lock)
        {
            consecutiveFailures = 0;
            stepFailures.remove(name);
            if (progressSignal == null)
            {
                markProgressLocked();
            }
        }
        detail(s, ReliabilityLog.line(scriptName, name, "success", "attempts", attempts));
        return new ActionResult(name, Outcome.SUCCESS, attempts, null, null, false);
    }

    private ActionResult fail(ReliabilitySettings s, String name, Outcome outcome, int attempts, String reason, Throwable error)
    {
        String clean = ReliabilityLog.sanitize(reason);
        ActionResult result = new ActionResult(name, outcome, attempts, clean, error, false);
        if (!s.isEnabled())
        {
            detail(s, ReliabilityLog.line(scriptName, name, "step-failed", "outcome", outcome.name(),
                "attempts", attempts, "reason", clean, "escalation", "off"));
            return result;
        }
        if (outcome == Outcome.UNRECOVERABLE)
        {
            safeStop("'" + name + "' cannot continue: " + clean);
            return result.stopping();
        }

        int consecutive;
        int forStep;
        Checkpoint cp;
        synchronized (lock)
        {
            consecutive = ++consecutiveFailures;
            forStep = stepFailures.merge(name, 1, Integer::sum);
            cp = checkpoint;
        }
        int max = s.getMaxConsecutiveFailures();
        log.info(ReliabilityLog.line(scriptName, name, "step-failed", "outcome", outcome.name(),
            "attempts", attempts, "consecutiveFailures", consecutive + "/" + max,
            "stepFailures", forStep + "/" + max, "reason", clean));

        if (forStep >= max)
        {
            safeStop("'" + name + "' failed " + forStep + " times without succeeding (last: " + clean + ")");
            return result.stopping();
        }
        if (consecutive >= max)
        {
            safeStop(consecutive + " steps failed in a row (last: '" + name + "', " + clean + ")");
            return result.stopping();
        }
        if (runRecovery(s, new RecoveryContext(RecoveryContext.Trigger.STEP_FAILED, name, clean, consecutive, forStep, cp)))
        {
            return result.stopping();
        }
        return result;
    }

    // ---- no progress

    /** Counts time and checks the no-progress timeout. True when the script is now stopping. */
    private boolean observe(ReliabilitySettings s)
    {
        long now = clock.nowMillis();
        boolean paused = isPaused();
        Supplier<?> signal;
        synchronized (lock)
        {
            if (lastObservedAt >= 0)
            {
                long gap = now - lastObservedAt;
                if (!paused && gap > 0 && gap <= PAUSE_GAP_MS)
                {
                    idleMs += gap;
                }
            }
            lastObservedAt = now;
            signal = progressSignal;
        }

        if (signal != null)
        {
            Object value;
            boolean read = true;
            try
            {
                value = signal.get();
            }
            catch (RuntimeException e)
            {
                value = null;
                read = false;
            }
            if (read)
            {
                synchronized (lock)
                {
                    if (signal == progressSignal && (!signalSampled || !Objects.equals(value, lastSignal)))
                    {
                        signalSampled = true;
                        lastSignal = value;
                        markProgressLocked();
                    }
                }
            }
        }

        if (!s.isEnabled() || isStopping())
        {
            return isStopping();
        }

        long timeout = s.getNoProgressTimeoutMillis();
        long idle;
        boolean recover;
        boolean triedBefore;
        int consecutive;
        Checkpoint cp;
        synchronized (lock)
        {
            idle = idleMs;
            if (idle < timeout)
            {
                return false;
            }
            triedBefore = stallRecoveryTried;
            recover = !stallRecoveryTried && recoveryHandler != null && !inRecovery.get();
            if (recover)
            {
                stallRecoveryTried = true;
                idleMs = 0;
            }
            consecutive = consecutiveFailures;
            cp = checkpoint;
        }

        String reason = "nothing progressed for " + (idle / 1000) + " seconds";
        if (recover)
        {
            log.info(ReliabilityLog.line(scriptName, null, "no-progress", "idleSeconds", idle / 1000,
                "timeoutSeconds", s.getNoProgressTimeoutSeconds(), "action", "recover"));
            return runRecovery(s, new RecoveryContext(RecoveryContext.Trigger.NO_PROGRESS, null, reason, consecutive, 0, cp));
        }
        if (inRecovery.get())
        {
            return false; // let the running recovery finish; it is checked again afterwards
        }
        safeStop(reason + (triedBefore ? ", even after trying to recover" : ""));
        return true;
    }

    private void markProgressLocked()
    {
        idleMs = 0;
        stallRecoveryTried = false;
    }

    // ---- recovery and stopping

    /** Runs the recovery handler, if any and not already running. True when the script is now stopping. */
    private boolean runRecovery(ReliabilitySettings s, RecoveryContext ctx)
    {
        RecoveryHandler handler = recoveryHandler;
        if (handler == null || !inRecovery.compareAndSet(false, true))
        {
            return isStopping();
        }
        String step = ctx.getStep();
        Checkpoint cp = ctx.getCheckpoint();
        log.info(ReliabilityLog.line(scriptName, step, "recovery-start", "trigger", ctx.getTrigger().name(),
            "reason", ctx.getReason(), "checkpoint", cp == null ? null : cp.toString()));
        try
        {
            boolean ok = handler.recover(ctx);
            log.info(ReliabilityLog.line(scriptName, step, "recovery-end", "recovered", ok));
        }
        catch (UnrecoverableActionException e)
        {
            inRecovery.set(false);
            safeStop("recovery could not fix it: " + unrecoverableReason(e));
            return true;
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return true;
        }
        catch (Exception e)
        {
            if (causedByInterrupt(e))
            {
                Thread.currentThread().interrupt();
                return true;
            }
            log.info(ReliabilityLog.line(scriptName, step, "recovery-error", "error", ReliabilityLog.describe(e)));
        }
        finally
        {
            inRecovery.set(false);
        }
        return isStopping();
    }

    private void safeStop(String reason)
    {
        String clean = ReliabilityLog.sanitize(reason);
        Checkpoint cp;
        int consecutive;
        synchronized (lock)
        {
            if (stopped)
            {
                return;
            }
            stopped = true;
            stopReason = clean;
            cp = checkpoint;
            consecutive = consecutiveFailures;
        }
        log.warn(ReliabilityLog.line(scriptName, null, "safe-stop", "reason", clean,
            "consecutiveFailures", consecutive, "checkpoint", cp == null ? null : cp.toString()));

        String message = displayName + " stopped itself so it would not carry on in a broken state."
            + "\n\nReason: " + clean
            + (cp == null ? "" : "\nLast good step: " + ReliabilityLog.sanitize(cp.getName()))
            + "\n\nRetry and stop limits are under Project X settings > Script reliability.";
        try
        {
            safeStopHandler.stop(message);
        }
        catch (RuntimeException e)
        {
            log.warn(ReliabilityLog.line(scriptName, null, "safe-stop-error", "error", ReliabilityLog.describe(e)));
        }
    }

    private ActionResult stoppingResult(String name, int attempts)
    {
        String why;
        synchronized (lock)
        {
            why = stopReason;
        }
        return new ActionResult(name, Outcome.UNRECOVERABLE, attempts,
            why != null ? "script stopped: " + why : "script is stopping", null, true);
    }

    // ---- helpers

    private boolean isStopping()
    {
        synchronized (lock)
        {
            if (stopped)
            {
                return true;
            }
        }
        return Thread.currentThread().isInterrupted() || !safeBoolean(scriptActive, true);
    }

    private boolean isOnClientThread()
    {
        return safeBoolean(clientThreadCheck, false);
    }

    private boolean isPaused()
    {
        // When in doubt, call it paused: that only ever delays a stop.
        return safeBoolean(pausedCheck, true);
    }

    private ReliabilitySettings settings()
    {
        ReliabilitySettings s = null;
        try
        {
            s = settings.get();
        }
        catch (RuntimeException ignored)
        {
            // fall through to the defaults
        }
        return s == null ? ReliabilitySettings.DEFAULTS : s;
    }

    private void detail(ReliabilitySettings s, String line)
    {
        if (s.isDetailedLogging())
        {
            log.info(line);
        }
        else
        {
            log.debug(line);
        }
    }

    private static String unrecoverableReason(Throwable e)
    {
        String msg = e.getMessage();
        return msg == null || msg.trim().isEmpty() ? ReliabilityLog.describe(e) : msg;
    }

    private static boolean safeBoolean(BooleanSupplier supplier, boolean fallback)
    {
        try
        {
            return supplier.getAsBoolean();
        }
        catch (RuntimeException | LinkageError e)
        {
            return fallback;
        }
    }

    static boolean causedByInterrupt(Throwable ex)
    {
        for (Throwable t = ex; t != null; t = t.getCause())
        {
            if (t instanceof InterruptedException || t instanceof ClosedByInterruptException)
            {
                return true;
            }
            if (t.getCause() == t)
            {
                break;
            }
        }
        return false;
    }

    /** The client thread, judged the same way {@code ClientThread} does it. */
    static boolean onClientThread()
    {
        ClientThread clientThread = ProjectX.getClientThread();
        return clientThread != null && clientThread.isClientThread();
    }

    /** Logged out, paused, or on a break: time that does not count as "no progress". */
    static boolean gamePaused()
    {
        return !ProjectX.isLoggedIn()
            || ProjectX.pauseAllScripts.get()
            || BreakHandlerScript.isBreakActive()
            || BreakHandlerScript.isMicroBreakActive()
            || BreakHandlerV2State.isBreakActive();
    }

    /** Wires a service to a script. {@code Script.reliability()} does this for every script. */
    public static final class Builder
    {
        private final String scriptName;
        private Supplier<ReliabilitySettings> settings = ReliabilitySettings::current;
        private ReliabilityClock clock = ReliabilityClock.SYSTEM;
        private BooleanSupplier scriptActive = () -> true;
        private BooleanSupplier clientThreadCheck = ScriptReliability::onClientThread;
        private BooleanSupplier pausedCheck = ScriptReliability::gamePaused;
        private SafeStopHandler safeStopHandler = ProjectX::showMessage;

        private Builder(String scriptName)
        {
            this.scriptName = scriptName == null || scriptName.trim().isEmpty() ? "Script" : scriptName.trim();
        }

        public Builder settings(Supplier<ReliabilitySettings> settings)
        {
            this.settings = Objects.requireNonNull(settings);
            return this;
        }

        public Builder clock(ReliabilityClock clock)
        {
            this.clock = Objects.requireNonNull(clock);
            return this;
        }

        /** False once the script's loop has been cancelled; guarded steps then do nothing. */
        public Builder scriptActive(BooleanSupplier scriptActive)
        {
            this.scriptActive = Objects.requireNonNull(scriptActive);
            return this;
        }

        public Builder clientThreadCheck(BooleanSupplier clientThreadCheck)
        {
            this.clientThreadCheck = Objects.requireNonNull(clientThreadCheck);
            return this;
        }

        public Builder pausedCheck(BooleanSupplier pausedCheck)
        {
            this.pausedCheck = Objects.requireNonNull(pausedCheck);
            return this;
        }

        public Builder onSafeStop(SafeStopHandler handler)
        {
            this.safeStopHandler = Objects.requireNonNull(handler);
            return this;
        }

        public ScriptReliability build()
        {
            return new ScriptReliability(this);
        }
    }
}
