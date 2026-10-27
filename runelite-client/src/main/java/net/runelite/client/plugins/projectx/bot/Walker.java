package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

/**
 * Walking: web-walks to any tile, including doors, stairs and transports.
 *
 * <pre>{@code
 * Walker.walkTo(new WorldPoint(3222, 3218, 0));
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Walker} is available here under the shorter name,
 * so {@code Rs2Walker.anything(...)} can be written {@code Walker.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Walker extends Rs2Walker
{
    private Walker()
    {
    }
}
