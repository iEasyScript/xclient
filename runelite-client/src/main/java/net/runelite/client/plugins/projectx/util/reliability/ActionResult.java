package net.runelite.client.plugins.projectx.util.reliability;

/** What happened to one guarded step. */
public final class ActionResult
{
    private final String step;
    private final Outcome outcome;
    private final int attempts;
    private final String reason;
    private final Throwable error;
    private final boolean scriptStopping;

    ActionResult(String step, Outcome outcome, int attempts, String reason, Throwable error, boolean scriptStopping)
    {
        this.step = step;
        this.outcome = outcome;
        this.attempts = attempts;
        this.reason = reason;
        this.error = error;
        this.scriptStopping = scriptStopping;
    }

    ActionResult stopping()
    {
        return scriptStopping ? this : new ActionResult(step, outcome, attempts, reason, error, true);
    }

    public String getStep()
    {
        return step;
    }

    public Outcome getOutcome()
    {
        return outcome;
    }

    public boolean isSuccess()
    {
        return outcome == Outcome.SUCCESS;
    }

    /** How many times the action ran: 0 when a precondition stopped it first. */
    public int getAttempts()
    {
        return attempts;
    }

    /** Why it failed, or null on success. Safe to log; never contains credentials. */
    public String getReason()
    {
        return reason;
    }

    /** The exception the action threw on its last attempt, if any. */
    public Throwable getError()
    {
        return error;
    }

    /**
     * True when the script has been stopped (by this step or earlier) or is being switched
     * off. The script should return from its loop straight away.
     */
    public boolean isScriptStopping()
    {
        return scriptStopping;
    }

    @Override
    public String toString()
    {
        return step + ": " + outcome + " after " + attempts + " attempt(s)" + (reason == null ? "" : " (" + reason + ")");
    }
}
