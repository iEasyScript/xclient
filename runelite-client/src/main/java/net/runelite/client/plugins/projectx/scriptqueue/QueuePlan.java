package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * A named queue: its steps, and whether to go through them again. Saved as JSON, with a
 * no-argument constructor so a field added later keeps its default in an older save.
 */
@Data
@NoArgsConstructor
public class QueuePlan
{
    /** The name a queue saved before there were named plans is given. */
    public static final String DEFAULT_NAME = "Default";

    private String name = DEFAULT_NAME;
    private List<QueueStep> steps = new ArrayList<>();
    /** Go through the steps again when the last one is done. */
    private boolean loop;
    /** With {@link #loop}: how many times through in all; 0 is no limit. */
    private int rounds;

    QueuePlan(String name)
    {
        this.name = name;
    }

    public List<QueueStep> steps()
    {
        if (steps == null)
        {
            steps = new ArrayList<>();
        }
        return steps;
    }

    /** How many times through the steps: 1 without loop, 0 for no limit. */
    int totalRounds()
    {
        return !loop ? 1 : Math.max(0, rounds);
    }
}
