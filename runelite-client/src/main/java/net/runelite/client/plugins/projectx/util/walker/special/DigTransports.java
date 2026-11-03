package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.projectx.shortestpath.Transport;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * "Dig here" transports: stand on the origin tile and dig with a spade (the Giant Mole hill in
 * Falador Park, the Brine Rat Cavern plateau). Ported from the DAX Web Walker (Apache-2.0, see
 * shortestpath/THIRD_PARTY_DAX.txt), NavigationSpecialCase GIANT_MOLE_HOLE and BRINE_RAT_CAVE_TREE.
 *
 * <p>In transports.tsv such a row is {@code Dig;Spade;0} with the spade in the Item IDs column, so the
 * pathfinder only plans it for a player who has a spade. There is no object to click, which is why
 * the walker used to stop at {@code objectId <= 0}.
 */
@Slf4j
public final class DigTransports {

    /** Farthest the player may be from the dig tile for the walker to walk over and dig. */
    static final int MAX_APPROACH_TILES = 12;
    static final int APPROACH_WAIT_MS = 7_000;
    static final int LANDING_WAIT_MS = 8_000;

    private DigTransports() {
    }

    /** True for a dig row: action "Dig" and no object. Pure. */
    public static boolean isDig(Transport transport) {
        return transport != null
                && transport.getObjectId() <= 0
                && "Dig".equalsIgnoreCase(transport.getAction());
    }

    /**
     * Walks onto the origin tile and digs. Script thread only.
     *
     * @return true once the player is near the destination
     */
    public static boolean dig(Transport transport) {
        WorldPoint origin = transport.getOrigin();
        WorldPoint destination = transport.getDestination();
        WorldPoint here = Rs2Player.getWorldLocation();
        if (origin == null || destination == null || here == null) {
            return false;
        }
        if (here.getPlane() != origin.getPlane() || here.distanceTo2D(origin) > MAX_APPROACH_TILES) {
            // Still walking the path towards it; the dig is attempted once we are close.
            return false;
        }
        if (!Rs2Inventory.hasItem(ItemID.SPADE)) {
            log.warn("[Walker] dig transport at {} needs a spade in the inventory", origin);
            return false;
        }
        if (!here.equals(origin)) {
            if (!Rs2Walker.walkFastCanvas(origin) && !Rs2Walker.walkMiniMap(origin)) {
                log.warn("[Walker] dig transport: could not click the dig tile {}", origin);
                return false;
            }
            if (!sleepUntil(() -> origin.equals(Rs2Player.getWorldLocation()), APPROACH_WAIT_MS)) {
                log.warn("[Walker] dig transport: did not reach the dig tile {} (at {})", origin, Rs2Player.getWorldLocation());
                return false;
            }
        }
        if (!Rs2Inventory.interact(ItemID.SPADE, "Dig")) {
            log.warn("[Walker] dig transport: spade Dig failed at {}", origin);
            return false;
        }
        boolean landed = sleepUntil(() -> {
            WorldPoint now = Rs2Player.getWorldLocation();
            return now != null && now.getPlane() == destination.getPlane() && now.distanceTo2D(destination) <= 5;
        }, LANDING_WAIT_MS);
        if (!landed) {
            log.warn("[Walker] dig at {} did not land near {} (at {})", origin, destination, Rs2Player.getWorldLocation());
        }
        return landed;
    }
}
