package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.shortestpath.Transport;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * Cobwebs ({@code Slash;Web;733} in transports.tsv). Ported from the DAX Web Walker (Apache-2.0, see
 * shortestpath/THIRD_PARTY_DAX.txt), PathObjectHandler WEB, canLeftclickWeb and useBladeOnWeb.
 *
 * <p>"Slash" only cuts the web when the wielded weapon slashes (varbit {@code COMBAT_WEAPON_CATEGORY})
 * or a knife is carried; otherwise the game says only a sharp blade will do, and the walker used to
 * click Slash for eight seconds and give up. Now it uses a knife or carried blade on the web, and the
 * pathfinder only routes through webs when one of those is true ({@link #canCutWebs}).
 */
@Slf4j
public final class WebCutting {

    /**
     * {@code COMBAT_WEAPON_CATEGORY} values whose weapons slash: axe, claw, slash sword, two-handed
     * sword, polearm, stab sword, whip and bladed staff (DAX SLASH_WEAPONS).
     */
    static final Set<Integer> SLASH_WEAPON_CATEGORIES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(1, 4, 9, 10, 12, 17, 20, 21)));

    /** Name fragments of carried items that cut a web when used on it (DAX useBladeOnWeb). */
    private static final String[] BLADE_NAME_PARTS = {
            "whip", "sword", "dagger", "claws", "scimitar", " axe", "halberd", "machete", "rapier"};

    /** Cut attempts before giving up on one web. */
    static final int MAX_CUT_ATTEMPTS = 6;

    private WebCutting() {
    }

    /** True for a transport row that crosses a cobweb. Pure. */
    public static boolean isWebTransport(Transport transport) {
        return transport != null && transport.getObjectId() == ObjectID.BIGWEB_SLASHABLE;
    }

    /** True when the weapon category slashes. Pure. */
    static boolean isSlashCategory(int weaponCategory) {
        return SLASH_WEAPON_CATEGORIES.contains(weaponCategory);
    }

    /** True for a knife or a carried blade by item name ("Swordfish" is not a sword). Pure. */
    static boolean isBladeName(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (n.equals("knife")) {
            return true;
        }
        if (n.contains("swordfish")) {
            return false;
        }
        for (String part : BLADE_NAME_PARTS) {
            if (n.contains(part)) {
                return true;
            }
        }
        return false;
    }

    /** The left-click "Slash" works: a slashing weapon is wielded or a knife is carried. */
    static boolean canSlashDirectly() {
        int category = ProjectX.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
        return isSlashCategory(category) || Rs2Inventory.hasItem(ItemID.KNIFE);
    }

    private static Rs2ItemModel carriedBlade() {
        return Rs2Inventory.items(item -> item != null && !item.isNoted() && isBladeName(item.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether this account can get through a web right now (used by the pathfinder). Aranea boots
     * keep the old behaviour of always allowing the route.
     */
    public static boolean canCutWebs() {
        try {
            return canSlashDirectly()
                    || carriedBlade() != null
                    || Rs2Equipment.isWearing(ItemID.ARANEA_BOOTS);
        } catch (RuntimeException e) {
            // No game state to read (logged out, tests): keep the old behaviour of routing through webs.
            log.debug("[Walker] web capability unreadable, allowing web routes: {}", e.toString());
            return true;
        }
    }

    private static boolean webStandsAt(WorldPoint location) {
        return !Rs2GameObject.getAll(o -> o.getId() == ObjectID.BIGWEB_SLASHABLE
                && Objects.equals(location, o.getWorldLocation())).isEmpty();
    }

    private static boolean useBladeOn(TileObject web) {
        Rs2ItemModel blade = carriedBlade();
        if (blade == null) {
            return false;
        }
        if (!Rs2Inventory.isItemSelected() && !Rs2Inventory.use(blade)) {
            return false;
        }
        if (!sleepUntil(Rs2Inventory::isItemSelected, 1200)) {
            return false;
        }
        // With an item selected the object click is "Use <item> -> Web".
        return Rs2GameObject.interact(web);
    }

    /**
     * Cuts the web and steps through it. Script thread only.
     *
     * @return true once the web is gone and the player is moving through, false when it could not be cut
     */
    public static boolean cut(TileObject web, WorldPoint destination) {
        final WorldPoint location = web.getWorldLocation();
        sleepUntil(() -> !Rs2Player.isMoving() && !Rs2Player.isAnimating(), 1200);
        int attempts = 0;
        while (webStandsAt(location) && attempts < MAX_CUT_ATTEMPTS) {
            attempts++;
            boolean clicked = canSlashDirectly() ? Rs2GameObject.interact(web, "Slash") : useBladeOn(web);
            if (!clicked) {
                log.warn("[Walker] cannot cut web at {}: no slashing weapon, knife or blade to use", location);
                return false;
            }
            WorldPoint player = Rs2Player.getWorldLocation();
            boolean adjacent = player != null && location != null && player.distanceTo2D(location) <= 1;
            // Cuts can miss; the web stays until one lands (DAX waits 0.05-0.8 s next to it, 2-4 s away).
            sleepUntil(() -> !webStandsAt(location), adjacent ? 1800 : 4000);
        }
        if (webStandsAt(location)) {
            log.warn("[Walker] web at {} still standing after {} cut attempts", location, attempts);
            return false;
        }
        final WorldPoint before = Rs2Player.getWorldLocation();
        if (destination != null) {
            Rs2Walker.walkFastCanvas(destination);
        }
        return sleepUntil(() -> !Objects.equals(before, Rs2Player.getWorldLocation()), 2400);
    }
}
