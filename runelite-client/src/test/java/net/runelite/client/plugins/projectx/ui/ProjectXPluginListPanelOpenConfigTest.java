package net.runelite.client.plugins.projectx.ui;

import javax.inject.Provider;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class ProjectXPluginListPanelOpenConfigTest {
    @Test
    @SuppressWarnings("unchecked")
    public void openingPluginWithoutConfigDescriptorDoesNotBuildConfigPanel() {
        Provider<ProjectXConfigPanel> configPanelProvider = mock(Provider.class);
        ProjectXPluginListPanel panel = new ProjectXPluginListPanel(
                mock(ConfigManager.class),
                mock(PluginManager.class),
                mock(ProjectXPluginManager.class),
                mock(ExternalPluginManager.class),
                mock(EventBus.class),
                configPanelProvider);

        panel.openConfigurationPanel(new ProjectXPluginConfigurationDescriptor(
                "No Config", "Plugin without config", new String[0], null, null));

        verifyNoInteractions(configPanelProvider);
    }
}
