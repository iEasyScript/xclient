package net.runelite.client.plugins.projectx.util.grounditem;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
public class InteractModel {
    int id;
    WorldPoint location;
    String name;

    public InteractModel(int id, WorldPoint location, String name) {
        this.id = id;
        this.location = location;
        this.name = name;
    }
}
