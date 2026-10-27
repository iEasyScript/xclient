package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.api.player.models.Rs2PlayerModel;

/**
 * A player in the scene (you or someone else), as returned by {@link Players#query()} and
 * {@link Players#local()}.
 *
 * <pre>{@code
 * PlayerModel friend = Players.query().nameEquals("Zezima").first();
 * }</pre>
 *
 * <p>Every method of the older {@link Rs2PlayerModel} works here, and a {@code PlayerModel} can be
 * passed anywhere an {@code Rs2PlayerModel} is expected. For your own character's state (health,
 * run energy, animation) the static helpers in {@link Player} are usually simpler.
 */
@SuppressWarnings("deprecation")
public class PlayerModel extends Rs2PlayerModel
{
    /** Your own character. */
    public PlayerModel()
    {
        super();
    }

    public PlayerModel(final net.runelite.api.Player player)
    {
        super(player);
    }

    /**
     * The same player as a {@code PlayerModel}. Returns {@code model} itself when it already is one.
     *
     * @return the player, or null when {@code model} is null
     */
    public static PlayerModel of(Rs2PlayerModel model)
    {
        if (model == null)
        {
            return null;
        }
        if (model instanceof PlayerModel)
        {
            return (PlayerModel) model;
        }
        return new PlayerModel(model.getPlayer());
    }
}
