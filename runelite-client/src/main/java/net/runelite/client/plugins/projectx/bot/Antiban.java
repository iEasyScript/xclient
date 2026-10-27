package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;

/**
 * The built-in antiban behaviour (breaks in attention, mouse moves, activity profiles).
 *
 * <p>Every static method and field of {@link Rs2Antiban} is available here under the shorter name,
 * so {@code Rs2Antiban.anything(...)} can be written {@code Antiban.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Antiban extends Rs2Antiban
{
    private Antiban()
    {
    }
}
