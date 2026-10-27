package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.projectx.util.prayer.Rs2PrayerEnum;

/**
 * Prayers: turn them on and off, check points, quick prayers, flicking.
 *
 * <pre>{@code
 * Prayers.toggle(Prayers.PROTECT_MELEE, true);
 * if (Prayers.isPrayerActive(Prayers.PIETY)) { ... }
 * }</pre>
 *
 * <p>Every static method of {@link Rs2Prayer} is available here, and the prayer constants of
 * {@link Rs2PrayerEnum} are here too: {@code Prayers.PIETY} is the very same value as
 * {@code Rs2PrayerEnum.PIETY}. A variable or parameter that holds a prayer still has the type
 * {@code Rs2PrayerEnum} (Java enums cannot be renamed without breaking compiled scripts).
 * Named {@code Prayers} so it does not clash with RuneLite's {@code net.runelite.api.Prayer}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Prayers extends Rs2Prayer
{
    private Prayers()
    {
    }

    public static final Rs2PrayerEnum THICK_SKIN = Rs2PrayerEnum.THICK_SKIN;
    public static final Rs2PrayerEnum BURST_STRENGTH = Rs2PrayerEnum.BURST_STRENGTH;
    public static final Rs2PrayerEnum CLARITY_THOUGHT = Rs2PrayerEnum.CLARITY_THOUGHT;
    public static final Rs2PrayerEnum SHARP_EYE = Rs2PrayerEnum.SHARP_EYE;
    public static final Rs2PrayerEnum MYSTIC_WILL = Rs2PrayerEnum.MYSTIC_WILL;
    public static final Rs2PrayerEnum ROCK_SKIN = Rs2PrayerEnum.ROCK_SKIN;
    public static final Rs2PrayerEnum SUPERHUMAN_STRENGTH = Rs2PrayerEnum.SUPERHUMAN_STRENGTH;
    public static final Rs2PrayerEnum IMPROVED_REFLEXES = Rs2PrayerEnum.IMPROVED_REFLEXES;
    public static final Rs2PrayerEnum RAPID_RESTORE = Rs2PrayerEnum.RAPID_RESTORE;
    public static final Rs2PrayerEnum RAPID_HEAL = Rs2PrayerEnum.RAPID_HEAL;
    public static final Rs2PrayerEnum PROTECT_ITEM = Rs2PrayerEnum.PROTECT_ITEM;
    public static final Rs2PrayerEnum HAWK_EYE = Rs2PrayerEnum.HAWK_EYE;
    public static final Rs2PrayerEnum MYSTIC_LORE = Rs2PrayerEnum.MYSTIC_LORE;
    public static final Rs2PrayerEnum STEEL_SKIN = Rs2PrayerEnum.STEEL_SKIN;
    public static final Rs2PrayerEnum ULTIMATE_STRENGTH = Rs2PrayerEnum.ULTIMATE_STRENGTH;
    public static final Rs2PrayerEnum INCREDIBLE_REFLEXES = Rs2PrayerEnum.INCREDIBLE_REFLEXES;
    public static final Rs2PrayerEnum PROTECT_MAGIC = Rs2PrayerEnum.PROTECT_MAGIC;
    public static final Rs2PrayerEnum PROTECT_RANGE = Rs2PrayerEnum.PROTECT_RANGE;
    public static final Rs2PrayerEnum PROTECT_MELEE = Rs2PrayerEnum.PROTECT_MELEE;
    public static final Rs2PrayerEnum EAGLE_EYE = Rs2PrayerEnum.EAGLE_EYE;
    public static final Rs2PrayerEnum DEAD_EYE = Rs2PrayerEnum.DEAD_EYE;
    public static final Rs2PrayerEnum MYSTIC_MIGHT = Rs2PrayerEnum.MYSTIC_MIGHT;
    public static final Rs2PrayerEnum MYSTIC_VIGOUR = Rs2PrayerEnum.MYSTIC_VIGOUR;
    public static final Rs2PrayerEnum RETRIBUTION = Rs2PrayerEnum.RETRIBUTION;
    public static final Rs2PrayerEnum REDEMPTION = Rs2PrayerEnum.REDEMPTION;
    public static final Rs2PrayerEnum SMITE = Rs2PrayerEnum.SMITE;
    public static final Rs2PrayerEnum PRESERVE = Rs2PrayerEnum.PRESERVE;
    public static final Rs2PrayerEnum CHIVALRY = Rs2PrayerEnum.CHIVALRY;
    public static final Rs2PrayerEnum PIETY = Rs2PrayerEnum.PIETY;
    public static final Rs2PrayerEnum RIGOUR = Rs2PrayerEnum.RIGOUR;
    public static final Rs2PrayerEnum AUGURY = Rs2PrayerEnum.AUGURY;

    /** Every value, same as {@code Rs2PrayerEnum.values()}. */
    public static Rs2PrayerEnum[] values()
    {
        return Rs2PrayerEnum.values();
    }

    /** Same as {@code Rs2PrayerEnum.valueOf(name)}. */
    public static Rs2PrayerEnum valueOf(String name)
    {
        return Rs2PrayerEnum.valueOf(name);
    }
}
