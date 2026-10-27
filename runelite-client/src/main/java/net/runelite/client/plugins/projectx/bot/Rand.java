package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.math.Rs2Random;

/**
 * Random numbers and human-like random delays. Named {@code Rand} so it does not clash with {@link java.util.Random}.
 *
 * <pre>{@code
 * sleep(Rand.between(600, 900));
 * }</pre>
 *
 * <p>Every static method and field of {@link Rs2Random} is available here under the shorter name,
 * so {@code Rs2Random.anything(...)} can be written {@code Rand.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class Rand extends Rs2Random
{
    private Rand()
    {
    }
}
