package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;

import java.util.stream.Stream;

/**
 * A search over the NPCs in the scene. Start one with {@link Npcs#query()}, add filters, then
 * finish with a terminal such as {@code nearest()}, {@code nearestReachable()}, {@code first()} or
 * {@code list()}:
 *
 * <pre>{@code
 * Npc match = Npcs.query().nameEquals("Cow").nearestReachable();
 * if (match != null) {
 *     match.interact("Attack");
 * }
 * }</pre>
 *
 * <p>Filters: {@code nameEquals}, {@code withName}, {@code withNames}, {@code withNameContains},
 * {@code withId}, {@code withIds}, {@code within}, {@code where}, {@code fromWorldView}.
 * Name filters read each entity's name on the client thread, so a name search off the client
 * thread is cheaper with the {@code *OnClientThread()} terminals ({@code nearestOnClientThread()},
 * {@code firstOnClientThread()}, {@code toListOnClientThread()}).
 *
 * <p>Reads the same per-tick cache as {@code ProjectX.getRs2NpcCache().query()}; the difference is
 * that matches come back typed as {@link Npc}.
 */
@SuppressWarnings("deprecation")
public final class NpcQuery extends AbstractEntityQueryable<NpcQuery, Npc>
        implements IEntityQueryable<NpcQuery, Npc>
{
    NpcQuery()
    {
        super();
    }

    @Override
    protected Stream<Npc> initialSource()
    {
        return ProjectX.getRs2NpcCache().getStream().map(Npc::of);
    }
}
