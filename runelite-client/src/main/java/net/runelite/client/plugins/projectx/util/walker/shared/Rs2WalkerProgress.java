package net.runelite.client.plugins.projectx.util.walker.shared;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.shortestpath.WorldPointUtil;

public final class Rs2WalkerProgress {
    private Rs2WalkerProgress() {
    }

    public static boolean hasMovementOrProgress(WorldPoint before, WorldPoint now, WorldPoint expectedDestination, WorldPoint target) {
        if (before == null || now == null) {
            return false;
        }
        if (!now.equals(before)) {
            return true;
        }
        if (isWithinChebyshev(now, expectedDestination, 1)) {
            return true;
        }
        return madeProgressToward(before, now, target);
    }

    public static boolean madeProgressToward(WorldPoint before, WorldPoint now, WorldPoint target) {
        if (before == null || now == null || target == null) {
            return false;
        }
        // Band-aware distance: plain distanceTo2D ignores the underground Y-offset, so a sideways
        // hop within an underground region can read as "closer" to a surface goal. Using the same
        // metric the pathfinder uses keeps this honest (e.g. re-entering Mor Ul Rek is not progress).
        return WorldPointUtil.undergroundAwareDistance(now, target)
                < WorldPointUtil.undergroundAwareDistance(before, target);
    }

    public static boolean isWithinChebyshev(WorldPoint from, WorldPoint to, int maxInclusive) {
        if (from == null || to == null) {
            return false;
        }
        if (from.getPlane() != to.getPlane()) {
            return false;
        }
        return from.distanceTo2D(to) <= maxInclusive;
    }
}
