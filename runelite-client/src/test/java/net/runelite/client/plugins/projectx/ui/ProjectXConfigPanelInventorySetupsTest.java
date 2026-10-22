package net.runelite.client.plugins.projectx.ui;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import org.junit.Test;

import javax.swing.JComboBox;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectXConfigPanelInventorySetupsTest {
    private static final String GROUP = "group";
    private static final String KEY = "setup";

    private final ConfigManager configManager = mock(ConfigManager.class);

    @Test
    public void nullSetupsReturnsEmptyBoxWithoutTouchingConfig() {
        JComboBox<InventorySetup> box = ProjectXConfigPanel.createInventorySetupsComboBox(configManager, GROUP, KEY, null);

        assertEquals(0, box.getItemCount());
        verify(configManager, never()).setConfiguration(anyString(), anyString(), anyString());
        verify(configManager, never()).setConfiguration(anyString(), anyString(), any(Object.class));
        verify(configManager, never()).sendConfig();
    }

    @Test
    public void emptySetupsClearsConfigWithoutSyncSave() {
        JComboBox<InventorySetup> box = ProjectXConfigPanel.createInventorySetupsComboBox(configManager, GROUP, KEY, Collections.emptyList());

        assertEquals(0, box.getItemCount());
        verify(configManager).setConfiguration(GROUP, KEY, "");
        verify(configManager, never()).sendConfig();
    }

    @Test
    public void missingStoredSetupDefaultsToFirstWithoutSyncSave() {
        InventorySetup first = setup("first");
        InventorySetup second = setup("second");

        JComboBox<InventorySetup> box = ProjectXConfigPanel.createInventorySetupsComboBox(configManager, GROUP, KEY, Arrays.asList(first, second));

        assertEquals(2, box.getItemCount());
        assertSame(first, box.getSelectedItem());
        verify(configManager).setConfiguration(GROUP, KEY, first);
        verify(configManager, never()).sendConfig();
    }

    @Test
    public void storedSetupIsSelectedByName() {
        InventorySetup first = setup("first");
        InventorySetup second = setup("second");
        InventorySetup stored = setup("second");
        when(configManager.getConfiguration(GROUP, KEY, InventorySetup.class)).thenReturn(stored);

        JComboBox<InventorySetup> box = ProjectXConfigPanel.createInventorySetupsComboBox(configManager, GROUP, KEY, Arrays.asList(first, second));

        assertSame(second, box.getSelectedItem());
        verify(configManager, never()).sendConfig();
    }

    private static InventorySetup setup(String name) {
        InventorySetup setup = mock(InventorySetup.class);
        when(setup.getName()).thenReturn(name);
        return setup;
    }
}
