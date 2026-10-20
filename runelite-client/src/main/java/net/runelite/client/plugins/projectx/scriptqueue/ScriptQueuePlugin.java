package net.runelite.client.plugins.projectx.scriptqueue;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
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
 * Script Queue: named plans of scripts run one after another, each until the first of
 * its conditions (a time, a level, experience, an item count, a time of day) or until
 * it stops by itself. Lives in its own sidebar tab.
 *
 * <p>Other features can follow it on the event bus ({@link ScriptQueueStepStarted},
 * {@link ScriptQueueStepEnded}, {@link ScriptQueueFinished}) and can check each step
 * before it starts ({@link #setPreStartCheck}).
 */
@PluginDescriptor(
    name = PluginDescriptor.Default + "Script Queue",
    description = "Run plans of scripts one after another: each for a time, to a level, xp or item goal, a clock time, or until it stops by itself.",
    tags = {"queue", "scheduler", "chain", "plan", "account", "projectx"},
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
    @Inject
    private EventBus eventBus;

    private ScriptQueuePanel panel;
    private NavigationButton navButton;

    /** The live tab, for {@link #open()}. Null while the plugin is off. */
    private static volatile ScriptQueuePlugin instance;

    /** See {@link #setPreStartCheck}. */
    private static volatile ScriptQueuePreStartCheck preStartCheck = ScriptQueuePreStartCheck.ALWAYS_OK;

    /**
     * Installs the check the queue makes before starting each step's script. A step that
     * fails it is handled as a script that failed to start. Takes effect from the next
     * time the queue is started. Null puts back the default, which lets every step start.
     */
    public static void setPreStartCheck(ScriptQueuePreStartCheck check)
    {
        preStartCheck = check == null ? ScriptQueuePreStartCheck.ALWAYS_OK : check;
    }

    static ScriptQueuePreStartCheck getPreStartCheck()
    {
        return preStartCheck;
    }

    /**
     * Starts a plan, as the Start queue button would: for website control. Not on
     * the Swing thread; waits up to five seconds for it.
     *
     * @param plan the plan's name, or null or blank for the one showing
     * @return null once it has started; otherwise why not, in plain words
     */
    public static String startPlan(String plan)
    {
        return onPanel(p -> p.startFromOutside(plan));
    }

    /**
     * Stops the queue, as the Stop queue button would: for website control. Not on
     * the Swing thread.
     *
     * @return null once it has been asked to stop; otherwise why not
     */
    public static String stopQueue()
    {
        return onPanel(ScriptQueuePanel::stopFromOutside);
    }

    private static String onPanel(java.util.function.Function<ScriptQueuePanel, String> action)
    {
        ScriptQueuePlugin live = instance;
        ScriptQueuePanel p = live == null ? null : live.panel;
        if (p == null)
        {
            return "the Script Queue is turned off in this client";
        }
        java.util.concurrent.FutureTask<String> task = new java.util.concurrent.FutureTask<>(() -> action.apply(p));
        SwingUtilities.invokeLater(task);
        try
        {
            return task.get(5, java.util.concurrent.TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
        catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e)
        {
            return "the Script Queue didn't answer";
        }
    }

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
        panel = new ScriptQueuePanel(configManager, pluginManager, gson, eventBus);
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
