package net.runelite.client.plugins.projectx.externalplugins;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.slayer.SlayerPlugin;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Only scripts from the store are tracked. RuneLite's own plugins that share a store
 * script's name (Slayer, Pest Control, Discord, Daily Tasks...) were tracked too, so
 * every dashboard listed them with runs as long as the client had been open.
 */
public class ProjectXSessionTrackerTest
{
    @Test
    public void runeLitesOwnPluginsAreNotStoreScripts()
    {
        Plugin builtIn = new SlayerPlugin();
        assertFalse("RuneLite's Slayer plugin shares the store Slayer script's name, but is not it",
            ProjectXSessionTracker.isStoreScript(builtIn));
        assertFalse(ProjectXSessionTracker.isStoreScript(null));
    }
}
