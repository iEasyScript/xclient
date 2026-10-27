package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;

/**
 * The items you carry: check, use, drop and combine them.
 *
 * <pre>{@code
 * if (Inventory.isFull()) {
 *     Bank.depositAll();
 * }
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Inventory} is available here under the shorter name,
 * so {@code Rs2Inventory.anything(...)} can be written {@code Inventory.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Inventory extends Rs2Inventory
{
    private Inventory()
    {
    }
}
