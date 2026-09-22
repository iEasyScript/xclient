package net.runelite.client.plugins.projectx.util.leaguetransport;

import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SeasonalTransportHandlersTest
{
	@Test
	public void defaultHandlerList_orderAndSize()
	{
		var handlers = SeasonalTransportHandlers.defaultHandlerList();
		assertTrue(handlers.size() >= 1);
		assertSame(SeasonalTransportHandlers.LEAGUES_AREA, handlers.get(0));
	}
}
