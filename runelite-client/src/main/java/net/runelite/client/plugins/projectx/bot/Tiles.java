package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;

/**
 * Tile helpers: reachability, walkable tiles, dangerous tiles, line of sight.
 *
 * <p>Every static method and field of {@link Rs2Tile} is available here under the shorter name.
 * Named {@code Tiles} so it does not clash with RuneLite's {@code net.runelite.api.Tile}.
 */
@SuppressWarnings({"deprecation", "removal"})
public abstract class Tiles extends Rs2Tile
{
    private Tiles()
    {
    }
}
