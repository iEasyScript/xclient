package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.SplitFlagMap;
import net.runelite.client.plugins.projectx.util.walker.banking.Rs2WalkerBankingPlanner;
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
 * End-to-end checks for transport data imported from Skretzo/shortest-path with
 * tools/shortestpath_sync.py: the real loader and the real pathfinder must pick up the new routes,
 * and the requirement fixes that came with the import must hold.
 */
public class UpstreamTransportImportRegressionTest {

    private static final WorldPoint BRIMHAVEN_CHARTER = new WorldPoint(2760, 3238, 0);
    private static final WorldPoint PORT_ROBERTS_DOCK = new WorldPoint(1871, 3309, 0);
    private static final WorldPoint CASTLE_WARS_CANOE = new WorldPoint(2439, 3135, 0);
    private static final WorldPoint GNOME_STRONGHOLD_CANOE = new WorldPoint(2525, 3408, 0);
    /** River Lum stepping stones (52 Agility), new in the import. */
    private static final WorldPoint STEPPING_STONE_SOUTH = new WorldPoint(3191, 3339, 0);
    private static final WorldPoint STEPPING_STONE_NORTH = new WorldPoint(3193, 3346, 0);

    private static SplitFlagMap collisionMap;
    private static HashMap<WorldPoint, Set<Transport>> transports;

    @BeforeClass
    public static void load() {
        collisionMap = SplitFlagMap.fromResources();
        assertNotNull("collision map should load", collisionMap);
        transports = Transport.loadAllFromResources();
    }

    @Test
    public void charterReachesTheNewPortRobertsDock() {
        Transport charter = find(BRIMHAVEN_CHARTER, PORT_ROBERTS_DOCK, TransportType.CHARTER_SHIP);
        assertEquals("Port Roberts", charter.getDisplayInfo());
        assertEquals("the Port Roberts charter needs 50 Sailing", 50, charter.getSkillLevels()[Skill.SAILING.ordinal()]);
        assertEquals("Coins", charter.getCurrencyName());

        List<WorldPoint> path = route(BRIMHAVEN_CHARTER, PORT_ROBERTS_DOCK, EnumSet.noneOf(TransportType.class));
        assertEquals("route should end at the Port Roberts dock", PORT_ROBERTS_DOCK, path.get(path.size() - 1));
        assertTrue("route from Brimhaven to Port Roberts should take the new charter: " + path,
                hasStep(path, BRIMHAVEN_CHARTER, PORT_ROBERTS_DOCK));
    }

    @Test
    public void newCanoeStationRouteIsUsed() {
        Transport canoe = find(CASTLE_WARS_CANOE, new WorldPoint(2523, 3408, 0), TransportType.CANOE);
        assertEquals(57, canoe.getSkillLevels()[Skill.WOODCUTTING.ordinal()]);

        List<WorldPoint> path = route(CASTLE_WARS_CANOE, GNOME_STRONGHOLD_CANOE, EnumSet.noneOf(TransportType.class));
        WorldPoint end = path.get(path.size() - 1);
        assertTrue("route should reach the Gnome Stronghold canoe station, ended at " + end,
                end.distanceTo(GNOME_STRONGHOLD_CANOE) <= 2);
        Set<String> canoeHops = transports.values().stream().flatMap(Set::stream)
                .filter(t -> t.getType() == TransportType.CANOE && t.getOrigin() != null)
                .map(t -> t.getOrigin() + ">" + t.getDestination())
                .collect(Collectors.toSet());
        boolean usesCanoe = false;
        for (int i = 0; i + 1 < path.size(); i++) {
            usesCanoe |= canoeHops.contains(path.get(i) + ">" + path.get(i + 1));
        }
        assertTrue("a Castle Wars to Gnome Stronghold route should paddle a canoe: " + path, usesCanoe);
    }

    @Test
    public void newAgilityShortcutShortensTheRoute() {
        Transport stones = find(STEPPING_STONE_SOUTH, STEPPING_STONE_NORTH, TransportType.AGILITY_SHORTCUT);
        assertEquals(52, stones.getSkillLevels()[Skill.AGILITY.ordinal()]);

        List<WorldPoint> withShortcut = route(STEPPING_STONE_SOUTH, STEPPING_STONE_NORTH,
                EnumSet.noneOf(TransportType.class));
        assertTrue("the stepping stones should be used: " + withShortcut,
                hasStep(withShortcut, STEPPING_STONE_SOUTH, STEPPING_STONE_NORTH));

        List<WorldPoint> withoutShortcut = route(STEPPING_STONE_SOUTH, STEPPING_STONE_NORTH,
                EnumSet.of(TransportType.AGILITY_SHORTCUT));
        assertFalse("without agility shortcuts the route must not hop the river",
                hasStep(withoutShortcut, STEPPING_STONE_SOUTH, STEPPING_STONE_NORTH));
        assertTrue("the shortcut should save a long walk (" + withShortcut.size() + " vs "
                        + withoutShortcut.size() + " tiles)",
                withoutShortcut.size() > withShortcut.size() + 20);
    }

    /**
     * Upstream reclassified the Tirannwn dense forest from a plain transport to a 56 Agility
     * shortcut. Our copy in transports.tsv had no level, so it must not survive next to the new row.
     */
    @Test
    public void relocatedDenseForestCarriesItsAgilityLevel() {
        WorldPoint origin = new WorldPoint(2188, 3162, 0);
        WorldPoint destination = new WorldPoint(2188, 3165, 0);
        Set<Transport> atOrigin = transports.getOrDefault(origin, Collections.emptySet());
        List<Transport> forest = atOrigin.stream()
                .filter(t -> destination.equals(t.getDestination()))
                .collect(Collectors.toList());
        assertEquals("exactly one dense forest transport should remain: " + forest, 1, forest.size());
        assertEquals(TransportType.AGILITY_SHORTCUT, forest.get(0).getType());
        assertEquals(56, forest.get(0).getSkillLevels()[Skill.AGILITY.ordinal()]);
    }

    /**
     * Banked canoe routes used to list seven axes as seven ';' groups, so the planner withdrew an
     * arbitrary one of them. Now every axe is one "any of" group and exactly one axe is requested.
     */
    @Test
    public void bankedCanoeRouteRequestsExactlyOneAxe() {
        Transport canoe = find(new WorldPoint(3132, 3510, 0), new WorldPoint(3109, 3415, 0), TransportType.CANOE);
        assertEquals(1, canoe.getItemIdRequirements().size());
        Set<Integer> axes = canoe.getItemIdRequirements().iterator().next();
        assertTrue(axes.contains(ItemID.BRONZE_AXE));
        assertTrue(axes.contains(ItemID.DRAGON_AXE));

        Map<Integer, Integer> withdraw = Rs2WalkerBankingPlanner.getMissingTransportItemIdsWithQuantities(List.of(canoe));
        assertEquals("one axe should be requested: " + withdraw, 1, withdraw.size());
        Map.Entry<Integer, Integer> only = withdraw.entrySet().iterator().next();
        assertTrue("the requested item should be an axe: " + only.getKey(), axes.contains(only.getKey()));
        assertEquals(1, only.getValue().intValue());
    }

    private static Transport find(WorldPoint origin, WorldPoint destination, TransportType type) {
        return transports.getOrDefault(origin, Collections.emptySet()).stream()
                .filter(t -> t.getType() == type && destination.equals(t.getDestination()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + type + " transport " + origin + " -> " + destination));
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
