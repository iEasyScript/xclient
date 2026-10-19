package net.runelite.client.plugins.projectx.scriptqueue;

import lombok.Value;

/**
 * Posted on the client {@link net.runelite.client.eventbus.EventBus} once when a run of
 * the Script Queue is over, however it ended. Posted from the queue's own thread, not
 * the client thread, after the last script has been stopped.
 */
@Value
public class ScriptQueueFinished
{
    public enum Outcome
    {
        /** Every step (and every round) was gone through. Steps may still have failed and been skipped. */
        COMPLETED,
        /** The player stopped it, or the Script Queue was turned off. */
        STOPPED,
        /** It gave up: a step failed with "stop the whole queue", or nothing in a looping plan could run. */
        ABORTED,
        /** The queue itself hit an unexpected error. */
        ERROR
    }

    /** The plan's name. */
    String plan;
    Outcome outcome;
    /** Why it ended early, in plain words; null when COMPLETED. */
    String reason;
    /** How many times through the plan were begun. */
    int rounds;
    /** Steps that ended well: a condition met (or already met), or the script stopped by itself. */
    int stepsCompleted;
    /** Steps that failed, after any retry. */
    int stepsFailed;
    /** From starting the queue to the end, breaks included. */
    long durationMillis;
    /** One line for an alert: "Plan Default finished: 4 steps done, 1 failed, in 3h 12m." */
    String summary;
}
