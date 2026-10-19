package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The runner against a fake client: how steps end, the failure settings, rounds, and the
 * events it posts. Time only moves when the runner sleeps, so a two-hour step takes no time.
 */
public class ScriptQueueRunnerTest
{
    private static final long MINUTE = 60_000;

    private QueueFakes.MutableClock clock;
    private QueueFakes.FakeHost host;
    private QueueFakes.FakeGame game;
    private QueueFakes.Listener listener;
    private ScriptQueuePreStartCheck check;

    @Before
    public void setUp()
    {
        clock = new QueueFakes.MutableClock(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"));
        host = new QueueFakes.FakeHost(clock);
        game = new QueueFakes.FakeGame();
        listener = new QueueFakes.Listener();
        check = ScriptQueuePreStartCheck.ALWAYS_OK;
    }

    private ScriptQueueRunner runner(QueuePlan plan, boolean logout)
    {
        return new ScriptQueueRunner(plan, logout, false, 0, 0, host, game, check, listener);
    }

    private ScriptQueueFinished run(QueuePlan plan)
    {
        runner(plan, false).run();
        List<ScriptQueueFinished> finished = host.events(ScriptQueueFinished.class);
        assertEquals("one finished event", 1, finished.size());
        return finished.get(0);
    }

    private static QueuePlan plan(QueueStep... steps)
    {
        QueuePlan plan = new QueuePlan("Main");
        plan.steps().addAll(List.of(steps));
        return plan;
    }

    private static QueueStep step(String name, QueueStep.OnFailure onFailure, StopCondition... conditions)
    {
        QueueStep s = QueueFakes.step("x." + name, name, conditions);
        s.setOnFailure(onFailure);
        return s;
    }

    private List<String> ends()
    {
        return host.events(ScriptQueueStepEnded.class).stream()
            .map(e -> e.getPluginName() + " " + e.getAttempt() + " " + e.getReason() + (e.isRetrying() ? " retrying" : ""))
            .collect(Collectors.toList());
    }

    // ---- how a step ends

    @Test(timeout = 10_000)
    public void aStepEndsWhenItsTimeIsUp()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        ScriptQueueFinished done = run(plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(30))));

        assertEquals(ScriptQueueFinished.Outcome.COMPLETED, done.getOutcome());
        assertEquals(1, magic.starts);
        assertEquals("stopped by the queue, not the player", List.of(false), magic.stops);
        ScriptQueueStepEnded end = host.events(ScriptQueueStepEnded.class).get(0);
        assertEquals(ScriptQueueStepEnded.Reason.CONDITION_MET, end.getReason());
        assertEquals("time's up", end.getDetail());
        assertEquals(30 * MINUTE, end.getRunMillis());
        assertEquals(1, done.getStepsCompleted());
        assertEquals(0, done.getStepsFailed());
    }

    @Test(timeout = 10_000)
    public void aStepEndsAtTheFirstOfItsConditions()
    {
        host.script("x.Magic");
        game.levels.put(Skill.MAGIC, 72);
        long start = clock.millis();
        host.onTick = () ->
        {
            if (clock.millis() - start >= 40 * MINUTE)
            {
                game.levels.put(Skill.MAGIC, 75);
            }
        };
        run(plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.level("Magic", 75), StopCondition.minutes(120))));

        ScriptQueueStepEnded end = host.events(ScriptQueueStepEnded.class).get(0);
        assertEquals("reached Magic 75", end.getDetail());
        assertTrue(listener.steps.contains("0 RUNNING Magic 72/75 · 1h 30m left"));
    }

    @Test(timeout = 10_000)
    public void aMetConditionSkipsTheStepWithoutStartingIt()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        game.levels.put(Skill.MAGIC, 80);
        ScriptQueueFinished done = run(plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.level("Magic", 75))));

        assertEquals(0, magic.starts);
        assertTrue(host.events(ScriptQueueStepStarted.class).isEmpty());
        assertEquals(List.of("Magic 1 ALREADY_MET"), ends());
        assertEquals(1, done.getStepsCompleted());
    }

    @Test(timeout = 10_000)
    public void aScriptStoppingByItselfEndsItsStepAndIsNotAFailure()
    {
        host.script("x.Fish").stopsAfterMs = 20 * MINUTE;
        host.script("x.Magic");
        ScriptQueueFinished done = run(plan(
            step("Fish", QueueStep.OnFailure.STOP, StopCondition.minutes(120)),
            step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(1))));

        assertEquals(List.of("Fish 1 STOPPED_BY_ITSELF", "Magic 1 CONDITION_MET"), ends());
        assertEquals(ScriptQueueFinished.Outcome.COMPLETED, done.getOutcome());
        assertEquals(2, done.getStepsCompleted());
    }

    // ---- failures and their settings

    @Test(timeout = 10_000)
    public void notInstalledSkipsToTheNextStepByDefault()
    {
        host.script("x.Magic");
        QueueStep missing = QueueFakes.step("x.Gone", "Gone", StopCondition.minutes(10));
        ScriptQueueFinished done = run(plan(missing, step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(1))));

        assertEquals(List.of("Gone 1 FAILED", "Magic 1 CONDITION_MET"), ends());
        assertEquals("isn't installed", host.events(ScriptQueueStepEnded.class).get(0).getDetail());
        assertEquals(ScriptQueueFinished.Outcome.COMPLETED, done.getOutcome());
        assertEquals(1, done.getStepsCompleted());
        assertEquals(1, done.getStepsFailed());
        assertTrue(listener.statuses.contains("Step 1/2: Gone isn't installed. Skipping."));
        assertTrue(listener.steps.contains("0 FAILED isn't installed"));
    }

    @Test(timeout = 10_000)
    public void aScriptThatDoesNotStartFailsItsStep()
    {
        host.script("x.Magic").failingStarts = 1;
        ScriptQueueFinished done = run(plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(10))));

        ScriptQueueStepEnded end = host.events(ScriptQueueStepEnded.class).get(0);
        assertEquals(ScriptQueueStepEnded.Reason.FAILED, end.getReason());
        assertTrue(end.getDetail(), end.getDetail().startsWith("didn't start"));
        assertEquals(1, done.getStepsFailed());
    }

    @Test(timeout = 10_000)
    public void retryOnceThenItWorks()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        magic.failingStarts = 1;
        ScriptQueueFinished done = run(plan(step("Magic", QueueStep.OnFailure.RETRY, StopCondition.minutes(10))));

        assertEquals(2, magic.starts);
        assertEquals(List.of("Magic 1 FAILED retrying", "Magic 2 CONDITION_MET"), ends());
        List<ScriptQueueStepStarted> starts = host.events(ScriptQueueStepStarted.class);
        assertEquals(2, starts.size());
        assertEquals(2, starts.get(1).getAttempt());
        assertEquals(1, done.getStepsCompleted());
        assertEquals("a retry that works is not a failure", 0, done.getStepsFailed());
    }

    @Test(timeout = 10_000)
    public void retryOnlyOnceThenSkip()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        magic.failingStarts = 5;
        host.script("x.Fish");
        ScriptQueueFinished done = run(plan(
            step("Magic", QueueStep.OnFailure.RETRY, StopCondition.minutes(10)),
            step("Fish", QueueStep.OnFailure.SKIP, StopCondition.minutes(1))));

        assertEquals(2, magic.starts);
        assertEquals(List.of("Magic 1 FAILED retrying", "Magic 2 FAILED", "Fish 1 CONDITION_MET"), ends());
        assertEquals(1, done.getStepsFailed());
        assertEquals(ScriptQueueFinished.Outcome.COMPLETED, done.getOutcome());
    }

    @Test(timeout = 10_000)
    public void stopTheQueueOnFailure()
    {
        QueueFakes.FakeScript fish = host.script("x.Fish");
        QueueStep missing = QueueFakes.step("x.Gone", "Gone", StopCondition.minutes(10));
        missing.setOnFailure(QueueStep.OnFailure.STOP);
        QueuePlan plan = plan(missing, step("Fish", QueueStep.OnFailure.SKIP, StopCondition.minutes(1)));
        ScriptQueueRunner r = runner(plan, true);
        r.run();

        ScriptQueueFinished done = host.events(ScriptQueueFinished.class).get(0);
        assertEquals(ScriptQueueFinished.Outcome.ABORTED, done.getOutcome());
        assertEquals("step 1 failed: Gone isn't installed", done.getReason());
        assertEquals(0, fish.starts);
        assertEquals(1, done.getStepsFailed());
        assertTrue(done.getSummary(), done.getSummary().startsWith("Queue stopped: step 1 failed: Gone isn't installed."));
        assertTrue("an abandoned queue still logs out when asked to", host.loggedOut);
    }

    @Test(timeout = 10_000)
    public void anErrorBeforeAScriptStopsByItselfIsAFailure()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        magic.stopsAfterMs = 30 * MINUTE;
        magic.error = "NullPointerException in the loop";
        ScriptQueueFinished done = run(plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(120))));

        ScriptQueueStepEnded end = host.events(ScriptQueueStepEnded.class).get(0);
        assertEquals(ScriptQueueStepEnded.Reason.FAILED, end.getReason());
        assertEquals("stopped with an error: NullPointerException in the loop", end.getDetail());
        assertEquals(1, done.getStepsFailed());
    }

    @Test(timeout = 10_000)
    public void aFailedPreStartCheckFailsTheStepWithoutStartingIt()
    {
        QueueFakes.FakeScript agility = host.script("x.Agility");
        host.script("x.Fish");
        check = s -> s.getPluginName().equals("Agility") ? "needs 50 Agility" : null;
        ScriptQueueFinished done = run(plan(
            step("Agility", QueueStep.OnFailure.SKIP, StopCondition.minutes(10)),
            step("Fish", QueueStep.OnFailure.SKIP, StopCondition.minutes(1))));

        assertEquals(0, agility.starts);
        assertEquals("can't start: needs 50 Agility", host.events(ScriptQueueStepEnded.class).get(0).getDetail());
        assertTrue(listener.statuses.contains("Step 1/2: Agility can't start: needs 50 Agility. Skipping."));
        assertEquals(1, done.getStepsCompleted());
        assertEquals(1, done.getStepsFailed());
    }

    @Test(timeout = 10_000)
    public void aFailedPreStartCheckFollowsTheStepsFailureSetting()
    {
        host.script("x.Agility");
        check = s -> "needs 50 Agility";
        ScriptQueueFinished done = run(plan(step("Agility", QueueStep.OnFailure.STOP, StopCondition.minutes(10))));
        assertEquals(ScriptQueueFinished.Outcome.ABORTED, done.getOutcome());
    }

    @Test(timeout = 10_000)
    public void aPreStartCheckThatThrowsFailsTheStep()
    {
        host.script("x.Agility");
        check = s ->
        {
            throw new IllegalStateException("boom");
        };
        run(plan(step("Agility", QueueStep.OnFailure.SKIP, StopCondition.minutes(10))));
        String detail = host.events(ScriptQueueStepEnded.class).get(0).getDetail();
        assertTrue(detail, detail.contains("IllegalStateException"));
    }

    // ---- rounds, stopping, events

    @Test(timeout = 10_000)
    public void aRepeatCountRunsThePlanThatManyTimes()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        QueuePlan plan = plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(5)));
        plan.setLoop(true);
        plan.setRounds(3);
        ScriptQueueFinished done = run(plan);

        assertEquals(3, magic.starts);
        assertEquals(3, done.getRounds());
        assertEquals(3, done.getStepsCompleted());
        assertEquals(List.of(1, 2, 3), host.events(ScriptQueueStepStarted.class).stream()
            .map(ScriptQueueStepStarted::getRound).collect(Collectors.toList()));
    }

    @Test(timeout = 10_000)
    public void roundsWithoutLoopRunOnce()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        QueuePlan plan = plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.minutes(5)));
        plan.setRounds(3);
        run(plan);
        assertEquals(1, magic.starts);
    }

    @Test(timeout = 10_000)
    public void anEndlessLoopWithNothingToRunGivesUp()
    {
        QueuePlan plan = plan(QueueFakes.step("x.Gone", "Gone", StopCondition.minutes(10)));
        plan.setLoop(true);
        ScriptQueueFinished done = run(plan);
        assertEquals(ScriptQueueFinished.Outcome.ABORTED, done.getOutcome());
        assertEquals(1, done.getRounds());
    }

    @Test(timeout = 10_000)
    public void stoppingTheQueueStopsTheScriptAsThePlayer()
    {
        QueueFakes.FakeScript magic = host.script("x.Magic");
        QueuePlan plan = plan(step("Magic", QueueStep.OnFailure.SKIP));
        plan.setLoop(true);
        ScriptQueueRunner r = runner(plan, true);
        long start = clock.millis();
        host.onTick = () ->
        {
            if (clock.millis() - start >= 10 * MINUTE)
            {
                r.requestStop();
            }
        };
        r.run();

        assertEquals(List.of(true), magic.stops);
        assertEquals(List.of("Magic 1 QUEUE_STOPPED"), ends());
        ScriptQueueFinished done = host.events(ScriptQueueFinished.class).get(0);
        assertEquals(ScriptQueueFinished.Outcome.STOPPED, done.getOutcome());
        assertFalse("the player is there: no logout", host.loggedOut);
    }

    @Test(timeout = 10_000)
    public void eventsCarryTheStepAndPlan()
    {
        host.script("x.Magic");
        game.levels.put(Skill.MAGIC, 75);
        host.script("x.Fish");
        QueueStep fish = step("Fish", QueueStep.OnFailure.SKIP, StopCondition.minutes(90));
        QueuePlan plan = plan(step("Magic", QueueStep.OnFailure.SKIP, StopCondition.level("Magic", 75)), fish);
        ScriptQueueFinished done = run(plan);

        ScriptQueueStepStarted started = host.events(ScriptQueueStepStarted.class).get(0);
        assertEquals("Main", started.getPlan());
        assertEquals(1, started.getStepIndex());
        assertEquals(2, started.getStepCount());
        assertEquals(1, started.getRound());
        assertEquals(1, started.getAttempt());
        assertEquals("x.Fish", started.getPluginClass());
        assertEquals("Fish", started.getPluginName());
        assertEquals("Fish for 1h 30m", started.getDescription());

        assertEquals("Main", done.getPlan());
        assertEquals(ScriptQueueFinished.Outcome.COMPLETED, done.getOutcome());
        assertEquals(null, done.getReason());
        assertEquals(2, done.getStepsCompleted());
        assertTrue(done.getDurationMillis() >= 90 * MINUTE);
        assertEquals("Queue finished. 2 steps done, in 1h 30m.", done.getSummary());
        // The finish comes last.
        assertTrue(host.events.get(host.events.size() - 1) instanceof ScriptQueueFinished);
    }
}
