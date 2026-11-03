package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.shortestpath.Transport;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * Quetzal whistle charges and its "Last-destination" option. Ported from the DAX Web Walker
 * (Apache-2.0, see shortestpath/THIRD_PARTY_DAX.txt), navigation_utils/Quetzal.java.
 *
 * <ul>
 *     <li>Charges are read from the game's "...quetzal whistle has N charges remaining." message
 *     and kept per account in the RuneScape profile config. With a known 0 (and no infinite
 *     whistle) the pathfinder stops planning whistle teleports.</li>
 *     <li>When varbit {@code QUETZAL_LAST_DESTINATION} already names the target, "Last-destination"
 *     goes there in one click instead of opening the map.</li>
 * </ul>
 */
@Slf4j
public final class QuetzalWhistle {

    static final String CONFIG_GROUP = "projectxwalker";
    static final String CONFIG_KEY_CHARGES = "quetzalWhistleCharges";

    /** Whistles that hold charges; the perfected (i) whistle never runs out. */
    static final Set<Integer> CHARGED_WHISTLES = Set.of(
            ItemID.HG_QUETZALWHISTLE_BASIC,
            ItemID.HG_QUETZALWHISTLE_ENHANCED,
            ItemID.HG_QUETZALWHISTLE_PERFECTED);
    static final int INFINITE_WHISTLE = ItemID.HG_QUETZALWHISTLE_PERFECTED_INFINITE;

    private static final Pattern CHARGES_PATTERN = Pattern.compile(
            "^Your (?:basic |enhanced |perfected )?quetzal whistle has (\\d+) charges? remaining\\.?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NO_CHARGES_PATTERN = Pattern.compile(
            ".*quetzal whistle.*\\bno charges\\b.*", Pattern.CASE_INSENSITIVE);

    /**
     * Landing tile and {@code QUETZAL_LAST_DESTINATION} value of each whistle destination
     * (DAX Quetzal.Location). Values are DAX's, not checked against the game here; a landing that
     * misses switches the option off for the session ({@link #lastDestinationDisabled}).
     */
    private static final int[][] LAST_DESTINATIONS = {
            {1390, 2901, 9},   // Aldarin
            {1411, 3361, 13},  // Auburnvale
            {1446, 3108, 5},   // Cam Torum Entrance
            {1696, 3140, 1},   // Civitas illa Fortis
            {1670, 2934, 6},   // Colossal Wyrm Remains
            {1779, 3111, 8},   // Fortis Colosseum
            {1585, 3053, 4},   // Hunter Guild
            {1344, 3022, 14},  // Kastori
            {1700, 3035, 7},   // Outer Fortis
            {1510, 3222, 10},  // Quetzacalli Gorge
            {1614, 3300, 11},  // Salvager Overlook
            {1548, 2995, 3},   // Sunset Coast
            {1226, 3091, 12},  // Tal Teklan
            {1437, 3171, 2},   // The Teomat
    };
    /** How far a transport destination may sit from a DAX landing tile and still be that destination. */
    static final int LANDING_MATCH_TILES = 10;

    private static volatile boolean lastDestinationDisabled;

    private QuetzalWhistle() {
    }

    /** Charges in a "...has N charges remaining." message, or -1. "No charges" messages give 0. Pure. */
    static int chargesFromMessage(String message) {
        if (message == null) {
            return -1;
        }
        String clean = Rs2UiHelper.stripColTags(message).replace("<br>", " ").trim();
        Matcher m = CHARGES_PATTERN.matcher(clean);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return NO_CHARGES_PATTERN.matcher(clean).matches() ? 0 : -1;
    }

    /** Game-message hook (client thread; cheap). */
    public static void onGameMessage(String message) {
        int charges = chargesFromMessage(message);
        if (charges >= 0) {
            storeCharges(charges);
        }
    }

    private static void storeCharges(int charges) {
        ConfigManager cm = ProjectX.getConfigManager();
        if (cm == null) {
            return;
        }
        // A zero carries when it was seen: recharging may not print a count, so "empty" expires.
        cm.setRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY_CHARGES,
                charges == 0 ? "0:" + System.currentTimeMillis() : String.valueOf(charges));
        log.debug("[Walker] quetzal whistle charges now {}", charges);
    }

    /** How long a seen "0 charges" keeps whistle teleports out of planning. */
    static final long EMPTY_VERDICT_TTL_MS = 60L * 60L * 1000L;

    /** Last known charges, or -1 when unknown (never seen, or a zero older than the TTL). */
    public static int knownCharges() {
        ConfigManager cm = ProjectX.getConfigManager();
        if (cm == null) {
            return -1;
        }
        return parseStoredCharges(cm.getRSProfileConfiguration(CONFIG_GROUP, CONFIG_KEY_CHARGES), System.currentTimeMillis());
    }

    /** Parses the stored value ("N", or "0:millis" for an empty whistle). Pure. */
    static int parseStoredCharges(String stored, long now) {
        if (stored == null || stored.trim().isEmpty()) {
            return -1;
        }
        String v = stored.trim();
        try {
            int colon = v.indexOf(':');
            if (colon < 0) {
                return Integer.parseInt(v);
            }
            int charges = Integer.parseInt(v.substring(0, colon));
            long seenAt = Long.parseLong(v.substring(colon + 1));
            return now - seenAt <= EMPTY_VERDICT_TTL_MS ? charges : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** True when the last charge message said 0. Safe from any thread. */
    public static boolean isKnownEmpty() {
        try {
            return knownCharges() == 0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** True for a whistle teleport row (any item id of the row is a whistle). Pure. */
    public static boolean isWhistleTransport(Transport transport) {
        if (transport == null || transport.getItemIdRequirements() == null) {
            return false;
        }
        return transport.getItemIdRequirements().stream()
                .flatMap(Collection::stream)
                .anyMatch(id -> CHARGED_WHISTLES.contains(id) || id == INFINITE_WHISTLE);
    }

    /**
     * Pathfinder check: false for a whistle teleport when the charged whistle is known empty and the
     * infinite one is not available.
     *
     * @param hasInfinite whether the perfected (i) whistle is carried/usable
     */
    public static boolean isUsable(Transport transport, boolean hasInfinite) {
        return !isWhistleTransport(transport) || hasInfinite || !isKnownEmpty();
    }

    /** The {@code QUETZAL_LAST_DESTINATION} value for a destination, or -1. Pure. */
    static int lastDestinationValue(WorldPoint destination) {
        if (destination == null || destination.getPlane() != 0) {
            return -1;
        }
        for (int[] d : LAST_DESTINATIONS) {
            if (Math.max(Math.abs(destination.getX() - d[0]), Math.abs(destination.getY() - d[1])) <= LANDING_MATCH_TILES) {
                return d[2];
            }
        }
        return -1;
    }

    /**
     * Uses "Last-destination" when the whistle's last destination is the target. Script thread only.
     *
     * @return null when it does not apply (caller opens the map as usual), else whether the player landed
     */
    public static Boolean tryLastDestination(Rs2ItemModel whistle, WorldPoint destination) {
        if (whistle == null || destination == null || lastDestinationDisabled) {
            return null;
        }
        int wanted = lastDestinationValue(destination);
        if (wanted < 0 || ProjectX.getVarbitValue(VarbitID.QUETZAL_LAST_DESTINATION) != wanted) {
            return null;
        }
        if (!Rs2Inventory.interact(whistle, "Last-destination")) {
            return null;
        }
        log.info("[Walker] quetzal whistle Last-destination -> {}", destination);
        boolean landed = sleepUntil(() -> {
            WorldPoint here = Rs2Player.getWorldLocation();
            return here != null && here.getPlane() == destination.getPlane()
                    && here.distanceTo2D(destination) <= LANDING_MATCH_TILES;
        }, 9000);
        if (!landed) {
            noteIfOutOfCharges();
            if (!isKnownEmpty()) {
                lastDestinationDisabled = true;
                log.warn("[Walker] quetzal whistle Last-destination did not land at {} (at {}); using the map from now on",
                        destination, Rs2Player.getWorldLocation());
            }
        }
        return landed;
    }

    /** After a whistle that did nothing: records 0 charges when the game said so. Script thread. */
    public static void noteIfOutOfCharges() {
        String text = Rs2Dialogue.getDialogueText();
        if (chargesFromMessage(text) == 0) {
            log.info("[Walker] quetzal whistle is out of charges; whistle teleports left out of planning");
            storeCharges(0);
        }
    }
}
