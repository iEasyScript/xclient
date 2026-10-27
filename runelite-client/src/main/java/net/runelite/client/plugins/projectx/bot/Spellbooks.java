package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.magic.Rs2Spellbook;

/**
 * The spellbooks, and switching between them.
 *
 * <p>The constants of {@link Rs2Spellbook} under a shorter name: {@code Spellbooks.MODERN} is the very
 * same value as {@code Rs2Spellbook.MODERN}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2Spellbook}:
 *
 * <pre>{@code
 * Rs2Spellbook choice = Spellbooks.MODERN;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class Spellbooks
{
    private Spellbooks()
    {
    }

    public static final Rs2Spellbook MODERN = Rs2Spellbook.MODERN;
    public static final Rs2Spellbook ANCIENT = Rs2Spellbook.ANCIENT;
    public static final Rs2Spellbook LUNAR = Rs2Spellbook.LUNAR;
    public static final Rs2Spellbook ARCEUUS = Rs2Spellbook.ARCEUUS;

    /** Every value, same as {@code Rs2Spellbook.values()}. */
    public static Rs2Spellbook[] values()
    {
        return Rs2Spellbook.values();
    }

    /** Same as {@code Rs2Spellbook.valueOf(name)}. */
    public static Rs2Spellbook valueOf(String name)
    {
        return Rs2Spellbook.valueOf(name);
    }

    /** Same as {@code Rs2Spellbook.fromValue(...)}. */
    public static Rs2Spellbook fromValue(int value)
    {
        return Rs2Spellbook.fromValue(value);
    }

    /** Same as {@code Rs2Spellbook.getCurrentSpellbook(...)}. */
    public static Rs2Spellbook getCurrentSpellbook()
    {
        return Rs2Spellbook.getCurrentSpellbook();
    }

    /** Same as {@code Rs2Spellbook.isLunarSpellBookUnlocked(...)}. */
    public static boolean isLunarSpellBookUnlocked()
    {
        return Rs2Spellbook.isLunarSpellBookUnlocked();
    }

    /** Same as {@code Rs2Spellbook.isAncientSpellBookUnlocked(...)}. */
    public static boolean isAncientSpellBookUnlocked()
    {
        return Rs2Spellbook.isAncientSpellBookUnlocked();
    }

    /** Same as {@code Rs2Spellbook.isArceuusSpellBookFullyUnlocked(...)}. */
    public static boolean isArceuusSpellBookFullyUnlocked()
    {
        return Rs2Spellbook.isArceuusSpellBookFullyUnlocked();
    }

    /** Same as {@code Rs2Spellbook.isArceuusSpellBookUnlocked(...)}. */
    public static boolean isArceuusSpellBookUnlocked()
    {
        return Rs2Spellbook.isArceuusSpellBookUnlocked();
    }

    /** Same as {@code Rs2Spellbook.isSpellbookChangedByLunar(...)}. */
    public static boolean isSpellbookChangedByLunar()
    {
        return Rs2Spellbook.isSpellbookChangedByLunar();
    }

    /** Same as {@code Rs2Spellbook.switchViaNpc(...)}. */
    public static boolean switchViaNpc()
    {
        return Rs2Spellbook.switchViaNpc();
    }

    /** Same as {@code Rs2Spellbook.isAltarNearby(...)}. */
    public static boolean isAltarNearby()
    {
        return Rs2Spellbook.isAltarNearby();
    }

    /** Same as {@code Rs2Spellbook.isTyssNearby(...)}. */
    public static boolean isTyssNearby()
    {
        return Rs2Spellbook.isTyssNearby();
    }

    /** Same as {@code Rs2Spellbook.getNearestSwitchLocation(...)}. */
    public static net.runelite.api.coords.WorldPoint getNearestSwitchLocation(boolean includeBankeItems)
    {
        return Rs2Spellbook.getNearestSwitchLocation(includeBankeItems);
    }

    /** Same as {@code Rs2Spellbook.canSwitchInPOH(...)}. */
    public static boolean canSwitchInPOH()
    {
        return Rs2Spellbook.canSwitchInPOH();
    }
}
