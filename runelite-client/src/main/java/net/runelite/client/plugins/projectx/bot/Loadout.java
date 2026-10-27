package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.util.Rs2InventorySetup;

import java.util.concurrent.ScheduledFuture;

/**
 * Loads a saved inventory setup (gear and inventory) from the bank.
 *
 * <pre>{@code
 * Loadout loadout = new Loadout("Mining", mainScheduledFuture);
 * if (!loadout.doesInventoryMatch()) {
 *     loadout.loadInventory();
 * }
 * }</pre>
 *
 * <p>Every method of the older {@link Rs2InventorySetup} works here. Named {@code Loadout} because
 * {@link InventorySetup} is already the name of the saved setup itself.
 */
@SuppressWarnings("deprecation")
public class Loadout extends Rs2InventorySetup
{
    public Loadout(String name, ScheduledFuture<?> mainScheduler)
    {
        super(name, mainScheduler);
    }

    public Loadout(InventorySetup setup, ScheduledFuture<?> mainScheduler)
    {
        super(setup, mainScheduler);
    }
}
