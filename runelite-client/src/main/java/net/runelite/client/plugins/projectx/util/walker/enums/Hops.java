package net.runelite.client.plugins.projectx.util.walker.enums;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public enum Hops {
    NONE("None"),
    ALDARIN("Aldarin", new WorldPoint(1365, 2941, 0)),
    LUMBRIDGE("Lumbridge", new WorldPoint(3231, 3318, 0)),
    SEERS_VILLAGE("Seers' Village", new WorldPoint(2670, 3522, 0)),
    YANILLE("Yanille", new WorldPoint(2578, 3102, 0));

    private final String name;
    private WorldPoint worldPoint;

    Hops(String name, WorldPoint worldPoint) {
        this.name = name;
        this.worldPoint = worldPoint;
    }

    Hops(String name) {
        this.name = name;
    }
}