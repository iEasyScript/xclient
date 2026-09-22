package net.runelite.client.plugins.projectx.util.walker.door;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.walker.door.model.DoorEdge;

import java.util.Map;

public final class Rs2DoorHandler {
    private Rs2DoorHandler() {
    }

    public static String doorAttemptKey(WorldPoint doorTile, WorldPoint fromWp, WorldPoint toWp) {
        if (fromWp != null && toWp != null) {
            return new DoorEdge(fromWp, toWp).normalizedKey();
        }
        return compactWorldPoint(doorTile) + "|" + compactWorldPoint(fromWp) + "->" + compactWorldPoint(toWp);
    }

    public static boolean shouldThrottleDoorAttempt(Map<String, Long> recentDoorAttemptByEdge,
                                                    long cooldownMs,
                                                    WorldPoint doorTile,
                                                    WorldPoint fromWp,
                                                    WorldPoint toWp) {
        String key = doorAttemptKey(doorTile, fromWp, toWp);
        long now = System.currentTimeMillis();
        recentDoorAttemptByEdge.entrySet().removeIf(entry -> now - entry.getValue() > cooldownMs);
        Long last = recentDoorAttemptByEdge.get(key);
        return last != null && now - last < cooldownMs;
    }

    public static void markDoorAttempt(Map<String, Long> recentDoorAttemptByEdge,
                                       WorldPoint doorTile,
                                       WorldPoint fromWp,
                                       WorldPoint toWp) {
        recentDoorAttemptByEdge.put(doorAttemptKey(doorTile, fromWp, toWp), System.currentTimeMillis());
    }

    public static void markStationaryDoorOpened(Map<WorldPoint, Long> recentlyOpenedStationaryDoors, WorldPoint doorTile) {
        if (doorTile != null) {
            recentlyOpenedStationaryDoors.put(doorTile, System.currentTimeMillis());
        }
    }

    public static boolean recentlyOpenedStationaryDoorOnSegment(Map<WorldPoint, Long> recentlyOpenedStationaryDoors,
                                                                long suppressMs,
                                                                WorldPoint fromWp,
                                                                WorldPoint toWp) {
        if (fromWp == null || toWp == null) {
            return false;
        }
        final int segmentDoorSuppressDist = 2;
        long now = System.currentTimeMillis();
        recentlyOpenedStationaryDoors.entrySet().removeIf(entry -> now - entry.getValue() > suppressMs);
        return recentlyOpenedStationaryDoors.keySet().stream()
                .anyMatch(door -> door != null
                        && door.getPlane() == fromWp.getPlane()
                        && (door.distanceTo2D(fromWp) <= segmentDoorSuppressDist || door.distanceTo2D(toWp) <= segmentDoorSuppressDist));
    }

    public static boolean shouldThrottleGlobalDoorInteraction(long nextDoorInteractionAllowedAtMs) {
        return System.currentTimeMillis() < nextDoorInteractionAllowedAtMs;
    }

    public static long markGlobalDoorInteractionCooldown(long cooldownMs) {
        return System.currentTimeMillis() + cooldownMs;
    }

    private static String compactWorldPoint(WorldPoint wp) {
        if (wp == null) {
            return "?";
        }
        return wp.getX() + "," + wp.getY() + ",p" + wp.getPlane();
    }
}
