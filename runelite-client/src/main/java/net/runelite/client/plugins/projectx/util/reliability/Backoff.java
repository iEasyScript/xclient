package net.runelite.client.plugins.projectx.util.reliability;

/**
 * Exponential wait between attempts of a guarded step: {@code base}, {@code 2 x base},
 * {@code 4 x base} and so on, never more than {@code max}. Deterministic on purpose: the
 * wait exists to let the game catch up (a lagging tick, an interface still opening), not
 * to change how the script plays.
 */
public final class Backoff
{
    /** One game tick. */
    public static final long DEFAULT_BASE_MS = 600;
    public static final long DEFAULT_MAX_MS = 4_800;

    private Backoff()
    {
    }

    /**
     * How long to wait after failed attempt number {@code attempt} (1 for the first)
     * before the next one.
     */
    public static long delayAfterAttempt(int attempt, long baseMs, long maxMs)
    {
        if (attempt < 1 || baseMs <= 0 || maxMs <= 0)
        {
            return 0;
        }
        int shift = Math.min(attempt - 1, 30);
        long delay = baseMs << shift;
        if (delay < 0 || (delay >> shift) != baseMs)
        {
            return maxMs; // overflowed
        }
        return Math.min(delay, maxMs);
    }
}
