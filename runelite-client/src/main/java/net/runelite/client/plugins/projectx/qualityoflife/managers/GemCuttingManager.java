package net.runelite.client.plugins.projectx.qualityoflife.managers;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.qualityoflife.QoLConfig;
import net.runelite.client.plugins.projectx.qualityoflife.enums.UncutGems;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.projectx.util.menu.NewMenuEntry;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import java.awt.event.KeyEvent;
import java.util.List;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

@Slf4j
public class GemCuttingManager {

    private final QoLConfig config;

    @Inject
    public GemCuttingManager(QoLConfig config) {
        this.config = config;
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event) {
        if (!ProjectX.isLoggedIn()) return;

        String option = event.getOption();
        int itemId = event.getItemId();

        if (itemId == ItemID.CHISEL && "Use".equals(option) && config.quickCutGems()) {
            modifyMenuEntry(event, "<col=FFA500>Cut Gem</col>", "", this::cutGemOnClick);
        }
    }

    private void modifyMenuEntry(MenuEntryAdded event, String newOption, String newTarget, java.util.function.Consumer<MenuEntry> onClick) {
        MenuEntry menuEntry = event.getMenuEntry();
        menuEntry.setOption(newOption);
        menuEntry.setTarget(newTarget);
        menuEntry.onClick(onClick);
    }

    private void cutGemOnClick(MenuEntry event) {
        List<Rs2ItemModel> gemToCut = UncutGems.getAvailableGems();

        if (gemToCut.isEmpty()) {
            ProjectX.log("<col=5F1515>No gems available or level too low to cut gems</col>");
            return;
        }

        Rs2ItemModel gemItem = gemToCut.get(0);
        if (gemItem == null) {
            ProjectX.log("<col=5F1515>Could not find the selected gem in inventory</col>");
            return;
        }

        ProjectX.log("<col=245C2D>Cutting: " + gemItem.getName() + "</col>");
        NewMenuEntry combined = createWidgetOnWidgetEntry("Cut", gemItem.getName(), gemItem.getSlot(), event.getParam1(), gemItem.getId());
        ProjectX.getMouse().click(ProjectX.getClient().getMouseCanvasPosition(), combined);
        ProjectX.getClientThread().runOnSeperateThread(() -> {
            sleepUntil(Rs2Widget::isProductionWidgetOpen, 1000);
            if (Rs2Widget.isProductionWidgetOpen()) {
                Rs2Keyboard.keyPress(KeyEvent.VK_SPACE);
            }
            return null;
        });
    }

    private NewMenuEntry createWidgetOnWidgetEntry(String option, String target, int param0, int param1, int itemId) {
        NewMenuEntry combined = new NewMenuEntry();
        combined.setOption(option);
        combined.setTarget(target);
        combined.setParam0(param0);
        combined.setParam1(param1);
        combined.setIdentifier(0);
        combined.setType(MenuAction.WIDGET_TARGET_ON_WIDGET);
        combined.setItemId(itemId);
        return combined;
    }
}
