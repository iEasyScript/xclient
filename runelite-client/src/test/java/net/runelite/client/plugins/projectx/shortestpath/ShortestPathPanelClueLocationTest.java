package net.runelite.client.plugins.projectx.shortestpath;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.plugins.cluescrolls.clues.ClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.LocationClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.LocationsClueScroll;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ShortestPathPanelClueLocationTest
{
	private static final WorldPoint LOCATION = new WorldPoint(2440, 5170, 0);

	private final AtomicBoolean onClientThread = new AtomicBoolean(false);
	private MockedStatic<ProjectX> projectx;
	private ClientThread clientThread;
	private ClueScrollPlugin cluePlugin;
	private ShortestPathPanel panel;

	@Before
	public void setUp()
	{
		clientThread = mock(ClientThread.class);
		when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
			Callable<?> callable = inv.getArgument(0);
			onClientThread.set(true);
			try
			{
				return Optional.ofNullable(callable.call());
			}
			finally
			{
				onClientThread.set(false);
			}
		});

		cluePlugin = mock(ClueScrollPlugin.class);
		PluginManager pluginManager = mock(PluginManager.class);
		when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(cluePlugin));

		projectx = Mockito.mockStatic(ProjectX.class);
		projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
		projectx.when(ProjectX::getPluginManager).thenReturn(pluginManager);

		panel = mock(ShortestPathPanel.class, Mockito.CALLS_REAL_METHODS);
	}

	@After
	public void tearDown()
	{
		projectx.close();
	}

	@Test
	public void resolvesSingleLocationOnClientThread()
	{
		ClueScroll clue = mock(ClueScroll.class, withSettings().extraInterfaces(LocationClueScroll.class));
		when(((LocationClueScroll) clue).getLocation(cluePlugin)).thenAnswer(inv -> {
			if (!onClientThread.get())
			{
				throw new IllegalStateException("must be called on client thread");
			}
			return LOCATION;
		});
		when(cluePlugin.getClue()).thenReturn(clue);

		assertEquals(LOCATION, panel.getCurrentClueLocation());
	}

	@Test
	public void resolvesMultipleLocationsOnClientThread()
	{
		ClueScroll clue = mock(ClueScroll.class, withSettings().extraInterfaces(LocationsClueScroll.class));
		when(((LocationsClueScroll) clue).getLocations(cluePlugin)).thenAnswer(inv -> {
			if (!onClientThread.get())
			{
				throw new IllegalStateException("must be called on client thread");
			}
			return new WorldPoint[]{LOCATION};
		});
		when(cluePlugin.getClue()).thenReturn(clue);

		assertEquals(LOCATION, panel.getCurrentClueLocation());
	}

	@Test
	public void returnsNullWhenClientThreadReturnsNothing()
	{
		ClueScroll clue = mock(ClueScroll.class, withSettings().extraInterfaces(LocationClueScroll.class));
		when(cluePlugin.getClue()).thenReturn(clue);
		doReturn(Optional.empty()).when(clientThread).runOnClientThreadOptional(any());

		assertNull(panel.getCurrentClueLocation());
	}

	@Test
	public void returnsNullWithoutActiveClue()
	{
		when(cluePlugin.getClue()).thenReturn(null);

		assertNull(panel.getCurrentClueLocation());
	}
}
