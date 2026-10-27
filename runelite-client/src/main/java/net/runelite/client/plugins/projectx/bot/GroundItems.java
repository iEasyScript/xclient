package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.grounditem.Rs2GroundItem;

import java.util.stream.Stream;

/**
 * Items lying on the ground.
 *
 * <pre>{@code
 * FloorItem bones = GroundItems.query().nameEquals("Bones").within(10).nearestReachable();
 * if (bones != null) {
 *     bones.pickup();
 * }
 * }</pre>
 *
 * <p>{@link #query()} is the way to find ground items. The older static helpers of
 * {@link Rs2GroundItem} ({@code GroundItems.loot(...)}, ...) are reachable here too so existing code
 * can move over by renaming.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class GroundItems extends Rs2GroundItem
{
    private GroundItems()
    {
    }

    /**
     * Starts a search over the items on the ground, for example
     * {@code GroundItems.query().nameEquals("Coins").nearest()}.
     */
    public static GroundItemQuery query()
    {
        return new GroundItemQuery();
    }

    /**
     * Starts a search over the ground items named exactly {@code name} (ignoring case). Same as
     * {@code query().nameEquals(name)}, for example {@code GroundItems.query("Bones").nearest()}.
     */
    public static GroundItemQuery query(String name)
    {
        return query().nameEquals(name);
    }

    /** Every item on the ground in the scene this tick. */
    public static Stream<FloorItem> stream()
    {
        return ProjectX.getRs2TileItemCache().getStream().map(FloorItem::of);
    }
}
