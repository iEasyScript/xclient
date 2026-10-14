package net.runelite.client.plugins.projectx.util.reliability;

/**
 * What the script does to get back on track after a step has failed (all its retries
 * used) or nothing has progressed for too long: walk back to the last checkpoint, re-bank,
 * close a stray interface. Runs on the script thread, so it may use Rs2 utilities and
 * guarded steps. Guarded steps inside it never start a second, nested recovery.
 *
 * <p>Return true when the script is back in a known state. Throw
 * {@link UnrecoverableActionException} when it cannot be, to stop the script now.
 */
@FunctionalInterface
public interface RecoveryHandler
{
    boolean recover(RecoveryContext context) throws Exception;
}
