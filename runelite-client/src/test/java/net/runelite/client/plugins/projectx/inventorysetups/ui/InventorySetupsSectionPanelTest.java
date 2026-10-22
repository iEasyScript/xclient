package net.runelite.client.plugins.projectx.inventorysetups.ui;

import java.util.Collections;
import java.util.Set;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsCache;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsPanelViewID;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsSection;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsSortingID;
import net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsConfig;
import net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InventorySetupsSectionPanelTest
{
	private static final String MISSING_SETUP = "Missing";

	private MInventorySetupsPlugin plugin;
	private MInventorySetupsConfig config;
	private InventorySetupsSection section;

	@Before
	public void setUp()
	{
		plugin = mock(MInventorySetupsPlugin.class);
		config = mock(MInventorySetupsConfig.class);
		when(plugin.getConfig()).thenReturn(config);
		when(plugin.getCache()).thenReturn(new InventorySetupsCache());
		when(config.sortingMode()).thenReturn(InventorySetupsSortingID.DEFAULT);

		section = new InventorySetupsSection("Unassigned");
		section.setMaximized(true);
		section.getSetups().add(MISSING_SETUP);
	}

	@Test
	public void standardViewSkipsSetupMissingFromCache()
	{
		when(config.panelView()).thenReturn(InventorySetupsPanelViewID.STANDARD);
		assertNotNull(buildPanel());
	}

	@Test
	public void iconViewSkipsSetupMissingFromCache()
	{
		when(config.panelView()).thenReturn(InventorySetupsPanelViewID.ICON);
		assertNotNull(buildPanel());
	}

	private InventorySetupsSectionPanel buildPanel()
	{
		final Set<String> names = Collections.singleton(MISSING_SETUP);
		return new InventorySetupsSectionPanel(plugin, mock(InventorySetupsPluginPanel.class), section, false, false, names, names, Collections.emptyList());
	}
}
