package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.player.Rs2Player;

/**
 * Your own character: position, health, run energy, animation, eating.
 *
 * <pre>{@code
 * if (Player.isAnimating()) {
 *     return;
 * }
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Player} is available here under the shorter name,
 * so {@code Rs2Player.anything(...)} can be written {@code Player.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Player extends Rs2Player
{
    private Player()
    {
    }
}
