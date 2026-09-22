package net.runelite.client.plugins.projectx.util.leaguetransport;

import net.runelite.client.plugins.projectx.shortestpath.Transport;

/**
 * Pluggable seasonal row executor for {@link net.runelite.client.plugins.projectx.util.walker.Rs2Walker}.
 * Order is defined by the list passed to {@link net.runelite.client.plugins.projectx.util.walker.Rs2Walker#setSeasonalTransportHandlers}.
 */
public interface SeasonalTransportHandler
{
	/**
	 * Cheap gate before {@link #tryUse(Transport)} — should not open UI or block.
	 */
	boolean matches(Transport transport);

	/**
	 * Attempts this handler's seasonal flow (not on the RuneLite client thread — same rules as Leagues/MoA).
	 *
	 * @return {@code true} when this handler consumed the transport attempt successfully
	 */
	boolean tryUse(Transport transport);
}
