package net.runelite.client.plugins.projectx.util.reliability;

import net.runelite.api.coords.WorldPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Retry, backoff, classification, escalation, loop and stall detection, and checkpoints,
 * driven by a fake clock: nothing here waits for real or touches a game client.
 */
public class ScriptReliabilityTest
{
    private static final class FakeClock implements ReliabilityClock
    {
        long now = 1_000_000;
        final List<Long> sleeps = new ArrayList<>();

        @Override
        public long nowMillis()
        {
            return now;
        }

        @Override
        public void sleep(long millis)
        {
            sleeps.add(millis);
            now += millis;
        }

        long slept()
        {
            return sleeps.stream().mapToLong(Long::longValue).sum();
        }
    }

    private final FakeClock clock = new FakeClock();
    private final AtomicReference<ReliabilitySettings> settings =
        new AtomicReference<>(ReliabilitySettings.of(true, 3, 5, 120, false));
    private final List<String> stops = new ArrayList<>();
    private volatile boolean active = true;
    private volatile boolean onClientThread;
    private volatile boolean paused;
    private ScriptReliability service;

    @Before
    public void setUp()
    {
        service = ScriptReliability.builder("TestScript")
            .settings(settings::get)
            .clock(clock)
            .scriptActive(() -> active)
            .clientThreadCheck(() -> onClientThread)
            .pausedCheck(() -> paused)
            .onSafeStop(stops::add)
            .build();
    }

    @After
    public void clearInterrupt()
    {
        Thread.interrupted();
    }

    private ActionResult failingStep(String name)
    {
        return service.step(name).action(() -> false).run();
    }

    private ActionResult succeedingStep(String name)
    {
        return service.step(name).action(() -> true).run();
    }

    // ---- retries, backoff and verification

    @Test
    public void succeedsFirstTimeWithoutWaiting()
    {
        ActionResult r = succeedingStep("Open bank");

        assertTrue(r.isSuccess());
        assertEquals(Outcome.SUCCESS, r.getOutcome());
        assertEquals(1, r.getAttempts());
        assertNull(r.getReason());
        assertTrue(clock.sleeps.isEmpty());
        assertEquals(0, service.getConsecutiveFailures());
    }

    @Test
    public void retriesWithExponentialBackoffUntilVerified()
    {
        AtomicInteger checks = new AtomicInteger();
        ActionResult r = service.step("Open bank")
            .action(() -> true)
            .verify(() -> checks.incrementAndGet() >= 3, 0)
            .run();

        assertTrue(r.isSuccess());
        assertEquals(3, r.getAttempts());
        assertEquals(Arrays.asList(600L, 1200L), clock.sleeps);
    }

    @Test
    public void exhaustedRetriesAreRecoverable()
    {
        ActionResult r = service.step("Open bank").action(() -> true).verify(() -> false, 0).run();

        assertEquals(Outcome.RECOVERABLE, r.getOutcome());
        assertEquals(4, r.getAttempts());
        assertEquals(Arrays.asList(600L, 1200L, 2400L), clock.sleeps);
        assertTrue(r.getReason(), r.getReason().contains("not confirmed"));
        assertFalse(r.isScriptStopping());
        assertEquals(1, service.getConsecutiveFailures());
        assertEquals(1, service.getStepFailures("Open bank"));
        assertTrue(stops.isEmpty());
    }

    @Test
    public void backoffIsCappedPerStep()
    {
        settings.set(ReliabilitySettings.of(true, 10, 5, 120, false));
        service.step("Walk").action(() -> false).backoff(1000, 3000).run();

        assertEquals(Arrays.asList(1000L, 2000L, 3000L, 3000L, 3000L, 3000L, 3000L, 3000L, 3000L, 3000L), clock.sleeps);
    }

    @Test
    public void stepCannotRetryMoreThanThePlayerAllows()
    {
        assertEquals(4, service.step("A").action(() -> false).maxRetries(9).run().getAttempts());
        assertEquals(2, service.step("B").action(() -> false).maxRetries(1).run().getAttempts());

        settings.set(ReliabilitySettings.of(true, 0, 5, 120, false));
        assertEquals(1, service.step("C").action(() -> false).run().getAttempts());
    }

    @Test
    public void verifyIsPolledUntilItsTimeout()
    {
        long start = clock.now;
        ActionResult ok = service.step("Chop")
            .action(() -> true)
            .verify(() -> clock.now - start >= 400, 1000)
            .pollEvery(100)
            .run();

        assertTrue(ok.isSuccess());
        assertEquals(Arrays.asList(100L, 100L, 100L, 100L), clock.sleeps);

        clock.sleeps.clear();
        settings.set(ReliabilitySettings.of(true, 0, 5, 120, false));
        ActionResult timedOut = service.step("Chop").action(() -> true).verify(() -> false, 1000).pollEvery(250).run();

        assertEquals(Outcome.RECOVERABLE, timedOut.getOutcome());
        assertEquals(1000L, clock.slept());
    }

    @Test
    public void actionThatCannotBeCarriedOutSkipsVerify()
    {
        AtomicInteger verifies = new AtomicInteger();
        ActionResult r = service.step("Click tree")
            .action(() -> false)
            .verify(() -> verifies.incrementAndGet() > 0, 1000)
            .run();

        assertEquals(0, verifies.get());
        assertEquals(Outcome.RECOVERABLE, r.getOutcome());
        assertTrue(r.getReason().contains("could not be carried out"));
    }

    @Test
    public void performCountsAsCarriedOut()
    {
        AtomicInteger runs = new AtomicInteger();
        ActionResult r = service.step("Note").perform(runs::incrementAndGet).run();

        assertTrue(r.isSuccess());
        assertEquals(1, runs.get());
    }

    // ---- classification

    @Test
    public void ordinaryExceptionIsRecoverableAndRetried()
    {
        AtomicInteger runs = new AtomicInteger();
        IllegalStateException boom = new IllegalStateException("menu missing");
        ActionResult r = service.step("Bank").action(() ->
        {
            runs.incrementAndGet();
            throw boom;
        }).run();

        assertEquals(4, runs.get());
        assertEquals(Outcome.RECOVERABLE, r.getOutcome());
        assertSame(boom, r.getError());
        assertTrue(r.getReason().contains("IllegalStateException: menu missing"));
        assertTrue(stops.isEmpty());
    }

    @Test
    public void unrecoverableExceptionStopsAtOnce()
    {
        AtomicInteger runs = new AtomicInteger();
        ActionResult r = service.step("Get pickaxe").action(() ->
        {
            runs.incrementAndGet();
            throw new UnrecoverableActionException("No pickaxe in the inventory or bank");
        }).run();

        assertEquals(1, runs.get());
        assertEquals(Outcome.UNRECOVERABLE, r.getOutcome());
        assertTrue(r.isScriptStopping());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0), stops.get(0).contains("No pickaxe in the inventory or bank"));
        assertTrue(service.isStopped());
    }

    @Test
    public void classifierCanMarkExceptionsUnrecoverable()
    {
        ActionResult r = service.step("Cast")
            .action(() ->
            {
                throw new IllegalArgumentException("no runes");
            })
            .classifier(t -> t instanceof IllegalArgumentException ? Outcome.UNRECOVERABLE : null)
            .run();

        assertEquals(Outcome.UNRECOVERABLE, r.getOutcome());
        assertEquals(1, r.getAttempts());
        assertEquals(1, stops.size());
    }

    @Test
    public void unmetPreconditionSkipsTheAction()
    {
        AtomicInteger runs = new AtomicInteger();
        ActionResult r = service.step("Deposit")
            .require(Precondition.of("bank open", () -> false))
            .action(() -> runs.incrementAndGet() > 0)
            .run();

        assertEquals(0, runs.get());
        assertEquals(0, r.getAttempts());
        assertEquals(Outcome.RECOVERABLE, r.getOutcome());
        assertTrue(r.getReason().contains("precondition not met: bank open"));
        assertEquals(1, service.getConsecutiveFailures());
    }

    @Test
    public void fatalPreconditionStops()
    {
        ActionResult r = service.step("Cut")
            .require(Precondition.of("has an axe", () -> false).fatal())
            .action(() -> true)
            .run();

        assertEquals(Outcome.UNRECOVERABLE, r.getOutcome());
        assertTrue(r.isScriptStopping());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0).contains("has an axe"));
    }

    @Test
    public void preconditionThatThrowsIsNotMet()
    {
        ActionResult r = service.step("Deposit")
            .require(Precondition.of("bank open", () ->
            {
                throw new NullPointerException();
            }))
            .action(() -> true)
            .run();

        assertEquals(Outcome.RECOVERABLE, r.getOutcome());
        assertEquals(0, r.getAttempts());
    }

    @Test
    public void preconditionsAreCheckedAgainBeforeEachRetry()
    {
        AtomicInteger checks = new AtomicInteger();
        ActionResult r = service.step("Deposit")
            .require(Precondition.of("bank open", () -> checks.incrementAndGet() == 1))
            .action(() -> false)
            .run();

        assertEquals(1, r.getAttempts());
        assertTrue(r.getReason().contains("precondition not met"));
    }

    // ---- escalation, loop detection and safe stop

    @Test
    public void stopsSafelyAfterTooManyFailuresInARow()
    {
        settings.set(ReliabilitySettings.of(true, 0, 3, 120, false));

        assertFalse(failingStep("A").isScriptStopping());
        assertFalse(failingStep("B").isScriptStopping());
        ActionResult third = failingStep("C");

        assertTrue(third.isScriptStopping());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0), stops.get(0).contains("3 steps failed in a row"));
        assertTrue(stops.get(0).contains("TestScript"));

        AtomicInteger runs = new AtomicInteger();
        ActionResult after = service.step("D").action(() -> runs.incrementAndGet() > 0).run();
        assertEquals(0, runs.get());
        assertTrue(after.isScriptStopping());
        assertEquals("only one stop", 1, stops.size());
    }

    @Test
    public void successResetsTheFailureCount()
    {
        settings.set(ReliabilitySettings.of(true, 0, 3, 120, false));
        failingStep("A");
        failingStep("A");
        succeedingStep("A");

        assertEquals(0, service.getConsecutiveFailures());
        assertEquals(0, service.getStepFailures("A"));
        failingStep("A");
        failingStep("A");
        assertTrue(stops.isEmpty());
    }

    @Test
    public void sameStepFailingRepeatedlyIsCaughtEvenBetweenSuccesses()
    {
        settings.set(ReliabilitySettings.of(true, 0, 3, 120, false));
        ActionResult last = null;
        for (int i = 0; i < 3; i++)
        {
            assertTrue(succeedingStep("Walk to bank").isSuccess());
            last = failingStep("Open bank");
            assertTrue(service.getConsecutiveFailures() <= 1);
        }

        assertTrue(last.isScriptStopping());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0), stops.get(0).contains("'Open bank' failed 3 times"));
    }

    @Test
    public void recoveryHandlerGetsTheFailureAndCheckpoint()
    {
        List<RecoveryContext> calls = new ArrayList<>();
        service.onRecover(ctx ->
        {
            calls.add(ctx);
            return true;
        });
        service.checkpoint("At the bank", new WorldPoint(3200, 3201, 0));

        ActionResult r = failingStep("Open bank");

        assertFalse(r.isScriptStopping());
        assertEquals(1, calls.size());
        RecoveryContext ctx = calls.get(0);
        assertEquals(RecoveryContext.Trigger.STEP_FAILED, ctx.getTrigger());
        assertEquals("Open bank", ctx.getStep());
        assertEquals(1, ctx.getConsecutiveFailures());
        assertEquals(1, ctx.getStepFailures());
        assertNotNull(ctx.getCheckpoint());
        assertEquals("At the bank", ctx.getCheckpoint().getName());
        assertEquals(new WorldPoint(3200, 3201, 0), ctx.getCheckpoint().getLocation());
    }

    @Test
    public void recoveryIsNotNested()
    {
        AtomicInteger calls = new AtomicInteger();
        service.onRecover(ctx ->
        {
            calls.incrementAndGet();
            return failingStep("Walk back").isSuccess();
        });

        failingStep("Open bank");

        assertEquals(1, calls.get());
        assertEquals(2, service.getConsecutiveFailures());
    }

    @Test
    public void recoveryThatCannotFixItStops()
    {
        service.onRecover(ctx ->
        {
            throw new UnrecoverableActionException("bank is unreachable");
        });

        ActionResult r = failingStep("Open bank");

        assertTrue(r.isScriptStopping());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0).contains("bank is unreachable"));
    }

    @Test
    public void recoveryThatThrowsIsLoggedNotFatal()
    {
        service.onRecover(ctx ->
        {
            throw new IllegalStateException("oops");
        });

        ActionResult r = failingStep("Open bank");

        assertFalse(r.isScriptStopping());
        assertTrue(stops.isEmpty());
    }

    @Test
    public void scriptCanStopItselfWithAReason()
    {
        service.named("Auto Woodcutter").checkpoint("Banked");
        service.stop("Out of food");

        assertTrue(service.isStopped());
        assertEquals("Out of food", service.getStopReason());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0).startsWith("Auto Woodcutter"));
        assertTrue(stops.get(0).contains("Last good step: Banked"));
    }

    // ---- master toggle and live settings

    @Test
    public void disabledRunsOnceAndNeverEscalates()
    {
        settings.set(ReliabilitySettings.of(false, 3, 1, 15, false));
        AtomicInteger recoveries = new AtomicInteger();
        service.onRecover(ctx -> recoveries.incrementAndGet() > 0);

        for (int i = 0; i < 10; i++)
        {
            ActionResult r = failingStep("Open bank");
            assertEquals(1, r.getAttempts());
            assertEquals(Outcome.RECOVERABLE, r.getOutcome());
        }
        ActionResult fatal = service.step("X").action(() ->
        {
            throw new UnrecoverableActionException("nope");
        }).run();

        assertEquals(Outcome.UNRECOVERABLE, fatal.getOutcome());
        assertFalse(fatal.isScriptStopping());
        assertEquals(0, recoveries.get());
        assertTrue(stops.isEmpty());
        assertTrue(clock.sleeps.isEmpty());
    }

    @Test
    public void settingsChangesApplyToTheNextStep()
    {
        assertEquals(4, failingStep("A").getAttempts());
        settings.set(ReliabilitySettings.of(true, 1, 5, 120, false));
        assertEquals(2, failingStep("A").getAttempts());
    }

    // ---- not running the action at all

    @Test
    public void interruptedThreadRunsNothing()
    {
        AtomicInteger runs = new AtomicInteger();
        Thread.currentThread().interrupt();

        ActionResult r = service.step("A").action(() -> runs.incrementAndGet() > 0).run();

        assertEquals(0, runs.get());
        assertTrue(r.isScriptStopping());
        assertTrue(stops.isEmpty());
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void interruptDuringActionEndsTheStepWithoutStopping()
    {
        ActionResult r = service.step("A").action(() ->
        {
            throw new RuntimeException(new InterruptedException());
        }).run();

        assertEquals(1, r.getAttempts());
        assertTrue(r.isScriptStopping());
        assertTrue(Thread.currentThread().isInterrupted());
        assertTrue(stops.isEmpty());
        assertEquals(0, service.getConsecutiveFailures());
    }

    @Test
    public void cancelledScriptRunsNothing()
    {
        active = false;
        AtomicInteger runs = new AtomicInteger();

        ActionResult r = service.step("A").action(() -> runs.incrementAndGet() > 0).run();

        assertEquals(0, runs.get());
        assertTrue(r.isScriptStopping());
        assertFalse(service.tick());
    }

    @Test
    public void refusesToRunOnTheClientThread()
    {
        onClientThread = true;
        AtomicInteger runs = new AtomicInteger();

        ActionResult r = service.step("A").action(() -> runs.incrementAndGet() > 0).run();

        assertEquals(0, runs.get());
        assertEquals(Outcome.UNRECOVERABLE, r.getOutcome());
        assertFalse(r.isScriptStopping());
        assertTrue(stops.isEmpty());
        assertTrue(clock.sleeps.isEmpty());
    }

    // ---- no progress

    /** Ticks once a second until the service says stop; returns how many ticks that took, or -1. */
    private int tickSeconds(int max)
    {
        for (int i = 1; i <= max; i++)
        {
            clock.now += 1000;
            if (!service.tick())
            {
                return i;
            }
        }
        return -1;
    }

    @Test
    public void noProgressWithoutRecoveryStopsAtTheTimeout()
    {
        service.tick();

        assertEquals(120, tickSeconds(300));
        assertEquals(1, stops.size());
        assertTrue(stops.get(0), stops.get(0).contains("nothing progressed for 120 seconds"));
    }

    @Test
    public void noProgressRecoversOnceThenStops()
    {
        List<RecoveryContext> calls = new ArrayList<>();
        service.onRecover(ctx ->
        {
            calls.add(ctx);
            return true;
        });
        service.tick();

        assertEquals(240, tickSeconds(600));
        assertEquals(1, calls.size());
        assertEquals(RecoveryContext.Trigger.NO_PROGRESS, calls.get(0).getTrigger());
        assertNull(calls.get(0).getStep());
        assertEquals(1, stops.size());
        assertTrue(stops.get(0).contains("even after trying to recover"));
    }

    @Test
    public void progressRestartsTheTimer()
    {
        service.tick();
        assertEquals(-1, tickSeconds(100));
        service.progress();
        assertEquals(-1, tickSeconds(100));
        service.checkpoint("Half way");
        assertEquals(-1, tickSeconds(100));
        assertTrue(succeedingStep("Chop").isSuccess());
        assertEquals(-1, tickSeconds(100));
        assertTrue(stops.isEmpty());
    }

    @Test
    public void pausedTimeDoesNotCount()
    {
        service.tick();
        paused = true;
        assertEquals(-1, tickSeconds(500));
        paused = false;
        assertEquals(-1, tickSeconds(119));
        assertEquals(1, tickSeconds(5));
    }

    @Test
    public void aLongGapBetweenTicksDoesNotCount()
    {
        service.tick();
        clock.now += 10 * 60_000;
        assertTrue(service.tick());
        assertEquals(-1, tickSeconds(119));
        assertEquals(1, tickSeconds(5));
    }

    @Test
    public void withAProgressSignalOnlyItsChangesCount()
    {
        AtomicInteger logs = new AtomicInteger();
        service.trackProgress(logs::get);
        service.tick();

        // Steps keep "succeeding" but the signal never moves: that is a loop, not progress.
        int stoppedAt = -1;
        for (int i = 1; i <= 200 && stoppedAt < 0; i++)
        {
            clock.now += 1000;
            if (succeedingStep("Open and close bank").isScriptStopping() || !service.tick())
            {
                stoppedAt = i;
            }
        }
        assertEquals(120, stoppedAt);
    }

    @Test
    public void aChangingProgressSignalKeepsTheScriptGoing()
    {
        AtomicInteger logs = new AtomicInteger();
        service.trackProgress(logs::get);
        service.tick();

        for (int i = 0; i < 10; i++)
        {
            assertEquals(-1, tickSeconds(100));
            logs.incrementAndGet();
        }
        assertTrue(stops.isEmpty());
    }

    @Test
    public void noProgressTimeoutFollowsSettings()
    {
        settings.set(ReliabilitySettings.of(true, 3, 5, 15, false));
        service.tick();
        assertEquals(15, tickSeconds(300));
    }

    @Test
    public void disabledNeverStopsForNoProgress()
    {
        settings.set(ReliabilitySettings.of(false, 3, 5, 15, false));
        service.tick();
        assertEquals(-1, tickSeconds(1000));
        assertTrue(stops.isEmpty());
    }

    // ---- checkpoints and reset

    @Test
    public void checkpointIsKeptUntilReset()
    {
        assertNull(service.getCheckpoint());
        service.checkpoint("Mined ore");
        service.checkpoint("Banked", new WorldPoint(1, 2, 0));

        Checkpoint cp = service.getCheckpoint();
        assertEquals("Banked", cp.getName());
        assertEquals(new WorldPoint(1, 2, 0), cp.getLocation());
        assertEquals(clock.now, cp.getAtMillis());

        failingStep("A");
        service.stop("done");
        service.reset();

        assertNull(service.getCheckpoint());
        assertEquals(0, service.getConsecutiveFailures());
        assertEquals(0, service.getStepFailures("A"));
        assertFalse(service.isStopped());
        assertNull(service.getStopReason());
        assertTrue(succeedingStep("B").isSuccess());
    }

    @Test(expected = IllegalStateException.class)
    public void stepWithoutActionIsAProgrammingError()
    {
        service.step("Nothing").run();
    }
}
