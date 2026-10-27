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

    /**
     * Clicks this entity with no particular action (the left-click option). Same as {@link #click()}.
     *
     * @return true if the click was sent
     */
    default boolean interact() {
        return click();
    }

    /**
     * Clicks this entity with the given menu action, for example {@code rock.interact("Mine")}.
     * Same as {@link #click(String)}.
     *
     * @param action the menu option to pick, such as "Mine", "Attack" or "Take"
     * @return true if the click was sent
     */
    default boolean interact(String action) {
        return click(action);
    }
    default boolean isReachable() {
        return Rs2Reachable.isReachable(getWorldLocation());
    }
}
