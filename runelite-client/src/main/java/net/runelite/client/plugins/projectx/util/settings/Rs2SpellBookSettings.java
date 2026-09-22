package net.runelite.client.plugins.projectx.util.settings;

import java.awt.Rectangle;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.globval.enums.InterfaceTab;
import net.runelite.client.plugins.projectx.util.menu.NewMenuEntry;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;
import net.runelite.client.plugins.projectx.util.tabs.Rs2Tab;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import java.util.Optional;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;
import static net.runelite.client.plugins.projectx.util.Global.sleepUntilTrue;
import static net.runelite.client.plugins.projectx.util.widget.Rs2Widget.*;

/**
 * Rs2SpellBookSettings allows to enable/disable spell book filters
 */
@Slf4j
public class Rs2SpellBookSettings {
    /**
     * toggle spell filter
     * @param text
     * @param toggle
     * @param varbitId
     * @param closeInterface
     * @return
     */
    public static boolean toggleSpellFilter(String text, boolean toggle, int varbitId, boolean closeInterface) {
        int expectedValue = toggle ? 0 : 1;
        if (ProjectX.getVarbitValue(varbitId) == expectedValue) {
            return true;
        }

		if (!Rs2Settings.isSpellFilteringEnabled()) {
			Rs2Settings.enableSpellFiltering();
		}

        Rs2Tab.switchTo(InterfaceTab.MAGIC);

        if (!sleepUntilTrue(() -> Rs2Tab.getCurrentTab() == InterfaceTab.MAGIC, 300, 2000)) {
            return false;
        }

        log.info("{} following spell filter: {}", toggle ? "Turning on" : "Turning off", text);
        if (!hasWidgetText(text, 218, 0, true)) {
            Rs2Widget.clickWidget("Filters", Optional.of(218), 0, true);

            boolean result = sleepUntilHasWidgetText(text, 218, 0, true, 2000);
            if (!result) {
                log.error("did not find widget with text: {}", text);
                return false;
            }
        }

        Rs2Widget.clickWidget(text, Optional.of(218), 0, true);
        sleepUntil(() -> ProjectX.getVarbitValue(varbitId) == expectedValue, 2000);

        if (closeInterface) {
            closeSpellBookFilter();
        }

        return ProjectX.getVarbitValue(varbitId) == expectedValue;
    }

    /**
     * close spell book filter
     */
    private static void closeSpellBookFilter() {
        if ( hasWidgetText("Spell Filters", 218, 0, true)) {
            Rs2Widget.clickWidget("Filters", Optional.of(218), 0, true);
            sleepUntilHasNotWidgetText("Spell Filters", 218, 0, true, 2000);
        }
    }

    /**
     * toggle spell filter
     * @param text
     * @param toggle
     * @param varbitId
     * @return
     */
    public static boolean toggleSpellFilter(String text, boolean toggle, int varbitId) {
        return toggleSpellFilter(text, toggle, varbitId, true);
    }

    /**
     * toggleCombatSpells
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleCombatSpells(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Show Combat spells", toggle, 6605, closeInterface);
    }

    /**
     * toggleTeleportSpells
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleTeleportSpells(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Show Teleport spells", toggle, 6609, closeInterface);
    }

    /**
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleUtilitySpells(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Show Utility spells", toggle, 6606, closeInterface);
    }

    /**
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleLackMagicLevel(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Show spells you lack the Magic level to cast", toggle, 6607, closeInterface);
    }

    /**
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleLackRunes(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Show spells you lack the runes to cast", toggle, 6608, closeInterface);
    }

    /**
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleLackRequirements(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Show spells you lack the requirements to cast", toggle, 12137, closeInterface);
    }

    /**
     * @param toggle
     * @param closeInterface
     * @return
     */
    public static boolean toggleIconResizing(boolean toggle, boolean closeInterface) {
        return toggleSpellFilter("Enable icon resizing (outside PvP areas)", toggle, 6548, closeInterface);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleCombatSpells(boolean toggle) {
        return toggleSpellFilter("Show Combat spells", toggle, 6605);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleTeleportSpells(boolean toggle) {
        return toggleSpellFilter("Show Teleport spells", toggle, 6609);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleUtilitySpells(boolean toggle) {
        return toggleSpellFilter("Show Utility spells", toggle, 6606);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleLackMagicLevel(boolean toggle) {
        return toggleSpellFilter("Show spells you lack the Magic level to cast", toggle, 6607);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleLackRunes(boolean toggle) {
        return toggleSpellFilter("Show spells you lack the runes to cast", toggle, 6608);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleLackRequirements(boolean toggle) {
        return toggleSpellFilter("Show spells you lack the requirements to cast", toggle, 12137, true);
    }

    /**
     * @param toggle
     * @return
     */
    public static boolean toggleIconResizing(boolean toggle) {
        return toggleSpellFilter("Enable icon resizing (outside PvP areas)", toggle, 6548, true);
    }

    /**
     * @return
     */
    public static boolean setAllFiltersOn() {
            boolean success = toggleCombatSpells(true, false) &&
                    toggleTeleportSpells(true, false) &&
                    toggleUtilitySpells(true, false) &&
                    toggleLackMagicLevel(true, false) &&
                    toggleLackRunes(true, false) &&
                    toggleLackRequirements(true, false) &&
                    toggleIconResizing(true, false);

            if (success) {
                closeSpellBookFilter();
            }
            return success;
    }

	public static boolean configureSpellbookSettings() {
		boolean success = toggleCombatSpells(true, false) &&
			toggleTeleportSpells(true, false) &&
			toggleUtilitySpells(true, false) &&
			toggleLackMagicLevel(true, false) &&
			toggleLackRunes(true, false) &&
			toggleLackRequirements(true, false) &&
			toggleIconResizing(false, false);

		if (success) {
			closeSpellBookFilter();
		}
		return success;
	}

    /**
     * @return
     */
    public static boolean setAllFiltersOff() {
        boolean success = toggleCombatSpells(false, false) &&
                toggleTeleportSpells(false, false) &&
                toggleUtilitySpells(false, false) &&
                toggleLackMagicLevel(false, false) &&
                toggleLackRunes(false, false) &&
                toggleLackRequirements(false, false) &&
                toggleIconResizing(false, false);

        if (success) {
            closeSpellBookFilter();
        }
        return success;
    }
}
