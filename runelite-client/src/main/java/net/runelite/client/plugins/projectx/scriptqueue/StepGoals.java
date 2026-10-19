package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.api.Skill;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Watches one step's conditions while it runs and says, in plain words, how far along
 * each is. The step ends at the first one met ("any of"). A reading the game can't give
 * (logged out, a bank not seen yet) never meets a condition, and the progress says so.
 *
 * <p>Kept for the whole step, retries included: experience is counted from when the
 * step began, minutes are minutes of the script running (breaks don't count), and a
 * clock time is the next time it comes round after the step began.
 */
final class StepGoals
{
    /** Where the step's conditions stand. */
    static final class Check
    {
        /** One of the conditions is met. */
        final boolean met;
        /** Which one, in words: "reached Magic 75", "time's up". Null unless met. */
        final String reason;
        /** Every condition's progress: "Magic 72/75 · 1h 12m left". */
        final String progress;

        Check(boolean met, String reason, String progress)
        {
            this.met = met;
            this.reason = reason;
            this.progress = progress;
        }
    }

    private final List<StopCondition> conditions;
    private final QueueGameState game;
    private final Clock clock;
    /** XP conditions: the skill's experience when the step began; -1 until known. */
    private final int[] xpBaseline;
    /** CLOCK conditions: when it comes round, in epoch milliseconds; -1 for a time that can't be read. */
    private final long[] clockAt;
    private boolean begun;
    private long runningMillis;

    StepGoals(List<StopCondition> conditions, QueueGameState game, Clock clock)
    {
        this.conditions = new ArrayList<>(conditions);
        this.game = game;
        this.clock = clock;
        this.xpBaseline = new int[this.conditions.size()];
        this.clockAt = new long[this.conditions.size()];
        Arrays.fill(xpBaseline, QueueGameState.UNKNOWN);
        Arrays.fill(clockAt, -1L);
    }

    /** The step is starting: take the experience baselines and work out the clock times. Only the first call counts. */
    void begin()
    {
        if (begun)
        {
            return;
        }
        begun = true;
        Instant now = clock.instant();
        for (int i = 0; i < conditions.size(); i++)
        {
            StopCondition c = conditions.get(i);
            if (c.getType() == StopCondition.Type.CLOCK)
            {
                LocalTime at = timeOf(c.getTime());
                clockAt[i] = at == null ? -1L : nextOccurrence(at, now, clock).toInstant().toEpochMilli();
            }
            else if (c.getType() == StopCondition.Type.XP)
            {
                Skill skill = ScriptQueueRunner.skillOf(c.getSkill());
                xpBaseline[i] = skill == null ? QueueGameState.UNKNOWN : game.xp(skill);
            }
        }
    }

    /** The script ran this much longer (breaks are not passed in). */
    void addRunning(long millis)
    {
        runningMillis += Math.max(0, millis);
    }

    long runningMillis()
    {
        return runningMillis;
    }

    Check check()
    {
        begin();
        if (conditions.isEmpty())
        {
            return new Check(false, null, "running " + QueueStep.formatMinutes((int) (runningMillis / 60_000)));
        }
        String reason = null;
        StringBuilder progress = new StringBuilder();
        for (int i = 0; i < conditions.size(); i++)
        {
            String[] r = evaluate(i);
            if (r[0] != null && reason == null)
            {
                reason = r[0];
            }
            if (progress.length() > 0)
            {
                progress.append(" · ");
            }
            progress.append(r[1]);
        }
        return new Check(reason != null, reason, progress.toString());
    }

    /** @return {reason it's met, or null; its progress} */
    private String[] evaluate(int i)
    {
        StopCondition c = conditions.get(i);
        if (c.getType() == null)
        {
            return new String[]{null, "an unknown condition"};
        }
        switch (c.getType())
        {
            case MINUTES:
            {
                long left = c.getMinutes() * 60_000L - runningMillis;
                if (left <= 0)
                {
                    return new String[]{"time's up", "time's up"};
                }
                return new String[]{null, QueueStep.formatMinutes((int) ceilMinutes(left)) + " left"};
            }
            case LEVEL:
            {
                Skill skill = ScriptQueueRunner.skillOf(c.getSkill());
                if (skill == null)
                {
                    return new String[]{null, "unknown skill " + c.getSkill()};
                }
                int level = game.level(skill);
                String progress = c.getSkill() + " " + known(level) + "/" + c.getLevel();
                return new String[]{level != QueueGameState.UNKNOWN && level >= c.getLevel()
                    ? "reached " + c.getSkill() + " " + c.getLevel() : null, progress};
            }
            case XP:
            {
                Skill skill = ScriptQueueRunner.skillOf(c.getSkill());
                if (skill == null)
                {
                    return new String[]{null, "unknown skill " + c.getSkill()};
                }
                int xp = game.xp(skill);
                if (xpBaseline[i] == QueueGameState.UNKNOWN)
                {
                    // Began logged out: count from the first time it can be read.
                    xpBaseline[i] = xp;
                }
                if (xp == QueueGameState.UNKNOWN || xpBaseline[i] == QueueGameState.UNKNOWN)
                {
                    return new String[]{null, c.getSkill() + " xp +?/" + StopCondition.number(c.getXp())};
                }
                long gained = (long) xp - xpBaseline[i];
                String progress = c.getSkill() + " xp +" + StopCondition.number(gained) + "/" + StopCondition.number(c.getXp());
                return new String[]{gained >= c.getXp()
                    ? "gained " + StopCondition.number(c.getXp()) + " " + c.getSkill() + " xp" : null, progress};
            }
            case TOTAL_LEVEL:
            {
                int total = game.totalLevel();
                String progress = "Total level " + (total == QueueGameState.UNKNOWN ? "?" : StopCondition.number(total))
                    + "/" + StopCondition.number(c.getLevel());
                return new String[]{total != QueueGameState.UNKNOWN && total >= c.getLevel()
                    ? "reached total level " + StopCondition.number(c.getLevel()) : null, progress};
            }
            case COMBAT_LEVEL:
            {
                int combat = game.combatLevel();
                String progress = "Combat " + known(combat) + "/" + c.getLevel();
                return new String[]{combat != QueueGameState.UNKNOWN && combat >= c.getLevel()
                    ? "reached combat level " + c.getLevel() : null, progress};
            }
            case ITEM:
                return evaluateItem(c);
            case CLOCK:
            {
                if (clockAt[i] < 0)
                {
                    return new String[]{null, "can't read the time " + c.getTime()};
                }
                long left = clockAt[i] - clock.millis();
                if (left <= 0)
                {
                    return new String[]{"it's " + c.getTime(), "it's " + c.getTime()};
                }
                return new String[]{null, c.getTime() + " in " + QueueStep.formatMinutes((int) ceilMinutes(left))};
            }
            default:
                return new String[]{null, "an unknown condition"};
        }
    }

    private String[] evaluateItem(StopCondition c)
    {
        String item = c.getItem() == null ? "" : c.getItem().trim();
        if (item.isEmpty())
        {
            return new String[]{null, "no item named"};
        }
        StopCondition.Place place = c.getPlace() == null ? StopCondition.Place.BANK : c.getPlace();
        int inventory = place == StopCondition.Place.BANK ? 0 : game.inventoryCount(item);
        int bank = place == StopCondition.Place.INVENTORY ? 0 : game.bankCount(item);
        String target = StopCondition.number(c.getCount()) + " " + place;
        if (bank == QueueGameState.UNKNOWN)
        {
            return new String[]{null, item + " " + place + ": not known until the bank has been opened"};
        }
        if (inventory == QueueGameState.UNKNOWN)
        {
            return new String[]{null, item + " ?/" + target + " (logged out)"};
        }
        long have = (long) inventory + bank;
        String progress = item + " " + StopCondition.number(have) + "/" + target;
        return new String[]{have >= c.getCount() ? StopCondition.number(c.getCount()) + " " + item + " " + place : null, progress};
    }

    private static String known(int value)
    {
        return value == QueueGameState.UNKNOWN ? "?" : Integer.toString(value);
    }

    private static long ceilMinutes(long millis)
    {
        return Math.max(1, (millis + 59_999) / 60_000);
    }

    /** "23:00" (or "9:30"), or null if it can't be read. */
    static LocalTime timeOf(String text)
    {
        if (text == null)
        {
            return null;
        }
        String t = text.trim();
        if (t.length() == 4 && t.charAt(1) == ':')
        {
            t = "0" + t;
        }
        try
        {
            return LocalTime.parse(t.toUpperCase(Locale.ROOT));
        }
        catch (DateTimeParseException e)
        {
            return null;
        }
    }

    /** The next time the clock shows {@code at}, strictly after {@code now}. */
    static ZonedDateTime nextOccurrence(LocalTime at, Instant now, Clock clock)
    {
        ZonedDateTime current = now.atZone(clock.getZone());
        ZonedDateTime candidate = current.with(at.truncatedTo(ChronoUnit.MINUTES));
        return candidate.isAfter(current) ? candidate : candidate.plusDays(1);
    }
}
