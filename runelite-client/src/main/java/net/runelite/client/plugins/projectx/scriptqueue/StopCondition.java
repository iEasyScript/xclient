package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Locale;

/**
 * One reason for a step to move on: a time, a level, some experience, an item count
 * or a time of day. A step stops at the first of its conditions to be met; a script
 * that stops by itself always ends its step too, so that one is never written down.
 *
 * <p>Saved as JSON with the queue: plain fields, and a no-argument constructor so a
 * field added later keeps its default when an older queue is read.
 */
@Data
@NoArgsConstructor
public class StopCondition
{
    public enum Type
    {
        MINUTES("Run for minutes"),
        LEVEL("Until a skill level"),
        XP("Until XP gained in a skill"),
        TOTAL_LEVEL("Until a total level"),
        COMBAT_LEVEL("Until a combat level"),
        ITEM("Until an item count"),
        CLOCK("Until a clock time");

        private final String label;

        Type(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    /** Where an item count is taken. The bank is only known from the last time it was open. */
    public enum Place
    {
        INVENTORY("in the inventory"),
        BANK("in the bank"),
        BOTH("in inventory + bank");

        private final String label;

        Place(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private Type type = Type.MINUTES;
    /** MINUTES: how long the script runs, breaks not counted. */
    private int minutes = 60;
    /** LEVEL and XP: the skill's name, as {@link net.runelite.api.Skill#getName()}. */
    private String skill;
    /** LEVEL, TOTAL_LEVEL and COMBAT_LEVEL: the level to reach. */
    private int level;
    /** XP: experience to gain in the skill from when the step starts. */
    private int xp;
    /** ITEM: the item's exact name, and how many. */
    private String item;
    private int count;
    private Place place = Place.BANK;
    /** CLOCK: a time of day, "HH:mm", the next time it comes round after the step starts. */
    private String time;

    public static StopCondition minutes(int minutes)
    {
        StopCondition c = new StopCondition();
        c.type = Type.MINUTES;
        c.minutes = minutes;
        return c;
    }

    public static StopCondition level(String skill, int level)
    {
        StopCondition c = new StopCondition();
        c.type = Type.LEVEL;
        c.skill = skill;
        c.level = level;
        return c;
    }

    public static StopCondition xp(String skill, int xp)
    {
        StopCondition c = new StopCondition();
        c.type = Type.XP;
        c.skill = skill;
        c.xp = xp;
        return c;
    }

    public static StopCondition totalLevel(int level)
    {
        StopCondition c = new StopCondition();
        c.type = Type.TOTAL_LEVEL;
        c.level = level;
        return c;
    }

    public static StopCondition combatLevel(int level)
    {
        StopCondition c = new StopCondition();
        c.type = Type.COMBAT_LEVEL;
        c.level = level;
        return c;
    }

    public static StopCondition item(String item, int count, Place place)
    {
        StopCondition c = new StopCondition();
        c.type = Type.ITEM;
        c.item = item;
        c.count = count;
        c.place = place;
        return c;
    }

    /** @param time "HH:mm", 24-hour */
    public static StopCondition clock(String time)
    {
        StopCondition c = new StopCondition();
        c.type = Type.CLOCK;
        c.time = time;
        return c;
    }

    /** In plain words, to follow "until": "Magic 75", "2h", "1,000 Nature rune in the bank". */
    public String describe()
    {
        if (type == null)
        {
            return "an unknown condition";
        }
        switch (type)
        {
            case MINUTES:
                return QueueStep.formatMinutes(minutes);
            case LEVEL:
                return skill + " " + level;
            case XP:
                return "+" + number(xp) + " " + skill + " xp";
            case TOTAL_LEVEL:
                return "total level " + number(level);
            case COMBAT_LEVEL:
                return "combat level " + level;
            case ITEM:
                return number(count) + " " + item + " " + (place == null ? Place.BANK : place);
            case CLOCK:
                return time;
            default:
                return "an unknown condition";
        }
    }

    @Override
    public String toString()
    {
        return describe();
    }

    static String number(long n)
    {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
