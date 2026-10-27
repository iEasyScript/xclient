package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.AbstractEntityQueryable;
import net.runelite.client.plugins.projectx.api.IEntityQueryable;

import java.util.stream.Stream;

/**
 * A search over the players in the scene. Start one with {@link Players#query()}, add filters, then
 * finish with a terminal such as {@code nearest()}, {@code nearestReachable()}, {@code first()} or
 * {@code list()}:
 *
 * <pre>{@code
 * PlayerModel match = Players.query().nameEquals("Zezima").first();
 * if (match != null) {
 *     match.getCombatLevel();
 * }
 * }</pre>
 *
 * <p>Filters: {@code nameEquals}, {@code withName}, {@code withNames}, {@code withNameContains},
 * {@code withId}, {@code withIds}, {@code within}, {@code where}, {@code fromWorldView}.
 * Name filters read each entity's name on the client thread, so a name search off the client
 * thread is cheaper with the {@code *OnClientThread()} terminals ({@code nearestOnClientThread()},
 * {@code firstOnClientThread()}, {@code toListOnClientThread()}).
 *
 * <p>Reads the same per-tick cache as {@code ProjectX.getRs2PlayerCache().query()}; the difference is
 * that matches come back typed as {@link PlayerModel}.
 */
@SuppressWarnings("deprecation")
public final class PlayerQuery extends AbstractEntityQueryable<PlayerQuery, PlayerModel>
        implements IEntityQueryable<PlayerQuery, PlayerModel>
{
    PlayerQuery()
    {
        super();
    }

    @Override
    protected Stream<PlayerModel> initialSource()
    {
        return ProjectX.getRs2PlayerCache().getStream().map(PlayerModel::of);
    }
}
