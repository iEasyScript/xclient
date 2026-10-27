package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.magic.Rs2Tome;

/**
 * Spell tomes (fire, water, earth).
 *
 * <p>The constants of {@link Rs2Tome} under a shorter name: {@code Tomes.NONE} is the very
 * same value as {@code Rs2Tome.NONE}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2Tome}:
 *
 * <pre>{@code
 * Rs2Tome choice = Tomes.NONE;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class Tomes
{
    private Tomes()
    {
    }

    public static final Rs2Tome NONE = Rs2Tome.NONE;
    public static final Rs2Tome TOME_OF_FIRE = Rs2Tome.TOME_OF_FIRE;
    public static final Rs2Tome TOME_OF_WATER = Rs2Tome.TOME_OF_WATER;
    public static final Rs2Tome TOME_OF_EARTH = Rs2Tome.TOME_OF_EARTH;

    /** Every value, same as {@code Rs2Tome.values()}. */
    public static Rs2Tome[] values()
    {
        return Rs2Tome.values();
    }

    /** Same as {@code Rs2Tome.valueOf(name)}. */
    public static Rs2Tome valueOf(String name)
    {
        return Rs2Tome.valueOf(name);
    }
}
