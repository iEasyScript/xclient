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
import net.runelite.client.plugins.projectx.qualityoflife.enums.DragonhideCrafting;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.menu.NewMenuEntry;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;

import java.util.List;
import java.util.Optional;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

@Slf4j
public class CraftingManager {
    private final QoLConfig config;

    @Inject
    public CraftingManager(QoLConfig config) {
        this.config = config;
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event) {
        if (!ProjectX.isLoggedIn()) return;

        String option = event.getOption();
        int itemId = event.getItemId();

        if (itemId == ItemID.NEEDLE && "Use".equals(option) && config.quickCraftItems()) {
            modifyMenuEntry(event, "<col=FFA500>Quick craft: </col>", config.craftingItem().getName(), this::craftHideOnClick);
        }
    }

    private void modifyMenuEntry(MenuEntryAdded event, String newOption, String newTarget, java.util.function.Consumer<MenuEntry> onClick) {
        MenuEntry menuEntry = event.getMenuEntry();
        menuEntry.setOption(newOption);
        menuEntry.setTarget(newTarget);
        menuEntry.onClick(onClick);
    }

    private void craftHideOnClick(MenuEntry event) {
        List<Rs2ItemModel> craftableHides = DragonhideCrafting.getCraftableHides();

        if (craftableHides.isEmpty()) {
            ProjectX.log("<col=5F1515>No craftable hides found in inventory</col>");
            return;
        }

        Rs2ItemModel hideItem = craftableHides.get(0);
        if (hideItem == null) {
            ProjectX.log("<col=5F1515>Could not find the selected hide in inventory</col>");
            return;
        }

        ProjectX.log("<col=245C2D>Crafting: " + hideItem.getName() + "</col>");
        NewMenuEntry combined = createWidgetOnWidgetEntry("Craft", hideItem.getName(), hideItem.getSlot(), event.getParam1(), hideItem.getId());
        ProjectX.getMouse().click(ProjectX.getClient().getMouseCanvasPosition(), combined);

        ProjectX.getClientThread().runOnSeperateThread(() -> {
            sleepUntil(Rs2Widget::isProductionWidgetOpen, 1000);
            if (Rs2Widget.isProductionWidgetOpen()) {
                Rs2Widget.clickWidget(config.craftingItem().getContainsInventoryName(), Optional.of(270),13,false);
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
