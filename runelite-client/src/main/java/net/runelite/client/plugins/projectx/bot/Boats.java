package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.boat.models.Rs2BoatModel;
import net.runelite.client.plugins.projectx.api.player.models.Rs2PlayerModel;

/**
 * Sailing boats.
 *
 * <pre>{@code
 * Rs2BoatModel boat = Boats.local();
 * if (boat != null) { ... }
 * }</pre>
 *
 * <p>The same as {@code ProjectX.getRs2BoatCache()}, as static methods. To steer, see
 * {@link BoatNavigator}.
 */
@SuppressWarnings("deprecation")
public final class Boats
{
    private Boats()
    {
    }

    /** The boat you are on, or null. Same as {@code Rs2BoatCache.getLocalBoat()}. */
    public static Rs2BoatModel local()
    {
        return ProjectX.getRs2BoatCache().getLocalBoat();
    }

    /** The boat {@code player} is on, or null. Same as {@code Rs2BoatCache.getBoat(player)}. */
    public static Rs2BoatModel of(Rs2PlayerModel player)
    {
        return ProjectX.getRs2BoatCache().getBoat(player);
    }
}
