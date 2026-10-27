package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.magic.Rs2CombatSpells;

/**
 * Combat (autocast) spells.
 *
 * <p>The constants of {@link Rs2CombatSpells} under a shorter name: {@code CombatSpells.WIND_STRIKE} is the very
 * same value as {@code Rs2CombatSpells.WIND_STRIKE}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2CombatSpells}:
 *
 * <pre>{@code
 * Rs2CombatSpells choice = CombatSpells.WIND_STRIKE;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class CombatSpells
{
    private CombatSpells()
    {
    }

    public static final Rs2CombatSpells WIND_STRIKE = Rs2CombatSpells.WIND_STRIKE;
    public static final Rs2CombatSpells WATER_STRIKE = Rs2CombatSpells.WATER_STRIKE;
    public static final Rs2CombatSpells EARTH_STRIKE = Rs2CombatSpells.EARTH_STRIKE;
    public static final Rs2CombatSpells FIRE_STRIKE = Rs2CombatSpells.FIRE_STRIKE;
    public static final Rs2CombatSpells WIND_BOLT = Rs2CombatSpells.WIND_BOLT;
    public static final Rs2CombatSpells WATER_BOLT = Rs2CombatSpells.WATER_BOLT;
    public static final Rs2CombatSpells EARTH_BOLT = Rs2CombatSpells.EARTH_BOLT;
    public static final Rs2CombatSpells FIRE_BOLT = Rs2CombatSpells.FIRE_BOLT;
    public static final Rs2CombatSpells WIND_BLAST = Rs2CombatSpells.WIND_BLAST;
    public static final Rs2CombatSpells WATER_BLAST = Rs2CombatSpells.WATER_BLAST;
    public static final Rs2CombatSpells EARTH_BLAST = Rs2CombatSpells.EARTH_BLAST;
    public static final Rs2CombatSpells FIRE_BLAST = Rs2CombatSpells.FIRE_BLAST;
    public static final Rs2CombatSpells WIND_WAVE = Rs2CombatSpells.WIND_WAVE;
    public static final Rs2CombatSpells WATER_WAVE = Rs2CombatSpells.WATER_WAVE;
    public static final Rs2CombatSpells EARTH_WAVE = Rs2CombatSpells.EARTH_WAVE;
    public static final Rs2CombatSpells FIRE_WAVE = Rs2CombatSpells.FIRE_WAVE;
    public static final Rs2CombatSpells WIND_SURGE = Rs2CombatSpells.WIND_SURGE;
    public static final Rs2CombatSpells WATER_SURGE = Rs2CombatSpells.WATER_SURGE;
    public static final Rs2CombatSpells EARTH_SURGE = Rs2CombatSpells.EARTH_SURGE;
    public static final Rs2CombatSpells FIRE_SURGE = Rs2CombatSpells.FIRE_SURGE;
    public static final Rs2CombatSpells SMOKE_RUSH = Rs2CombatSpells.SMOKE_RUSH;
    public static final Rs2CombatSpells SHADOW_RUSH = Rs2CombatSpells.SHADOW_RUSH;
    public static final Rs2CombatSpells BLOOD_RUSH = Rs2CombatSpells.BLOOD_RUSH;
    public static final Rs2CombatSpells ICE_RUSH = Rs2CombatSpells.ICE_RUSH;
    public static final Rs2CombatSpells SMOKE_BURST = Rs2CombatSpells.SMOKE_BURST;
    public static final Rs2CombatSpells SHADOW_BURST = Rs2CombatSpells.SHADOW_BURST;
    public static final Rs2CombatSpells BLOOD_BURST = Rs2CombatSpells.BLOOD_BURST;
    public static final Rs2CombatSpells ICE_BURST = Rs2CombatSpells.ICE_BURST;
    public static final Rs2CombatSpells SMOKE_BLITZ = Rs2CombatSpells.SMOKE_BLITZ;
    public static final Rs2CombatSpells SHADOW_BLITZ = Rs2CombatSpells.SHADOW_BLITZ;
    public static final Rs2CombatSpells BLOOD_BLITZ = Rs2CombatSpells.BLOOD_BLITZ;
    public static final Rs2CombatSpells ICE_BLITZ = Rs2CombatSpells.ICE_BLITZ;
    public static final Rs2CombatSpells SMOKE_BARRAGE = Rs2CombatSpells.SMOKE_BARRAGE;
    public static final Rs2CombatSpells SHADOW_BARRAGE = Rs2CombatSpells.SHADOW_BARRAGE;
    public static final Rs2CombatSpells BLOOD_BARRAGE = Rs2CombatSpells.BLOOD_BARRAGE;
    public static final Rs2CombatSpells ICE_BARRAGE = Rs2CombatSpells.ICE_BARRAGE;

    /** Every value, same as {@code Rs2CombatSpells.values()}. */
    public static Rs2CombatSpells[] values()
    {
        return Rs2CombatSpells.values();
    }

    /** Same as {@code Rs2CombatSpells.valueOf(name)}. */
    public static Rs2CombatSpells valueOf(String name)
    {
        return Rs2CombatSpells.valueOf(name);
    }
}
