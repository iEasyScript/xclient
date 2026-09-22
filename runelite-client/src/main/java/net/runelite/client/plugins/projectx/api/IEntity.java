package net.runelite.client.plugins.projectx.api;

import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.reachable.Rs2Reachable;

public interface IEntity {
    int getId();
    String getName();
    WorldPoint getWorldLocation();
    LocalPoint getLocalLocation();
    WorldView getWorldView();
    boolean click();
    boolean click(String action);
    default boolean isReachable() {
        return Rs2Reachable.isReachable(getWorldLocation());
    }
}
