package net.runelite.client.plugins.projectx.util.walker;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.shortestpath.Restriction;
import net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.PathfinderConfig;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A cancelled search never reports done. The walker used to wait on one forever, so a
 * script asking to walk to the bank every two seconds never moved.
 */
public class StalePathfinderTest
{
    @After
    public void clearStatics()
    {
        ShortestPathPlugin.pathfinderFuture = null;
    }

    private static Pathfinder search()
    {
        PathfinderConfig config = new PathfinderConfig(null, new HashMap<>(), Collections.<Restriction>emptyList(), null, null);
        return new Pathfinder(config, new WorldPoint(3222, 3218, 0), new WorldPoint(3164, 3485, 0));
    }

    @Test
    public void aCancelledSearchIsStale()
    {
        Pathfinder pf = search();
        assertFalse("a fresh, queued search is worth waiting for", Rs2Walker.isStalePathfinder(pf));
        pf.cancel();
        assertTrue(pf.isCancelled());
        assertTrue("cancelled: it will never be done", Rs2Walker.isStalePathfinder(pf));
    }

    @Test
    public void noSearchIsNeverStale()
    {
        assertFalse(Rs2Walker.isStalePathfinder(null));
    }
}
