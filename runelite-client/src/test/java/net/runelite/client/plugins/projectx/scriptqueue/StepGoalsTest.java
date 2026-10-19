package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.api.Skill;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Each kind of stop condition, and "any of" between several. The game and the clock are
 * fakes, so every reading is set by the test.
 */
public class StepGoalsTest
{
    private static final ZoneId ZONE = ZoneId.of("Europe/London");
    private static final long MINUTE = 60_000;

    private QueueFakes.MutableClock clock;
    private QueueFakes.FakeGame game;

    @Before
    public void setUp()
    {
        clock = at(2026, 10, 15, 22, 0);
        game = new QueueFakes.FakeGame();
    }

    private static QueueFakes.MutableClock at(int y, int mo, int d, int h, int mi)
    {
        return new QueueFakes.MutableClock(LocalDateTime.of(y, mo, d, h, mi).atZone(ZONE).toInstant(), ZONE);
    }

    private StepGoals goals(StopCondition... conditions)
    {
        StepGoals g = new StepGoals(List.of(conditions), game, clock);
        g.begin();
        return g;
    }

    @Test
    public void minutesCountOnlyTheTimeTheScriptRan()
    {
        StepGoals g = goals(StopCondition.minutes(60));
        g.addRunning(30 * MINUTE);
        // Time spent on a break is never passed in: the clock moving alone changes nothing.
        clock.advance(120 * MINUTE);
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("30m left", c.progress);

        g.addRunning(30 * MINUTE);
        c = g.check();
        assertTrue(c.met);
        assertEquals("time's up", c.reason);
    }

    @Test
    public void minutesLeftRoundUp()
    {
        StepGoals g = goals(StopCondition.minutes(120));
        g.addRunning(47 * MINUTE + 30_000);
        assertEquals("1h 13m left", g.check().progress);
    }

    @Test
    public void skillLevel()
    {
        StepGoals g = goals(StopCondition.level("Magic", 75));
        game.levels.put(Skill.MAGIC, 72);
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("Magic 72/75", c.progress);

        game.levels.put(Skill.MAGIC, 75);
        c = g.check();
        assertTrue(c.met);
        assertEquals("reached Magic 75", c.reason);
    }

    @Test
    public void unknownLevelIsNotMet()
    {
        game.loggedIn = false;
        StepGoals.Check c = goals(StopCondition.level("Magic", 1)).check();
        assertFalse(c.met);
        assertEquals("Magic ?/1", c.progress);
    }

    @Test
    public void unknownSkillIsNeverMet()
    {
        StepGoals.Check c = goals(StopCondition.level("Cheese", 1)).check();
        assertFalse(c.met);
        assertEquals("unknown skill Cheese", c.progress);
    }

    @Test
    public void xpIsCountedFromWhenTheStepBegan()
    {
        game.xp.put(Skill.MAGIC, 1_000_000);
        StepGoals g = goals(StopCondition.xp("Magic", 50_000));

        game.xp.put(Skill.MAGIC, 1_049_999);
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("Magic xp +49,999/50,000", c.progress);

        game.xp.put(Skill.MAGIC, 1_050_000);
        c = g.check();
        assertTrue(c.met);
        assertEquals("gained 50,000 Magic xp", c.reason);
    }

    @Test
    public void xpBeganLoggedOutCountsFromTheFirstReading()
    {
        game.loggedIn = false;
        StepGoals g = goals(StopCondition.xp("Magic", 100));
        assertEquals("Magic xp +?/100", g.check().progress);

        game.loggedIn = true;
        game.xp.put(Skill.MAGIC, 5_000);
        assertEquals("Magic xp +0/100", g.check().progress);
        game.xp.put(Skill.MAGIC, 5_100);
        assertTrue(g.check().met);
    }

    @Test
    public void xpBaselineSurvivesASecondBegin()
    {
        game.xp.put(Skill.MAGIC, 1_000);
        StepGoals g = goals(StopCondition.xp("Magic", 100));
        game.xp.put(Skill.MAGIC, 1_060);
        // A retry begins again: the experience already gained still counts.
        g.begin();
        game.xp.put(Skill.MAGIC, 1_100);
        assertTrue(g.check().met);
    }

    @Test
    public void totalLevel()
    {
        StepGoals g = goals(StopCondition.totalLevel(1_500));
        game.total = 1_499;
        assertEquals("Total level 1,499/1,500", g.check().progress);
        assertFalse(g.check().met);
        game.total = 1_500;
        assertEquals("reached total level 1,500", g.check().reason);
    }

    @Test
    public void combatLevel()
    {
        StepGoals g = goals(StopCondition.combatLevel(70));
        game.combat = 68;
        assertEquals("Combat 68/70", g.check().progress);
        game.combat = 71;
        assertTrue(g.check().met);
        assertEquals("reached combat level 70", g.check().reason);
    }

    @Test
    public void itemsInTheBank()
    {
        StepGoals g = goals(StopCondition.item("Nature rune", 1_000, StopCondition.Place.BANK));
        game.bank.put("nature rune", 640);
        game.inventory.put("nature rune", 500);
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("Nature rune 640/1,000 in the bank", c.progress);

        game.bank.put("nature rune", 1_000);
        c = g.check();
        assertTrue(c.met);
        assertEquals("1,000 Nature rune in the bank", c.reason);
    }

    @Test
    public void aBankNotSeenYetIsNotMetAndSaysSo()
    {
        game.bankSeen = false;
        StepGoals.Check c = goals(StopCondition.item("Nature rune", 1, StopCondition.Place.BANK)).check();
        assertFalse(c.met);
        assertTrue(c.progress, c.progress.contains("not known until the bank has been opened"));

        c = goals(StopCondition.item("Nature rune", 1, StopCondition.Place.BOTH)).check();
        assertFalse(c.met);
        assertTrue(c.progress, c.progress.contains("not known until the bank has been opened"));
    }

    @Test
    public void itemsInTheInventory()
    {
        game.bankSeen = false;
        StepGoals g = goals(StopCondition.item("Logs", 27, StopCondition.Place.INVENTORY));
        game.inventory.put("logs", 26);
        assertFalse(g.check().met);
        assertEquals("Logs 26/27 in the inventory", g.check().progress);
        game.inventory.put("logs", 27);
        assertTrue(g.check().met);
    }

    @Test
    public void itemsInInventoryAndBankAddUp()
    {
        StepGoals g = goals(StopCondition.item("Coal", 100, StopCondition.Place.BOTH));
        game.inventory.put("coal", 40);
        game.bank.put("coal", 59);
        assertFalse(g.check().met);
        game.bank.put("coal", 60);
        assertTrue(g.check().met);
    }

    @Test
    public void clockTimeLaterToday()
    {
        StepGoals g = goals(StopCondition.clock("23:00"));
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("23:00 in 1h", c.progress);

        clock.advance(59 * MINUTE);
        assertFalse(g.check().met);
        clock.advance(MINUTE);
        c = g.check();
        assertTrue(c.met);
        assertEquals("it's 23:00", c.reason);
    }

    @Test
    public void clockTimeAlreadyPastMeansTomorrow()
    {
        clock = at(2026, 10, 15, 23, 30);
        StepGoals g = goals(StopCondition.clock("23:00"));
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("23:00 in 23h 30m", c.progress);
    }

    @Test
    public void clockTimeAcceptsASingleDigitHour()
    {
        clock = at(2026, 10, 15, 8, 0);
        assertEquals("9:30 in 1h 30m", goals(StopCondition.clock("9:30")).check().progress);
        assertNull(StepGoals.timeOf("25:00"));
        assertNull(StepGoals.timeOf("soon"));
    }

    @Test
    public void anyOfStopsAtTheFirstMet()
    {
        StepGoals g = goals(StopCondition.level("Magic", 75), StopCondition.minutes(120));
        game.levels.put(Skill.MAGIC, 72);
        g.addRunning(48 * MINUTE);
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("Magic 72/75 · 1h 12m left", c.progress);

        // The level comes first.
        game.levels.put(Skill.MAGIC, 75);
        c = g.check();
        assertTrue(c.met);
        assertEquals("reached Magic 75", c.reason);
    }

    @Test
    public void anyOfTimeCanComeFirst()
    {
        StepGoals g = goals(StopCondition.level("Magic", 75), StopCondition.minutes(120));
        game.levels.put(Skill.MAGIC, 73);
        g.addRunning(120 * MINUTE);
        StepGoals.Check c = g.check();
        assertTrue(c.met);
        assertEquals("time's up", c.reason);
    }

    @Test
    public void anyOfAnUnknownReadingDoesNotHideAnotherMet()
    {
        game.bankSeen = false;
        game.combat = 90;
        StepGoals.Check c = goals(StopCondition.item("Nature rune", 5, StopCondition.Place.BANK), StopCondition.combatLevel(80)).check();
        assertTrue(c.met);
        assertEquals("reached combat level 80", c.reason);
    }

    @Test
    public void noConditionsRunsUntilTheScriptStops()
    {
        StepGoals g = goals();
        g.addRunning(5 * MINUTE);
        StepGoals.Check c = g.check();
        assertFalse(c.met);
        assertEquals("running 5m", c.progress);
    }

    @Test
    public void stepsDescribeTheirConditionsInPlainWords()
    {
        assertEquals("AIO Magic until Magic 75 or 2h",
            QueueFakes.step("a", "AIO Magic", StopCondition.level("Magic", 75), StopCondition.minutes(120)).describe());
        assertEquals("AIO Magic for 1h 30m", QueueFakes.step("a", "AIO Magic", StopCondition.minutes(90)).describe());
        assertEquals("Fisher until it stops", QueueFakes.step("a", "Fisher").describe());
        assertEquals("Runecrafter until 1,000 Nature rune in the bank or 23:00",
            QueueFakes.step("a", "Runecrafter", StopCondition.item("Nature rune", 1_000, StopCondition.Place.BANK),
                StopCondition.clock("23:00")).describe());
        assertEquals("Fighter until combat level 70 or total level 1,000 or +50,000 Attack xp",
            QueueFakes.step("a", "Fighter", StopCondition.combatLevel(70), StopCondition.totalLevel(1_000),
                StopCondition.xp("Attack", 50_000)).describe());
    }
}
