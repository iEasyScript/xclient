package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;

/**
 * What you are wearing: check, equip and remove gear.
 *
 * <pre>{@code
 * boolean axe = Equipment.isWearing("Bronze axe");
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Equipment} is available here under the shorter name,
 * so {@code Rs2Equipment.anything(...)} can be written {@code Equipment.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Equipment extends Rs2Equipment
{
    private Equipment()
    {
    }
}
