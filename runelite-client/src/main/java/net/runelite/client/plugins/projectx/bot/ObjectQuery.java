package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;

import java.util.stream.Stream;

/**
 * A search over the scene objects (rocks, trees, doors, banks...) in the scene. Start one with {@link Objects#query()}, add filters, then
 * finish with a terminal such as {@code nearest()}, {@code nearestReachable()}, {@code first()} or
 * {@code list()}:
 *
 * <pre>{@code
 * SceneObject match = Objects.query().nameEquals("Copper rocks").nearestReachable();
 * if (match != null) {
 *     match.interact("Mine");
 * }
 * }</pre>
 *
 * <p>Filters: {@code nameEquals}, {@code withName}, {@code withNames}, {@code withNameContains},
 * {@code withId}, {@code withIds}, {@code within}, {@code where}, {@code fromWorldView}.
 * Name filters read each entity's name on the client thread, so a name search off the client
 * thread is cheaper with the {@code *OnClientThread()} terminals ({@code nearestOnClientThread()},
 * {@code firstOnClientThread()}, {@code toListOnClientThread()}).
 *
 * <p>Reads the same per-tick cache as {@code ProjectX.getRs2TileObjectCache().query()}; the difference is
 * that matches come back typed as {@link SceneObject}.
 */
@SuppressWarnings("deprecation")
public final class ObjectQuery extends AbstractEntityQueryable<ObjectQuery, SceneObject>
        implements IEntityQueryable<ObjectQuery, SceneObject>
{
    ObjectQuery()
    {
        super();
    }

    @Override
    protected Stream<SceneObject> initialSource()
    {
        return ProjectX.getRs2TileObjectCache().getStream().map(SceneObject::of);
    }
}
