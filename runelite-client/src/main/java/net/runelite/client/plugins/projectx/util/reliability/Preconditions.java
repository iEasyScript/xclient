package net.runelite.client.plugins.projectx.util.reliability;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;

import java.util.function.BooleanSupplier;

/**
 * Ready-made {@link Precondition}s built only on the existing Rs2 utilities and caches.
 * Each check is evaluated when the guarded step runs, on the script thread; the utilities
 * underneath do their own client-thread hops. Nothing here clicks or changes game state,
 * with one inherited exception: {@link Rs2Bank#isOpen()} enters the bank PIN if the PIN
 * screen is up, as it does everywhere else it is called.
 */
public final class Preconditions
{
    private Preconditions()
    {
    }

    /** Any check of the script's own. */
    public static Precondition check(String description, BooleanSupplier check)
    {
        return Precondition.of(description, check);
    }

    public static Precondition loggedIn()
    {
        return Precondition.of("logged in", ProjectX::isLoggedIn);
    }

    /** An item whose name matches (ignoring case, as {@link Rs2Inventory#hasItem(String...)} does). */
    public static Precondition hasItem(String name)
    {
        return Precondition.of("has " + name, () -> Rs2Inventory.hasItem(name));
    }

    public static Precondition hasItem(int itemId)
    {
        return Precondition.of("has item " + itemId, () -> Rs2Inventory.hasItem(itemId));
    }

    public static Precondition hasItemAmount(String name, int amount)
    {
        return Precondition.of("has " + amount + " x " + name, () -> Rs2Inventory.hasItemAmount(name, amount));
    }

    public static Precondition hasItemAmount(int itemId, int amount)
    {
        return Precondition.of("has " + amount + " x item " + itemId, () -> Rs2Inventory.hasItemAmount(itemId, amount));
    }

    public static Precondition freeSlots(int slots)
    {
        return Precondition.of(slots + " free inventory slot(s)", () -> Rs2Inventory.emptySlotCount() >= slots);
    }

    public static Precondition inventoryNotFull()
    {
        return Precondition.of("inventory not full", () -> !Rs2Inventory.isFull());
    }

    public static Precondition bankOpen()
    {
        return Precondition.of("bank open", Rs2Bank::isOpen);
    }

    public static Precondition bankClosed()
    {
        return Precondition.of("bank closed", () -> !Rs2Bank.isOpen());
    }

    /** An NPC with this name is loaded nearby. */
    public static Precondition npcPresent(String name)
    {
        return Precondition.of(name + " nearby",
            () -> ProjectX.getRs2NpcCache().query().withName(name).nearestOnClientThread() != null);
    }

    /** A game object with this name is loaded nearby. */
    public static Precondition objectPresent(String name)
    {
        return Precondition.of(name + " nearby",
            () -> ProjectX.getRs2TileObjectCache().query().withName(name).nearestOnClientThread() != null);
    }

    /** The tile can be walked to from where the player stands, without doors or obstacles. */
    public static Precondition reachable(WorldPoint tile)
    {
        return Precondition.of("can reach " + describe(tile), () -> Rs2Tile.isTileReachable(tile));
    }

    /** The player is within {@code radius} tiles of {@code centre}. */
    public static Precondition playerNear(WorldPoint centre, int radius)
    {
        return Precondition.of("within " + radius + " tiles of " + describe(centre),
            () -> Rs2Player.isInArea(centre, radius));
    }

    private static String describe(WorldPoint p)
    {
        return p == null ? "nowhere" : p.getX() + "," + p.getY() + "," + p.getPlane();
    }
}
