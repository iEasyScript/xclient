package net.runelite.client.plugins.projectx.util.walker.door.model;

import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;

public final class DoorCandidate {
    private final TileObject object;
    private final WorldPoint probe;
    private final WorldPoint from;
    private final WorldPoint to;
    private final String action;
    private final String mode;

    public DoorCandidate(TileObject object, WorldPoint probe, WorldPoint from, WorldPoint to, String action, String mode) {
        this.object = object;
        this.probe = probe;
        this.from = from;
        this.to = to;
        this.action = action;
        this.mode = mode;
    }

    public TileObject object() {
        return object;
    }

    public WorldPoint probe() {
        return probe;
    }

    public WorldPoint from() {
        return from;
    }

    public WorldPoint to() {
        return to;
    }

    public String action() {
        return action;
    }

    public String mode() {
        return mode;
    }
}
