package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * One step of the queue: a script, and when to move on from it. A step moves on at the
 * first of its conditions to be met. Whatever the conditions, a script that stops by
 * itself (out of supplies, say) also ends its step, so the queue never waits on a script
 * that has already finished; a step with no conditions runs until that happens.
 *
 * <p>Saved as JSON. Queues saved before steps had a list of conditions have a single
 * {@link Until} instead: {@link #conditions()} turns that into the list, and the old
 * fields are kept in step with the list, so a queue saved now still reads sensibly in
 * an older client.
 */
@Data
@NoArgsConstructor
public class QueueStep
{
    /** The single condition queues had before steps could have several. */
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

    /** What to do when the step's script can't start, or stops with an error. */
    public enum OnFailure
    {
        SKIP("Skip to the next step"),
        RETRY("Retry once, then skip"),
        STOP("Stop the whole queue");

        private final String label;

        OnFailure(String label)
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

    // ---- the old single condition: read from older queues, kept up to date for older clients
    private Until until = Until.MINUTES;
    private int minutes = 60;
    /** Skill name, for LEVEL. */
    private String skill;
    private int level;

    /** Null in a queue saved before steps had several conditions: see {@link #conditions()}. */
    private List<StopCondition> conditions;
    private OnFailure onFailure = OnFailure.SKIP;

    /**
     * The step's conditions, any one of which ends it. Built from the old single condition
     * the first time a step from an older queue is asked.
     */
    public List<StopCondition> conditions()
    {
        if (conditions == null)
        {
            List<StopCondition> list = new ArrayList<>();
            Until u = until == null ? Until.STOPS : until;
            if (u == Until.MINUTES)
            {
                list.add(StopCondition.minutes(minutes));
            }
            else if (u == Until.LEVEL && skill != null)
            {
                list.add(StopCondition.level(skill, level));
            }
            conditions = list;
        }
        return conditions;
    }

    /** Sets the conditions, and the old single condition to the nearest it can say. */
    public void setConditions(List<StopCondition> list)
    {
        conditions = list == null ? new ArrayList<>() : new ArrayList<>(list);
        StopCondition time = null;
        StopCondition lvl = null;
        for (StopCondition c : conditions)
        {
            if (c.getType() == StopCondition.Type.MINUTES && time == null)
            {
                time = c;
            }
            else if (c.getType() == StopCondition.Type.LEVEL && lvl == null)
            {
                lvl = c;
            }
        }
        if (time != null)
        {
            until = Until.MINUTES;
            minutes = time.getMinutes();
        }
        else if (lvl != null)
        {
            until = Until.LEVEL;
            skill = lvl.getSkill();
            level = lvl.getLevel();
        }
        else
        {
            until = Until.STOPS;
        }
    }

    public OnFailure onFailure()
    {
        return onFailure == null ? OnFailure.SKIP : onFailure;
    }

    /** "AIO Magic until Magic 75 or 2h", "Woodcutter for 1h", "Fishing until it stops". */
    public String describe()
    {
        List<StopCondition> list = conditions();
        if (list.isEmpty())
        {
            return pluginName + " until it stops";
        }
        if (list.size() == 1 && list.get(0).getType() == StopCondition.Type.MINUTES)
        {
            return pluginName + " for " + formatMinutes(list.get(0).getMinutes());
        }
        return pluginName + " until " + list.stream().map(StopCondition::describe).collect(Collectors.joining(" or "));
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
