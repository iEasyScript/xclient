package net.runelite.client.plugins.projectx.inventorysetups.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsCache;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsPanelViewID;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsSection;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsSortingID;
import net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsConfig;
import net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySetupsPluginPanelTest
{
	private List<InventorySetup> previousSetups;
	private MInventorySetupsPlugin plugin;

	@Before
	public void setUp()
	{
		previousSetups = MInventorySetupsPlugin.inventorySetups;
		MInventorySetupsPlugin.inventorySetups = new ArrayList<>();

		plugin = mock(MInventorySetupsPlugin.class);
		final MInventorySetupsConfig config = mock(MInventorySetupsConfig.class);
		when(plugin.getConfig()).thenReturn(config);
		when(plugin.getCache()).thenReturn(new InventorySetupsCache());
		when(plugin.getClientThread()).thenReturn(mock(ClientThread.class));
		when(plugin.getSavedVersionString()).thenReturn("");
		when(plugin.getCurrentVersionString()).thenReturn("");
		when(config.sectionMode()).thenReturn(true);
		when(config.sortingMode()).thenReturn(InventorySetupsSortingID.DEFAULT);
		when(config.panelView()).thenReturn(InventorySetupsPanelViewID.STANDARD);
	}

	@After
	public void tearDown()
	{
		MInventorySetupsPlugin.inventorySetups = previousSetups;
	}

	@Test
	public void redrawSkipsSectionMissingFromCache() throws Exception
	{
		when(plugin.getSections()).thenReturn(Collections.singletonList(new InventorySetupsSection("Loading")));
		redraw();
	}

	@Test
	public void redrawSkipsSetupMissingFromCache() throws Exception
	{
		final InventorySetup setup = mock(InventorySetup.class);
		when(setup.getName()).thenReturn("Loading");
		MInventorySetupsPlugin.inventorySetups.add(setup);
		when(plugin.getSections()).thenReturn(Collections.emptyList());
		redraw();
	}

	private void redraw() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			final InventorySetupsPluginPanel panel = new InventorySetupsPluginPanel(plugin, mock(ItemManager.class));
			panel.redrawOverviewPanel(false);
		});
	}
}
