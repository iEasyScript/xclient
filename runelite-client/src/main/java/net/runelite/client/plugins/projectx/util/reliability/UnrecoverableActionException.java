package net.runelite.client.plugins.projectx.util.reliability;

/**
 * Thrown from a guarded action, a precondition or a recovery handler when the script can
 * no longer carry on (for example: no pickaxe in the inventory or the bank). The step is
 * classified {@link Outcome#UNRECOVERABLE} and the script is stopped safely, with this
 * exception's message as the reason shown to the player.
 */
public class UnrecoverableActionException extends RuntimeException
{
    public UnrecoverableActionException(String reason)
    {
        super(reason);
    }

    public UnrecoverableActionException(String reason, Throwable cause)
    {
        super(reason, cause);
    }
}
