package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.Pathfinder;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.PathfinderConfig;
import net.runelite.client.plugins.projectx.shortestpath.pathfinder.SplitFlagMap;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * A transport claims its destination tile when it leaves the pending queue, not when it
 * joins it. Claiming on join let an expensive transport, queued first, shadow a cheaper
 * route to the same tile found a moment later (the slow home teleport beating a tablet
 * and POH portal to Lumbridge).
 */
public class TransportClaimOrderTest
{
    // Three walkable tiles far enough apart that walking between them never competes.
    private static final WorldPoint START = new WorldPoint(3222, 3218, 0);   // Lumbridge
    private static final WorldPoint MIDDLE = new WorldPoint(2964, 3378, 0);  // Falador
    private static final WorldPoint TARGET = new WorldPoint(3093, 3493, 0);  // Edgeville

    private static SplitFlagMap collisionMap;

    @BeforeClass
    public static void loadCollisionMap()
    {
        collisionMap = SplitFlagMap.fromResources();
    }

    @Test
    public void cheaperChainBeatsExpensiveTransportQueuedFirst() throws Exception
    {
        Transport direct = transport(START, TARGET, 30);
        Transport toMiddle = transport(START, MIDDLE, 2);
        Transport fromMiddle = transport(MIDDLE, TARGET, 3);

        PathfinderConfig config = configWith(Map.of(
            START, orderedSet(direct, toMiddle),
            MIDDLE, orderedSet(fromMiddle)));

        Pathfinder pathfinder = new Pathfinder(config, START, TARGET);
        pathfinder.run();

        List<WorldPoint> path = pathfinder.getPath();
        assertEquals("route must end at the target", TARGET, path.get(path.size() - 1));
        assertTrue("the 5-tick chain through " + MIDDLE + " must win over the 30-tick direct transport, path=" + path,
            path.contains(MIDDLE));
    }

    @Test
    public void expensiveTransportStillUsedWhenItIsTheOnlyWay() throws Exception
    {
        PathfinderConfig config = configWith(Map.of(START, orderedSet(transport(START, TARGET, 30))));

        Pathfinder pathfinder = new Pathfinder(config, START, TARGET);
        pathfinder.run();

        List<WorldPoint> path = pathfinder.getPath();
        assertEquals("the only transport must still be taken", TARGET, path.get(path.size() - 1));
    }

    private static Transport transport(WorldPoint origin, WorldPoint destination, int duration)
    {
        HashMap<String, String> fields = new HashMap<>();
        fields.put("Origin", origin.getX() + " " + origin.getY() + " " + origin.getPlane());
        fields.put("Destination", destination.getX() + " " + destination.getY() + " " + destination.getPlane());
        fields.put("Duration", String.valueOf(duration));
        return new Transport(fields, TransportType.TRANSPORT);
    }

    private static Set<Transport> orderedSet(Transport... transports)
    {
        Set<Transport> set = new LinkedHashSet<>();
        Collections.addAll(set, transports);
        return set;
    }

    @SuppressWarnings("unchecked")
    private static PathfinderConfig configWith(Map<WorldPoint, Set<Transport>> byOrigin) throws Exception
    {
        PathfinderConfig config = new PathfinderConfig(collisionMap, new HashMap<>(), Collections.<Restriction>emptyList(), null, null);
        Field cutoff = PathfinderConfig.class.getDeclaredField("calculationCutoffMillis");
        cutoff.setAccessible(true);
        cutoff.setLong(config, 10_000);

        Field transportsField = PathfinderConfig.class.getDeclaredField("transports");
        transportsField.setAccessible(true);
        Map<WorldPoint, Set<Transport>> transports = (Map<WorldPoint, Set<Transport>>) transportsField.get(config);
        Field packedField = PathfinderConfig.class.getDeclaredField("transportsPacked");
        packedField.setAccessible(true);
        PrimitiveIntHashMap<Set<Transport>> packed = (PrimitiveIntHashMap<Set<Transport>>) packedField.get(config);
        for (Map.Entry<WorldPoint, Set<Transport>> e : byOrigin.entrySet())
        {
            transports.put(e.getKey(), e.getValue());
            packed.put(WorldPointUtil.packWorldPoint(e.getKey()), e.getValue());
        }
        return config;
    }
}
