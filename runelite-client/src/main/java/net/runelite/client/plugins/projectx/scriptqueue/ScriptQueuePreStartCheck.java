package net.runelite.client.plugins.projectx.scriptqueue;

/**
 * A check the Script Queue makes before it starts each step's script (and again before
 * a retry). Install one with {@link ScriptQueuePlugin#setPreStartCheck}. Until one is,
 * every step may start.
 *
 * <p>A step that fails the check is treated as a step whose script failed to start:
 * its "if it fails" setting decides whether to skip it, retry it once or stop the queue,
 * and the reason is shown in the queue's status.
 *
 * <p>Called on the queue's own thread, never the client thread, with the player logged in
 * unless they have logged out by hand. It may read game state through the usual helpers.
 */
@FunctionalInterface
public interface ScriptQueuePreStartCheck
{
    /** Lets every step start. */
    ScriptQueuePreStartCheck ALWAYS_OK = step -> null;

    /**
     * @param step the step about to start: {@link QueueStep#getPluginClass()} names the script
     * @return null if it may start; otherwise why not, in plain words ("needs 50 Agility")
     */
    String check(QueueStep step);
}
