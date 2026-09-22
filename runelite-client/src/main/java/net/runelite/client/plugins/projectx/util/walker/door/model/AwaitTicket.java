package net.runelite.client.plugins.projectx.util.walker.door.model;

import net.runelite.api.coords.WorldPoint;

public final class AwaitTicket {
    private final long startedAtMs;
    private final WorldPoint beforePosition;

    public AwaitTicket(long startedAtMs, WorldPoint beforePosition) {
        this.startedAtMs = startedAtMs;
        this.beforePosition = beforePosition;
    }

    public long startedAtMs() {
        return startedAtMs;
    }

    public WorldPoint beforePosition() {
        return beforePosition;
    }
}
