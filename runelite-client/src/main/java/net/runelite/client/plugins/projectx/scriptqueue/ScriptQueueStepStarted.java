package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.Value;

/**
 * Posted on the client {@link net.runelite.client.eventbus.EventBus} when the Script Queue
 * has started a step's script: once per attempt, so twice for a step that is retried.
 * Posted from the queue's own thread, not the client thread.
 */
@Value
public class ScriptQueueStepStarted
{
    /** The plan's name. */
    String plan;
    /** Which step, from 0. */
    int stepIndex;
    /** How many steps the plan has. */
    int stepCount;
    /** Which time through the plan, from 1. */
    int round;
    /** 1, or 2 when the step is being retried. */
    int attempt;
    /** The script's plugin class name. */
    String pluginClass;
    /** The script's name as shown. */
    String pluginName;
    /** The step in plain words: "AIO Magic until Magic 75 or 2h". */
    String description;
}
