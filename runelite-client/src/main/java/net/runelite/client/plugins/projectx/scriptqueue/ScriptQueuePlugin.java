package net.runelite.client.plugins.projectx.scriptqueue;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ExternalPluginsChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Script Queue: chain scripts, each running for a time, to a skill level, or until
 * it stops by itself. Lives in its own sidebar tab.
 */
@PluginDescriptor(
    name = PluginDescriptor.Default + "Script Queue",
    description = "Run scripts one after another: for a time, until a skill level, or until each stops by itself.",
    tags = {"queue", "scheduler", "chain", "projectx"},
    enabledByDefault = true
)
@Slf4j
public class ScriptQueuePlugin extends Plugin
{
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private ConfigManager configManager;
    @Inject
    private PluginManager pluginManager;
    @Inject
    private Gson gson;

    private ScriptQueuePanel panel;
    private NavigationButton navButton;

    /** The live tab, for {@link #open()}. Null while the plugin is off. */
    private static volatile ScriptQueuePlugin instance;

    /**
     * Opens the Script Queue tab, for buttons elsewhere (the Project X home tab).
     *
     * @return false if the Script Queue is turned off
     */
    public static boolean open()
    {
        ScriptQueuePlugin live = instance;
        if (live == null || live.navButton == null)
        {
            return false;
        }
        SwingUtilities.invokeLater(() -> live.clientToolbar.openPanel(live.navButton));
        return true;
    }

    @Override
    protected void startUp()
    {
        panel = new ScriptQueuePanel(configManager, pluginManager, gson);
        navButton = NavigationButton.builder()
            .tooltip("Script Queue")
            .icon(icon())
            .priority(1)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
        instance = this;
    }

    @Override
    protected void shutDown()
    {
        instance = null;
        if (panel != null)
        {
            panel.shutdown();
        }
        clientToolbar.removeNavigation(navButton);
        panel = null;
    }

    /** New or removed store scripts: the dropdown follows. */
    @Subscribe
    public void onExternalPluginsChanged(ExternalPluginsChanged event)
    {
        ScriptQueuePanel p = panel;
        if (p != null)
        {
            SwingUtilities.invokeLater(p::refreshScripts);
        }
    }

    /** A small list-with-play-arrow icon, drawn rather than shipped as a file. */
    private static BufferedImage icon()
    {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(net.runelite.client.plugins.projectx.ui.ProjectXTheme.GOLD);
        g.setStroke(new BasicStroke(2f));
        for (int y : new int[]{3, 8, 13})
        {
            g.drawLine(6, y, 15, y);
            g.fillRect(1, y - 1, 3, 3);
        }
        g.dispose();
        return img;
    }
}
