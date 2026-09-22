package net.runelite.client.plugins.projectx;

/**
 * Contract for ProjectX scripts executed by the scheduler.
 * Implementations should return {@code true} from {@link #run()} when the loop should continue.
 */
/**
 * Contract for ProjectX scripts executed by scheduler loops.
 * Return {@code true} to continue the loop iteration; {@code false} pauses/aborts the caller's cycle.
 */
public interface IScript {
    boolean run();
}
