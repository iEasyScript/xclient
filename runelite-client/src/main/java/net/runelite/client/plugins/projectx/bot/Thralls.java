package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.magic.thralls.Rs2Thrall;

/**
 * Arceuus thralls.
 *
 * <p>The constants of {@link Rs2Thrall} under a shorter name: {@code Thralls.LESSER_GHOST} is the very
 * same value as {@code Rs2Thrall.LESSER_GHOST}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2Thrall}:
 *
 * <pre>{@code
 * Rs2Thrall choice = Thralls.LESSER_GHOST;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class Thralls
{
    private Thralls()
    {
    }

    public static final Rs2Thrall LESSER_GHOST = Rs2Thrall.LESSER_GHOST;
    public static final Rs2Thrall LESSER_SKELETON = Rs2Thrall.LESSER_SKELETON;
    public static final Rs2Thrall LESSER_ZOMBIE = Rs2Thrall.LESSER_ZOMBIE;
    public static final Rs2Thrall SUPERIOR_GHOST = Rs2Thrall.SUPERIOR_GHOST;
    public static final Rs2Thrall SUPERIOR_SKELETON = Rs2Thrall.SUPERIOR_SKELETON;
    public static final Rs2Thrall SUPERIOR_ZOMBIE = Rs2Thrall.SUPERIOR_ZOMBIE;
    public static final Rs2Thrall GREATER_GHOST = Rs2Thrall.GREATER_GHOST;
    public static final Rs2Thrall GREATER_SKELETON = Rs2Thrall.GREATER_SKELETON;
    public static final Rs2Thrall GREATER_ZOMBIE = Rs2Thrall.GREATER_ZOMBIE;

    /** Every value, same as {@code Rs2Thrall.values()}. */
    public static Rs2Thrall[] values()
    {
        return Rs2Thrall.values();
    }

    /** Same as {@code Rs2Thrall.valueOf(name)}. */
    public static Rs2Thrall valueOf(String name)
    {
        return Rs2Thrall.valueOf(name);
    }

    /** Same as {@code Rs2Thrall.canCast(...)}. */
    public static boolean canCast(Rs2Thrall thrall)
    {
        return Rs2Thrall.canCast(thrall);
    }

    /** Same as {@code Rs2Thrall.isActive(...)}. */
    public static boolean isActive()
    {
        return Rs2Thrall.isActive();
    }

    /** Same as {@code Rs2Thrall.cast(...)}. */
    public static boolean cast(Rs2Thrall thrall)
    {
        return Rs2Thrall.cast(thrall);
    }

    /** Same as {@code Rs2Thrall.getBestThrall(...)}. */
    public static Rs2Thrall getBestThrall(net.runelite.client.plugins.projectx.util.magic.thralls.ThrallType type)
    {
        return Rs2Thrall.getBestThrall(type);
    }
}
