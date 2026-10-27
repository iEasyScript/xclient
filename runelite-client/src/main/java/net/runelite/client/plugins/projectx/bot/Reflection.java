package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.reflection.Rs2Reflection;

/**
 * Low-level menu and item-action reflection helpers.
 *
 * <p>Every static method and field of {@link Rs2Reflection} is available here under the shorter name,
 * so {@code Rs2Reflection.anything(...)} can be written {@code Reflection.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Reflection extends Rs2Reflection
{
    private Reflection()
    {
    }
}
