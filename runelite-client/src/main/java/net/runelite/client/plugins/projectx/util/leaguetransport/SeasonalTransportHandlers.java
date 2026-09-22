package net.runelite.client.plugins.projectx.util.leaguetransport;

import net.runelite.client.plugins.projectx.shortestpath.Transport;

import java.util.List;

/**
 * Built-in {@link SeasonalTransportHandler} instances and default ordering.
 */
public final class SeasonalTransportHandlers
{
	private SeasonalTransportHandlers()
	{
	}

	public static final SeasonalTransportHandler LEAGUES_AREA = new SeasonalTransportHandler()
	{
		@Override
		public boolean matches(Transport transport)
		{
			return Rs2LeaguesTransport.matchesLeaguesAreaTransportPrefix(transport);
		}

		@Override
		public boolean tryUse(Transport transport)
		{
			return Rs2LeaguesTransport.tryHandleLeaguesAreaTransport(transport);
		}
	};

	public static List<SeasonalTransportHandler> defaultHandlerList()
	{
		return List.of(LEAGUES_AREA);
	}
}
