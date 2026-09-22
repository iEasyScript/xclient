package net.runelite.client.plugins.projectx.util.walker.door.model;

import net.runelite.api.coords.WorldPoint;

public final class DoorEdge {
    private final WorldPoint from;
    private final WorldPoint to;

    public DoorEdge(WorldPoint from, WorldPoint to) {
        this.from = from;
        this.to = to;
    }

    public String normalizedKey() {
        String a = compact(from);
        String b = compact(to);
        return a.compareTo(b) <= 0 ? a + "<->" + b : b + "<->" + a;
    }

    private static String compact(WorldPoint wp) {
        if (wp == null) {
            return "?";
        }
        return wp.getX() + "," + wp.getY() + ",p" + wp.getPlane();
    }
}
