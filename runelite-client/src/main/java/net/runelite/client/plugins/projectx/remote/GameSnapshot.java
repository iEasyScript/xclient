package net.runelite.client.plugins.projectx.remote;

import lombok.Value;

/**
 * What the game looked like a moment ago, read on the client thread every few
 * ticks so the heartbeat thread never has to wait for it. No coordinates.
 */
@Value
public class GameSnapshot
{
    public static final GameSnapshot LOGGED_OUT = new GameSnapshot(false, -1, false, null, -1, -1, -1);

    boolean loggedIn;
    /** -1 when not known. */
    int world;
    boolean members;
    /** The local player's display name, or null. */
    String name;
    int totalLevel;
    /** Percent of base level, 0-100; -1 when not known. */
    int hpPercent;
    int prayerPercent;

    /** Current over base, as a whole percent capped at 100 (a boost reads as full). */
    public static int percent(int current, int base)
    {
        if (base <= 0 || current < 0)
        {
            return -1;
        }
        return Math.min(100, (int) Math.round(current * 100.0 / base));
    }
}
