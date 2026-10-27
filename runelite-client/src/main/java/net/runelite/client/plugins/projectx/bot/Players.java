package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;

import java.util.stream.Stream;

/**
 * Players in the scene, yourself included.
 *
 * <pre>{@code
 * PlayerModel friend = Players.query().nameEquals("Zezima").first();
 * int nearby = Players.query().within(10).count();
 * }</pre>
 *
 * <p>For your own character's state (health, run energy, animation, eating) use {@link Player}.
 */
@SuppressWarnings("deprecation")
public final class Players
{
    private Players()
    {
    }

    /**
     * Starts a search over the players in the scene, for example
     * {@code Players.query().within(5).list()}.
     */
    public static PlayerQuery query()
    {
        return new PlayerQuery();
    }

    /**
     * Starts a search over the players named exactly {@code name} (ignoring case). Same as
     * {@code query().nameEquals(name)}, for example {@code Players.query("Zezima").nearest()}.
     */
    public static PlayerQuery query(String name)
    {
        return query().nameEquals(name);
    }

    /** Every player in the scene this tick. */
    public static Stream<PlayerModel> stream()
    {
        return ProjectX.getRs2PlayerCache().getStream().map(PlayerModel::of);
    }

    /** Your own character, as a {@link PlayerModel}. */
    public static PlayerModel local()
    {
        return new PlayerModel();
    }
}
