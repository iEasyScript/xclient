package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.PathfinderConfig;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Planted spirit trees are only routed through once this account is known to have them
 * grown, and Respawn Teleport only lands at the respawn point that is actually active.
 */
public class SpiritTreeAndRespawnGateTest
{
    // ---- SpiritTreePatchState

    @Test
    public void onlyAGrownTreeIsTravelable()
    {
        assertTrue(SpiritTreePatchState.travelable(20));
        for (int v : new int[]{0, 8, 19, 21, 32, 44, 63})
        {
            assertFalse("value " + v, SpiritTreePatchState.travelable(v));
        }
    }

    @Test
    public void nothingObservedMeansUnknown()
    {
        SpiritTreePatchState state = new SpiritTreePatchState();
        assertNull(state.getTravelableTreesOrNull());
        assertTrue(state.applyVarbitSample("Port Sarim", 20));
        assertEquals(Set.of("Port Sarim"), state.getTravelableTreesOrNull());
        assertTrue("a dead tree evicts the earlier observation", state.applyVarbitSample("Port Sarim", 35));
        assertEquals(Set.of(), state.getTravelableTreesOrNull());
    }

    @Test
    public void regionMustSettleForATickBeforeSampling()
    {
        SpiritTreePatchState state = new SpiritTreePatchState();
        state.notePlayerRegion(12082, 100);
        assertFalse("entry tick may still hold the previous region's varbit", state.isRegionSettled(12082));
        state.notePlayerRegion(12082, 101);
        assertTrue(state.isRegionSettled(12082));
        state.notePlayerRegion(12082, 103);
        assertFalse("a skipped tick restarts the wait", state.isRegionSettled(12082));
    }

    @Test
    public void travelMenuListsPlantedTreesAndGreysUnusableOnes()
    {
        SpiritTreePatchState state = new SpiritTreePatchState();
        Widget[] rows = {
            row("<col=735a28>1</col>: Tree Gnome Village"),
            row("<col=735a28>2</col>: Gnome Stronghold"),
            row("<col=735a28>6</col>: Port Sarim"),
            row("<col=735a28>7</col>: <col=5f5f5f>Hosidius</col>"),
        };
        assertTrue(state.applyMenu(rows, false));
        assertEquals("Hosidius is greyed out, so planted but not usable", Set.of("Port Sarim"), state.getTravelableTreesOrNull());
    }

    @Test
    public void aMenuThatIsNotTheSpiritTreeMenuIsIgnored()
    {
        SpiritTreePatchState state = new SpiritTreePatchState();
        Widget[] rows = {row("<col=735a28>1</col>: Something else"), row("<col=735a28>2</col>: Port Sarim")};
        assertFalse(state.applyMenu(rows, false));
        assertNull(state.getTravelableTreesOrNull());
    }

    @Test
    public void storedValuesParseStrictly()
    {
        assertEquals(Integer.valueOf(20), SpiritTreePatchState.parseStored("20:1791300000"));
        assertNull(SpiritTreePatchState.parseStored("20"));
        assertNull(SpiritTreePatchState.parseStored("20:"));
        assertNull(SpiritTreePatchState.parseStored("x:1"));
        assertNull(SpiritTreePatchState.parseStored("20:1:2"));
    }

    // ---- PathfinderConfig gates

    private static final WorldPoint GNOME_VILLAGE_TREE = new WorldPoint(2542, 3170, 0);
    private static final WorldPoint PORT_SARIM_TREE = new WorldPoint(3058, 3257, 0);

    @Test
    public void plantedTreeIsNotUsedUntilKnownToBeGrown() throws Exception
    {
        PathfinderConfig config = config();
        setBoolean(config, "useSpiritTreePortSarim", true);
        Transport toPortSarim = transport(GNOME_VILLAGE_TREE, PORT_SARIM_TREE, TransportType.SPIRIT_TREE, null);

        assertFalse("nothing observed yet", spiritTreeRouteEnabled(config, toPortSarim));

        config.availableSpiritTrees = Set.of("Hosidius");
        assertFalse("seen, but not at Port Sarim", spiritTreeRouteEnabled(config, toPortSarim));

        config.availableSpiritTrees = Set.of("Port Sarim");
        assertTrue(spiritTreeRouteEnabled(config, toPortSarim));

        setBoolean(config, "useSpiritTreePortSarim", false);
        assertFalse("the setting can still turn it off", spiritTreeRouteEnabled(config, toPortSarim));
    }

    @Test
    public void respawnTeleportLandsOnlyAtTheActiveRespawn() throws Exception
    {
        PathfinderConfig config = config();
        Transport lumbridge = transport(null, new WorldPoint(3221, 3218, 0), TransportType.TELEPORTATION_SPELL, "Respawn Teleport");
        Transport prifddinas = transport(null, new WorldPoint(3265, 6077, 0), TransportType.TELEPORTATION_SPELL, "Respawn Teleport");
        Transport falador = transport(null, new WorldPoint(2964, 3378, 0), TransportType.TELEPORTATION_SPELL, "Respawn Teleport");

        assertTrue(respawnGate(config, lumbridge));
        assertFalse(respawnGate(config, prifddinas));
        assertTrue("others are gated by their own varbit, not here", respawnGate(config, falador));

        setBoolean(config, "respawnPrifddinas", true);
        assertFalse(respawnGate(config, lumbridge));
        assertTrue(respawnGate(config, prifddinas));
    }

    // ---- helpers

    private static Widget row(String text)
    {
        Widget w = mock(Widget.class);
        when(w.getText()).thenReturn(text);
        return w;
    }

    private static PathfinderConfig config()
    {
        return new PathfinderConfig(null, new HashMap<>(), Collections.<Restriction>emptyList(), null, null);
    }

    private static Transport transport(WorldPoint origin, WorldPoint destination, TransportType type, String displayInfo)
    {
        HashMap<String, String> fields = new HashMap<>();
        if (origin != null)
        {
            fields.put("Origin", origin.getX() + " " + origin.getY() + " " + origin.getPlane());
        }
        fields.put("Destination", destination.getX() + " " + destination.getY() + " " + destination.getPlane());
        if (displayInfo != null)
        {
            fields.put("Display info", displayInfo);
        }
        return new Transport(fields, type);
    }

    private static void setBoolean(PathfinderConfig config, String name, boolean value) throws Exception
    {
        Field f = PathfinderConfig.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(config, value);
    }

    private static boolean spiritTreeRouteEnabled(PathfinderConfig config, Transport t) throws Exception
    {
        Method m = PathfinderConfig.class.getDeclaredMethod("isSpiritTreeRouteEnabled", Transport.class);
        m.setAccessible(true);
        return (boolean) m.invoke(config, t);
    }

    private static boolean respawnGate(PathfinderConfig config, Transport t) throws Exception
    {
        Method m = PathfinderConfig.class.getDeclaredMethod("checkRespawnGate", Transport.class);
        m.setAccessible(true);
        return (boolean) m.invoke(config, t);
    }
}
