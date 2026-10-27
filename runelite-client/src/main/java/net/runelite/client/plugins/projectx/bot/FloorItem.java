package net.runelite.client.plugins.projectx.bot;

import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel;

/**
 * An item lying on the ground, as returned by {@link GroundItems#query()}.
 *
 * <pre>{@code
 * FloorItem bones = GroundItems.query().nameEquals("Bones").nearestReachable();
 * if (bones != null) {
 *     bones.interact("Take");   // or bones.pickup()
 * }
 * }</pre>
 *
 * <p>Every method of the older {@link Rs2TileItemModel} works here, and a {@code FloorItem} can be
 * passed anywhere an {@code Rs2TileItemModel} is expected. Named {@code FloorItem} so it does not
 * clash with the ground items plugin's own {@code GroundItem} class.
 */
@SuppressWarnings("deprecation")
public class FloorItem extends Rs2TileItemModel
{
    public FloorItem(Tile tile, TileItem tileItem)
    {
        super(tile, tileItem);
    }

    /**
     * The same item as a {@code FloorItem}. Returns {@code model} itself when it already is one.
     *
     * @return the item, or null when {@code model} is null
     */
    public static FloorItem of(Rs2TileItemModel model)
    {
        if (model == null)
        {
            return null;
        }
        if (model instanceof FloorItem)
        {
            return (FloorItem) model;
        }
        return new FloorItem(model.getTile(), model.getTileItem());
    }
}
