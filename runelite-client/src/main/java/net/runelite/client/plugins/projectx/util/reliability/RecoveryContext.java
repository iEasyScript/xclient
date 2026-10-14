package net.runelite.client.plugins.projectx.util.reliability;

/** Why a recovery handler was called, and what the service knows at that point. */
public final class RecoveryContext
{
    public enum Trigger
    {
        /** A guarded step failed after all its retries, or a precondition was not met. */
        STEP_FAILED,
        /** Nothing progressed for the player's no-progress timeout. */
        NO_PROGRESS
    }

    private final Trigger trigger;
    private final String step;
    private final String reason;
    private final int consecutiveFailures;
    private final int stepFailures;
    private final Checkpoint checkpoint;

    RecoveryContext(Trigger trigger, String step, String reason, int consecutiveFailures, int stepFailures,
                    Checkpoint checkpoint)
    {
        this.trigger = trigger;
        this.step = step;
        this.reason = reason;
        this.consecutiveFailures = consecutiveFailures;
        this.stepFailures = stepFailures;
        this.checkpoint = checkpoint;
    }

    public Trigger getTrigger()
    {
        return trigger;
    }

    /** The step that failed, or null for {@link Trigger#NO_PROGRESS}. */
    public String getStep()
    {
        return step;
    }

    public String getReason()
    {
        return reason;
    }

    /** Failed steps in a row, across all steps. */
    public int getConsecutiveFailures()
    {
        return consecutiveFailures;
    }

    /** Times this step has failed since it last succeeded. */
    public int getStepFailures()
    {
        return stepFailures;
    }

    /** The last known-good step, or null when the script has not set one. */
    public Checkpoint getCheckpoint()
    {
        return checkpoint;
    }
}
