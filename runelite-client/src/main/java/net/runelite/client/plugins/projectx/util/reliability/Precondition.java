package net.runelite.client.plugins.projectx.util.reliability;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Something that must hold before a guarded action runs, with a short description used in
 * the log and, if the script ends up stopped, in the message to the player.
 *
 * <p>A precondition that is not met fails the step without running the action, as
 * {@link Outcome#RECOVERABLE} (the recovery handler may fix it: re-bank, walk back) or, if
 * marked {@link #fatal()}, as {@link Outcome#UNRECOVERABLE}. Ready-made checks built on the
 * Rs2 utilities are in {@link Preconditions}.
 */
public final class Precondition
{
    private final String description;
    private final BooleanSupplier check;
    private final boolean fatal;

    private Precondition(String description, BooleanSupplier check, boolean fatal)
    {
        this.description = Objects.requireNonNull(description, "description");
        this.check = Objects.requireNonNull(check, "check");
        this.fatal = fatal;
    }

    public static Precondition of(String description, BooleanSupplier check)
    {
        return new Precondition(description, check, false);
    }

    /** The same check, but failing it stops the script instead of trying to recover. */
    public Precondition fatal()
    {
        return new Precondition(description, check, true);
    }

    public String getDescription()
    {
        return description;
    }

    public boolean isFatal()
    {
        return fatal;
    }

    /**
     * Evaluates the check on the calling (script) thread. An exception from the check
     * counts as not met, except {@link UnrecoverableActionException}, which is passed on.
     */
    boolean test()
    {
        try
        {
            return check.getAsBoolean();
        }
        catch (UnrecoverableActionException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            return false;
        }
    }

    @Override
    public String toString()
    {
        return description;
    }
}
