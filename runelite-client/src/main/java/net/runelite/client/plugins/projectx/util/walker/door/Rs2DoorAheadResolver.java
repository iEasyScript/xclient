package net.runelite.client.plugins.projectx.util.walker.door;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;

import java.util.ArrayList;
import java.util.List;

public final class Rs2DoorAheadResolver {
    private Rs2DoorAheadResolver() {
    }

    public static List<WorldPoint> buildSegmentProbes(WorldPoint fromWp, WorldPoint toWp, WorldPoint doorWp) {
        if (fromWp == null || toWp == null || doorWp == null) {
            return List.of();
        }
        List<WorldPoint> probes = new ArrayList<>();
        probes.add(doorWp);

        boolean diagonal = Math.abs(fromWp.getX() - toWp.getX()) > 0
                && Math.abs(fromWp.getY() - toWp.getY()) > 0;
        if (diagonal) {
            probes.add(new WorldPoint(toWp.getX(), fromWp.getY(), doorWp.getPlane()));
            probes.add(new WorldPoint(fromWp.getX(), toWp.getY(), doorWp.getPlane()));
        }
        return probes;
    }

    public static boolean isPathEdgeBlocked(WorldPoint from, WorldPoint to) {
        if (from == null || to == null) {
            return false;
        }
        return !Rs2Tile.isTileReachable(to) || !hasLineOfSightBetween(from, to);
    }

    private static boolean hasLineOfSightBetween(WorldPoint a, WorldPoint b) {
        if (a == null || b == null) {
            return false;
        }
        WorldPoint from = a;
        WorldPoint to = b;
        return ProjectX.getClientThread().runOnClientThreadOptional(() ->
                from.toWorldArea().hasLineOfSightTo(
                        ProjectX.getClient().getTopLevelWorldView(),
                        to.toWorldArea()))
                .orElse(false);
    }
}
