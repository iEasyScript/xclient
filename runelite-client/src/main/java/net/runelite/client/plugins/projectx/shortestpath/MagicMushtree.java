package net.runelite.client.plugins.projectx.shortestpath;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * Handles the Magic Mushtree (Mycelium Transportation System) on Fossil Island.
 * The mushtree network connects four locations:
 * - House on the Hill
 * - Verdant Valley
 * - Sticky Swamp
 * - Mushroom Meadow
 */
@Getter
@RequiredArgsConstructor
public enum MagicMushtree {
    HOUSE_ON_THE_HILL("House on the Hill", new WorldPoint(3764, 3879, 1)),
    VERDANT_VALLEY("Verdant Valley", new WorldPoint(3760, 3758, 0)),
    STICKY_SWAMP("Sticky Swamp", new WorldPoint(3676, 3755, 0)),
    MUSHROOM_MEADOW("Mushroom Meadow", new WorldPoint(3676, 3871, 0));

    private final String destinationName;
    private final WorldPoint destination;

    // Object IDs for the Magic Mushtrees
    public static final int MUSHTREE_HOUSE_ON_HILL = 30920;
    public static final int MUSHTREE_OTHER = 30924;

    private static final int OFFSET = 10;

    /**
     * Checks if the given object ID is a Magic Mushtree.
     */
    public static boolean isMagicMushtree(int objectId) {
        return objectId == MUSHTREE_HOUSE_ON_HILL || objectId == MUSHTREE_OTHER;
    }

    /**
     * Checks if the given TileObject is a Magic Mushtree.
     */
    public static boolean isMagicMushtree(TileObject tileObject) {
        return tileObject != null && isMagicMushtree(tileObject.getId());
    }

    /**
     * Handles the Magic Mushtree transport after the initial "Use" interaction.
     * Waits for the menu to appear, then clicks the appropriate destination.
     *
     * @param transport The transport containing the destination
     * @return true if the transport was handled successfully
     */
    public static boolean handleTransport(Transport transport) {
        WorldPoint dest = transport.getDestination();
        MagicMushtree destination = getByDestination(dest);

        if (destination == null) {
            return false;
        }

        // Wait for the mushtree menu widget to appear
        if (!sleepUntil(() -> Rs2Widget.hasWidget("Mycelium"), 5000)) {
            return false;
        }

        // Click the destination option
        if (!Rs2Widget.clickWidget(destination.getDestinationName())) {
            return false;
        }

        // Wait until we arrive at destination
        sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(dest) < OFFSET, 10000);
        return true;
    }

    /**
     * Gets the MagicMushtree enum by destination WorldPoint.
     */
    public static MagicMushtree getByDestination(WorldPoint destination) {
        if (destination == null) return null;

        for (MagicMushtree mushtree : values()) {
            WorldPoint dest = mushtree.getDestination();
            if (dest.equals(destination)) {
                return mushtree;
            }
            // Also match by X and Y only (ignore plane differences in destination matching)
            if (dest.getX() == destination.getX() && dest.getY() == destination.getY()) {
                return mushtree;
            }
        }
        return null;
    }
}
