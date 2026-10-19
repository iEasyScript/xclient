package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.Value;

/**
 * Posted on the client {@link net.runelite.client.eventbus.EventBus} when a step of the
 * Script Queue is over, once per attempt. A step that never started its script (already
 * met, not installed, requirements not met) still ends, with no {@link ScriptQueueStepStarted}
 * before it. Posted from the queue's own thread, not the client thread.
 */
@Value
public class ScriptQueueStepEnded
{
    public enum Reason
    {
        /** One of the step's conditions was met, and the queue stopped the script. */
        CONDITION_MET,
        /** A condition was already met before the script started, so it wasn't started. */
        ALREADY_MET,
        /** The script stopped by itself (out of supplies, say), with no error seen. */
        STOPPED_BY_ITSELF,
        /** It couldn't start, or stopped with an error: see {@code detail} and {@code retrying}. */
        FAILED,
        /** The player stopped the queue during the step. */
        QUEUE_STOPPED
    }

    /** The plan's name. */
    String plan;
    /** Which step, from 0. */
    int stepIndex;
    /** How many steps the plan has. */
    int stepCount;
    /** Which time through the plan, from 1. */
    int round;
    /** 1, or 2 when the step was being retried. */
    int attempt;
    /** The script's plugin class name. */
    String pluginClass;
    /** The script's name as shown. */
    String pluginName;
    Reason reason;
    /** Why, in plain words: "reached Magic 75", "didn't start (not owned, or it stopped straight away)". */
    String detail;
    /** FAILED only: the step is about to be tried once more. */
    boolean retrying;
    /** How long the script ran in this attempt, breaks not counted; 0 if it never started. */
    long runMillis;
}
