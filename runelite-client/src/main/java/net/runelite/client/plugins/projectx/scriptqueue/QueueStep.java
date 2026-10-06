package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One step of the queue: a script, and when to move on from it. Whatever the
 * condition, a script that stops by itself (out of supplies, say) also ends its
 * step, so the queue never waits on a script that has already finished.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueueStep
{
    public enum Until
    {
        MINUTES("Run for minutes"),
        STOPS("Until it stops by itself"),
        LEVEL("Until a skill level");

        private final String label;

        Until(String label)
        {
            this.label = label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    /** The plugin's class name, which is how it is found again after a restart. */
    private String pluginClass;
    /** Its name as shown, for the list. */
    private String pluginName;
    private Until until = Until.MINUTES;
    private int minutes = 60;
    /** Skill name, for LEVEL. */
    private String skill;
    private int level;

    public String describe()
    {
        switch (until)
        {
            case MINUTES:
                return pluginName + " for " + formatMinutes(minutes);
            case LEVEL:
                return pluginName + " until " + skill + " " + level;
            default:
                return pluginName + " until it stops";
        }
    }

    static String formatMinutes(int minutes)
    {
        if (minutes < 60)
        {
            return minutes + "m";
        }
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? h + "h" : h + "h " + m + "m";
    }
}
