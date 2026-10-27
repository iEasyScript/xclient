package net.runelite.client.plugins.projectx.bot;

import net.runelite.api.ItemComposition;
import net.runelite.http.api.item.ItemPrice;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.item.Rs2ItemManager;

import java.util.List;

/**
 * Item lookups: ids by name, prices, compositions.
 *
 * <pre>{@code
 * int id = Items.getItemId("Shark");
 * int gePrice = Items.getGEPrice(id);
 * }</pre>
 *
 * <p>The same lookups as {@link Rs2ItemManager} and {@code ProjectX.getRs2ItemManager()}, as
 * static methods. Named {@code Items} so it does not clash with RuneLite's {@code ItemManager}.
 */
@SuppressWarnings("deprecation")
public final class Items
{
    private Items()
    {
    }

    /** Same as {@link Rs2ItemManager#getItemIdByName(String, boolean)}. */
    public static int getItemIdByName(String itemName, boolean fuzzy)
    {
        return Rs2ItemManager.getItemIdByName(itemName, fuzzy);
    }

    /** Items whose name matches {@code query}, with prices. */
    public static List<ItemPrice> searchItem(String query)
    {
        return ProjectX.getRs2ItemManager().searchItem(query);
    }

    /** The id of the first item named {@code itemName}, or -1. */
    public static int getItemId(String itemName)
    {
        return ProjectX.getRs2ItemManager().getItemId(itemName);
    }

    /** The item's definition (name, actions, noted/stackable), or null. */
    public static ItemComposition getItemComposition(int itemId)
    {
        return ProjectX.getRs2ItemManager().getItemComposition(itemId);
    }

    /** The item's store value, or -1. */
    public static int getPrice(int itemId)
    {
        return ProjectX.getRs2ItemManager().getPrice(itemId);
    }

    /** The Grand Exchange price of the item named {@code itemName}, or -1. */
    public static int getGEPrice(String itemName)
    {
        return ProjectX.getRs2ItemManager().getGEPrice(itemName);
    }

    /** The Grand Exchange price of the item, or -1. */
    public static int getGEPrice(int itemId)
    {
        return ProjectX.getRs2ItemManager().getGEPrice(itemId);
    }
}
