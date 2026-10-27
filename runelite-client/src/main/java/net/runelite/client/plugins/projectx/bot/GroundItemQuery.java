package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;

import java.util.stream.Stream;

/**
 * A search over the items on the ground in the scene. Start one with {@link GroundItems#query()}, add filters, then
 * finish with a terminal such as {@code nearest()}, {@code nearestReachable()}, {@code first()} or
 * {@code list()}:
 *
 * <pre>{@code
 * FloorItem match = GroundItems.query().nameEquals("Bones").nearestReachable();
 * if (match != null) {
 *     match.interact("Take");
 * }
 * }</pre>
 *
 * <p>Filters: {@code nameEquals}, {@code withName}, {@code withNames}, {@code withNameContains},
 * {@code withId}, {@code withIds}, {@code within}, {@code where}, {@code fromWorldView}.
 * Name filters read each entity's name on the client thread, so a name search off the client
 * thread is cheaper with the {@code *OnClientThread()} terminals ({@code nearestOnClientThread()},
 * {@code firstOnClientThread()}, {@code toListOnClientThread()}).
 *
 * <p>Reads the same per-tick cache as {@code ProjectX.getRs2TileItemCache().query()}; the difference is
 * that matches come back typed as {@link FloorItem}.
 */
@SuppressWarnings("deprecation")
public final class GroundItemQuery extends AbstractEntityQueryable<GroundItemQuery, FloorItem>
        implements IEntityQueryable<GroundItemQuery, FloorItem>
{
    GroundItemQuery()
    {
        super();
    }

    @Override
    protected Stream<FloorItem> initialSource()
    {
        return ProjectX.getRs2TileItemCache().getStream().map(FloorItem::of);
    }
}
