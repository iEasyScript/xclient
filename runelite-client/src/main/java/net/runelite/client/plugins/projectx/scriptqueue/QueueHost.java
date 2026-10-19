package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.client.plugins.Plugin;

import java.time.Clock;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Everything the queue does to the client: find, start and stop scripts, BreakHandler
 * breaks, logging out, posting events, and time itself. The real one is
 * {@link LiveQueueHost}; tests give the runner a fake whose clock moves when it sleeps.
 */
interface QueueHost
{
    /** The installed script a step names, or null if it isn't installed. */
    Plugin find(QueueStep step);

    boolean isActive(Plugin plugin);

    void start(Plugin plugin);

    /** Stops it, marked as stopped by the player ({@code manual}) or by the queue. */
    void stop(Plugin plugin, boolean manual);

    /** A BreakHandler break is on. */
    boolean onBreak();

    /** The break in words: "On a break, 4m 10s left". */
    String breakStatus();

    /**
     * Asks the BreakHandler for a break and waits until it is over.
     *
     * @return false if no break happened, so the caller can take a short pause instead
     */
    boolean takeBreak(BooleanSupplier stopRequested, Consumer<String> status) throws InterruptedException;

    void logout();

    /** Posts an event on the client's event bus. */
    void post(Object event);

    /**
     * An error the script's own code logged at or after {@code sinceMillis}, in a few words,
     * or null if there was none.
     */
    String errorSince(Plugin plugin, long sinceMillis);

    void sleep(long millis) throws InterruptedException;

    Clock clock();
}
