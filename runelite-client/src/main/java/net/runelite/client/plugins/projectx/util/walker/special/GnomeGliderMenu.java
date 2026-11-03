package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Picks a gnome glider destination by the button's action text ("Travel to Kar-Hewo"...), as the
 * DAX Web Walker did (Apache-2.0, see shortestpath/THIRD_PARTY_DAX.txt; GnomeGlider.to). The
 * per-destination component ids stay as the fallback when no action matches.
 */
@Slf4j
public final class GnomeGliderMenu {

    /** Component of each destination button, by gnome_gliders.tsv display info. */
    static final Map<String, Integer> FALLBACK_BUTTONS = new HashMap<>();

    static {
        FALLBACK_BUTTONS.put("Ta Quir Priw", InterfaceID.Glidermap.GRANDTREE_BUTTON);
        FALLBACK_BUTTONS.put("Sindarpos", InterfaceID.Glidermap.WHITEWOLFMOUNTAIN_BUTTON);
        FALLBACK_BUTTONS.put("Lemanto Andra", InterfaceID.Glidermap.VARROCK_BUTTON);
        FALLBACK_BUTTONS.put("Kar-Hewo", InterfaceID.Glidermap.ALKHARID_BUTTON);
        FALLBACK_BUTTONS.put("Gandius", InterfaceID.Glidermap.KARAMJA_BUTTON);
        FALLBACK_BUTTONS.put("Lemantolly Undri", InterfaceID.Glidermap.OGREAREA_BUTTON);
        FALLBACK_BUTTONS.put("Ookookolly Undri", InterfaceID.Glidermap.APEATOLL_BUTTON);
    }

    private GnomeGliderMenu() {
    }

    /** True when one of the actions names the destination. Pure. */
    static boolean actionsName(String[] actions, String destination) {
        if (actions == null || destination == null || destination.isEmpty()) {
            return false;
        }
        String wanted = destination.toLowerCase(Locale.ROOT);
        for (String action : actions) {
            if (action != null && Rs2UiHelper.stripColTags(action).toLowerCase(Locale.ROOT).contains(wanted)) {
                return true;
            }
        }
        return false;
    }

    /** Finds the button whose action names the destination. Client thread only. */
    private static Widget findByAction(String destination) {
        Widget root = ProjectX.getClient().getWidget(InterfaceID.Glidermap.UNIVERSE);
        if (root == null || root.isHidden()) {
            return null;
        }
        Deque<Widget> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 500) {
            Widget w = queue.poll();
            if (w == null || w.isHidden()) {
                continue;
            }
            if (actionsName(w.getActions(), destination)) {
                return w;
            }
            for (Widget[] children : new Widget[][]{w.getStaticChildren(), w.getDynamicChildren(), w.getNestedChildren()}) {
                if (children != null) {
                    for (Widget c : children) {
                        if (c != null) {
                            queue.add(c);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Clicks the destination on the open glider map. Script thread only.
     *
     * @return true when a destination button was clicked
     */
    public static boolean clickDestination(String destination) {
        if (destination == null || destination.isEmpty()) {
            return false;
        }
        Widget byAction = ProjectX.getClientThread()
                .runOnClientThreadOptional(() -> findByAction(destination))
                .orElse(null);
        if (byAction != null) {
            return Rs2Widget.clickWidget(byAction);
        }
        Integer fallback = FALLBACK_BUTTONS.get(destination);
        if (fallback == null) {
            log.error("[Walker] gnome glider destination \"{}\" not on the map (no action match, no known button)", destination);
            return false;
        }
        log.debug("[Walker] gnome glider: no action names \"{}\", using button {}", destination, fallback);
        return Rs2Widget.clickWidget(fallback);
    }
}
