package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.SplitFlagMap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Grimstone Dungeon frost dragon platform (customer reports, AIO Fighter Plus, 2026-11-05).
 * The south "Uneven stone ledges" (60120) need 83 Agility but were a plain transport with no
 * level, so players below 83 were sent at a shortcut the game refused. The north "Large stone
 * ledges" (60119, no level) were missing, so a walk between the platform and the cave just
 * north of it had no way over the ledge and teleported out through the house instead.
 */
public class GrimstoneTransportRegressionTest {

    private static final WorldPoint SOUTH_LEDGE_BELOW = new WorldPoint(2902, 10454, 0);
    private static final WorldPoint SOUTH_LEDGE_ABOVE = new WorldPoint(2902, 10456, 0);
    private static final WorldPoint NORTH_LEDGE_BELOW = new WorldPoint(2896, 10480, 0);
    private static final WorldPoint NORTH_LEDGE_ABOVE = new WorldPoint(2896, 10478, 0);
    /** DLP fairy ring inside the dungeon. */
    private static final WorldPoint FAIRY_RING = new WorldPoint(2926, 10455, 0);
    /** The attack-area centre from the first report, on the platform. */
    private static final WorldPoint PLATFORM = new WorldPoint(2897, 10468, 0);

    private static SplitFlagMap collisionMap;
    private static HashMap<WorldPoint, Set<Transport>> transports;

    @BeforeClass
    public static void load() {
        collisionMap = SplitFlagMap.fromResources();
        assertNotNull("collision map should load", collisionMap);
        transports = Transport.loadAllFromResources();
    }

    @Test
    public void southLedgesAreAnAgilityShortcutNeeding83() {
        for (WorldPoint[] hop : new WorldPoint[][]{{SOUTH_LEDGE_BELOW, SOUTH_LEDGE_ABOVE}, {SOUTH_LEDGE_ABOVE, SOUTH_LEDGE_BELOW}}) {
            List<Transport> rows = between(hop[0], hop[1]);
            assertEquals("exactly one transport " + hop[0] + " -> " + hop[1] + ": " + rows, 1, rows.size());
            Transport ledge = rows.get(0);
            assertEquals(TransportType.AGILITY_SHORTCUT, ledge.getType());
            assertEquals(ObjectID.GRIMSTONE_SHORTCUT_SOUTH, ledge.getObjectId());
            assertEquals(83, ledge.getSkillLevels()[Skill.AGILITY.ordinal()]);
        }
    }

    @Test
    public void northLedgesNeedNoLevel() {
        for (WorldPoint[] hop : new WorldPoint[][]{{NORTH_LEDGE_BELOW, NORTH_LEDGE_ABOVE}, {NORTH_LEDGE_ABOVE, NORTH_LEDGE_BELOW}}) {
            List<Transport> rows = between(hop[0], hop[1]);
            assertEquals("exactly one transport " + hop[0] + " -> " + hop[1] + ": " + rows, 1, rows.size());
            Transport ledge = rows.get(0);
            assertEquals(TransportType.TRANSPORT, ledge.getType());
            assertEquals(ObjectID.GRIMSTONE_SHORTCUT_NORTH, ledge.getObjectId());
            for (int level : ledge.getSkillLevels()) {
                assertEquals("the large stone ledges need no skill", 0, level);
            }
        }
    }

    @Test
    public void withoutTheShortcutThePlatformIsReachedOverTheNorthLedges() {
        List<WorldPoint> path = route(FAIRY_RING, PLATFORM, EnumSet.of(TransportType.AGILITY_SHORTCUT));
        assertEquals("route should end on the platform", PLATFORM, path.get(path.size() - 1));
        assertTrue("route should climb the north ledges: " + path, hasStep(path, NORTH_LEDGE_BELOW, NORTH_LEDGE_ABOVE));
        assertFalse("route must not use the 83 Agility ledges: " + path, hasStep(path, SOUTH_LEDGE_BELOW, SOUTH_LEDGE_ABOVE));
    }

    @Test
    public void platformToTheCaveBelowIsAShortClimb() {
        // Second report: player on the platform, attack area just below the north ledges.
        WorldPoint onPlatform = new WorldPoint(2897, 10477, 0);
        WorldPoint below = new WorldPoint(2898, 10479, 0);
        List<WorldPoint> path = route(onPlatform, below, EnumSet.of(TransportType.AGILITY_SHORTCUT));
        assertEquals("route should end below the ledges", below, path.get(path.size() - 1));
        assertTrue("route should climb down the north ledges: " + path, hasStep(path, NORTH_LEDGE_ABOVE, NORTH_LEDGE_BELOW));
        assertTrue("a two-tile hop should stay short, was " + path.size() + " tiles", path.size() <= 10);
    }

    private static List<Transport> between(WorldPoint origin, WorldPoint destination) {
        return transports.getOrDefault(origin, Collections.emptySet()).stream()
                .filter(t -> destination.equals(t.getDestination()))
                .collect(Collectors.toList());
    }

    private static boolean hasStep(List<WorldPoint> path, WorldPoint from, WorldPoint to) {
        for (int i = 0; i + 1 < path.size(); i++) {
            if (path.get(i).equals(from) && path.get(i + 1).equals(to)) {
                return true;
            }
        }
        return false;
    }

    /** Runs the real pathfinder with every loaded transport except the given types. */
    private static List<WorldPoint> route(WorldPoint start, WorldPoint target, Set<TransportType> without) {
        PathfinderConfig config = new PathfinderConfig(collisionMap, transports, Collections.emptyList(), null, null);
        try {
            java.lang.reflect.Field cutoff = PathfinderConfig.class.getDeclaredField("calculationCutoffMillis");
            cutoff.setAccessible(true);
            cutoff.setLong(config, 10000);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        for (Map.Entry<WorldPoint, Set<Transport>> entry : transports.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Set<Transport> usable = new HashSet<>();
            for (Transport t : entry.getValue()) {
                if (!without.contains(t.getType())) {
                    usable.add(t);
                }
            }
            if (!usable.isEmpty()) {
                config.getTransports().put(entry.getKey(), usable);
                config.getTransportsPacked().put(WorldPointUtil.packWorldPoint(entry.getKey()), usable);
            }
        }
        Pathfinder pathfinder = new Pathfinder(config, start, target);
        pathfinder.run();
        assertTrue("pathfinder should complete", pathfinder.isDone());
        List<WorldPoint> path = pathfinder.getPath();
        assertFalse("path " + start + " -> " + target + " should not be empty", path.isEmpty());
        return path;
    }
}
