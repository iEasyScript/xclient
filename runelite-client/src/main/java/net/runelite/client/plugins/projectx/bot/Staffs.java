package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.magic.Rs2Staff;

/**
 * Magic staves and the runes they provide.
 *
 * <p>The constants of {@link Rs2Staff} under a shorter name: {@code Staffs.NONE} is the very
 * same value as {@code Rs2Staff.NONE}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2Staff}:
 *
 * <pre>{@code
 * Rs2Staff choice = Staffs.NONE;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class Staffs
{
    private Staffs()
    {
    }

    public static final Rs2Staff NONE = Rs2Staff.NONE;
    public static final Rs2Staff STAFF_OF_AIR = Rs2Staff.STAFF_OF_AIR;
    public static final Rs2Staff STAFF_OF_WATER = Rs2Staff.STAFF_OF_WATER;
    public static final Rs2Staff STAFF_OF_EARTH = Rs2Staff.STAFF_OF_EARTH;
    public static final Rs2Staff STAFF_OF_FIRE = Rs2Staff.STAFF_OF_FIRE;
    public static final Rs2Staff AIR_BATTLESTAFF = Rs2Staff.AIR_BATTLESTAFF;
    public static final Rs2Staff WATER_BATTLESTAFF = Rs2Staff.WATER_BATTLESTAFF;
    public static final Rs2Staff EARTH_BATTLESTAFF = Rs2Staff.EARTH_BATTLESTAFF;
    public static final Rs2Staff FIRE_BATTLESTAFF = Rs2Staff.FIRE_BATTLESTAFF;
    public static final Rs2Staff DUST_BATTLESTAFF = Rs2Staff.DUST_BATTLESTAFF;
    public static final Rs2Staff LAVA_BATTLESTAFF = Rs2Staff.LAVA_BATTLESTAFF;
    public static final Rs2Staff MIST_BATTLESTAFF = Rs2Staff.MIST_BATTLESTAFF;
    public static final Rs2Staff MUD_BATTLESTAFF = Rs2Staff.MUD_BATTLESTAFF;
    public static final Rs2Staff SMOKE_BATTLESTAFF = Rs2Staff.SMOKE_BATTLESTAFF;
    public static final Rs2Staff STEAM_BATTLESTAFF = Rs2Staff.STEAM_BATTLESTAFF;
    public static final Rs2Staff MYSTIC_AIR_STAFF = Rs2Staff.MYSTIC_AIR_STAFF;
    public static final Rs2Staff MYSTIC_WATER_STAFF = Rs2Staff.MYSTIC_WATER_STAFF;
    public static final Rs2Staff MYSTIC_EARTH_STAFF = Rs2Staff.MYSTIC_EARTH_STAFF;
    public static final Rs2Staff MYSTIC_FIRE_STAFF = Rs2Staff.MYSTIC_FIRE_STAFF;
    public static final Rs2Staff MYSTIC_DUST_STAFF = Rs2Staff.MYSTIC_DUST_STAFF;
    public static final Rs2Staff MYSTIC_LAVA_STAFF = Rs2Staff.MYSTIC_LAVA_STAFF;
    public static final Rs2Staff MYSTIC_MIST_STAFF = Rs2Staff.MYSTIC_MIST_STAFF;
    public static final Rs2Staff MYSTIC_MUD_STAFF = Rs2Staff.MYSTIC_MUD_STAFF;
    public static final Rs2Staff MYSTIC_SMOKE_STAFF = Rs2Staff.MYSTIC_SMOKE_STAFF;
    public static final Rs2Staff MYSTIC_STEAM_STAFF = Rs2Staff.MYSTIC_STEAM_STAFF;
    public static final Rs2Staff TWINFLAME_STAFF = Rs2Staff.TWINFLAME_STAFF;
    public static final Rs2Staff LAVA_BATTLESTAFF_OR = Rs2Staff.LAVA_BATTLESTAFF_OR;
    public static final Rs2Staff MYSTIC_LAVA_STAFF_OR = Rs2Staff.MYSTIC_LAVA_STAFF_OR;
    public static final Rs2Staff STEAM_BATTLESTAFF_OR = Rs2Staff.STEAM_BATTLESTAFF_OR;
    public static final Rs2Staff MYSTIC_STEAM_STAFF_OR = Rs2Staff.MYSTIC_STEAM_STAFF_OR;
    public static final Rs2Staff KODAI_WAND = Rs2Staff.KODAI_WAND;

    /** Every value, same as {@code Rs2Staff.values()}. */
    public static Rs2Staff[] values()
    {
        return Rs2Staff.values();
    }

    /** Same as {@code Rs2Staff.valueOf(name)}. */
    public static Rs2Staff valueOf(String name)
    {
        return Rs2Staff.valueOf(name);
    }
}
