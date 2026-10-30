package net.runelite.client.plugins.projectx.util.walker;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.ProjectX;

import java.awt.Rectangle;
import java.util.function.IntFunction;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

/**
 * The game's "are you sure you want to go in there?" warnings: the Mort Myre swamp gate, the
 * Shantay Pass, the Stronghold of Security ladders and the rest of the list the Doomsayer toggles.
 * Each is its own interface (560-580) with the same layout: the proceed button is {@code WARN1}.
 * Until the walker read them all, only the desert and Stronghold warnings were answered; at the
 * Mort Myre gate the warning stayed open, the gate never let the player through and the walker
 * clicked the gate again in a loop.
 *
 * <p>Visibility and bounds are read on the client thread; the click is dispatched from the
 * calling (script) thread. The "don't ask again" toggle is left as the player set it.
 */
@Slf4j
final class AreaWarning {

    /** Proceed button ({@code WARN1}) of every warning interface that has one. */
    static final int[] PROCEED_BUTTONS = {
            InterfaceID.CwsWarning1.WARN1,
            InterfaceID.CwsWarning2.WARN1,
            InterfaceID.CwsWarning3.WARN1,
            InterfaceID.CwsWarning4.WARN1,
            InterfaceID.CwsWarning5.WARN1,
            InterfaceID.CwsWarning6.WARN1,
            InterfaceID.CwsWarning8.WARN1,
            InterfaceID.CwsWarning9.WARN1,
            InterfaceID.CwsWarning10.WARN1,
            InterfaceID.CwsWarning12.WARN1,
            InterfaceID.CwsWarning13.WARN1,
            InterfaceID.CwsWarning14.WARN1,
            InterfaceID.CwsWarning15.WARN1,
            InterfaceID.CwsWarning16.WARN1,
            InterfaceID.CwsWarning17.WARN1,
            InterfaceID.CwsWarning18.WARN1,
            InterfaceID.CwsWarning19.WARN1,
            InterfaceID.CwsWarning20.WARN1,
            InterfaceID.CwsWarning21.WARN1,
            InterfaceID.CwsWarning22.WARN1,
            InterfaceID.CwsWarning23.WARN1,
    };

    private static final int CLOSE_WAIT_MS = 1800;

    private AreaWarning() {
    }

    /**
     * The proceed button of the warning that is open, or -1. Pure: {@code lookup} returns the
     * widget for a packed component id (the client's on the client thread, a fake in tests).
     */
    static int openProceedButton(IntFunction<Widget> lookup) {
        for (int component : PROCEED_BUTTONS) {
            Widget button = lookup.apply(component);
            if (button != null && !button.isHidden()) {
                return component;
            }
        }
        return -1;
    }

    /** True while one of the warnings is open. Safe from any thread. */
    static boolean isOpen() {
        return ProjectX.getClientThread()
                .runOnClientThreadOptional(() -> openProceedButton(ProjectX.getClient()::getWidget) != -1)
                .orElse(false);
    }

    /**
     * Presses proceed on the open warning, if any, and waits for it to close. Must not be called
     * on the client thread (it clicks and waits).
     *
     * @return true when a warning was open and its proceed button was clicked
     */
    static boolean acceptIfOpen() {
        Rectangle bounds = ProjectX.getClientThread().runOnClientThreadOptional(() -> {
            int component = openProceedButton(ProjectX.getClient()::getWidget);
            if (component == -1) {
                return null;
            }
            Rectangle r = ProjectX.getClient().getWidget(component).getBounds();
            return r == null || r.isEmpty() ? null : r;
        }).orElse(null);
        if (bounds == null) {
            return false;
        }
        log.info("[Walker] area warning open, pressing proceed");
        ProjectX.getMouse().click(bounds);
        sleepUntil(() -> !isOpen(), CLOSE_WAIT_MS);
        return true;
    }
}
