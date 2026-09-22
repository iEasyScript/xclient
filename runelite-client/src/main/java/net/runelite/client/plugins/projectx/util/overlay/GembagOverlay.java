package net.runelite.client.plugins.projectx.util.overlay;

import net.runelite.api.Point;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Gembag;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import javax.inject.Inject;
import java.awt.*;

public class GembagOverlay extends WidgetItemOverlay {

    @Inject
    public GembagOverlay() {
        showOnInventory();
    }

    @Override
    public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget) {
        if (!Rs2Gembag.getGemBagItemIds().contains(itemId)) return;
        
        final Rectangle bounds = itemWidget.getCanvasBounds();
        final TextComponent textComponent = new TextComponent();
        textComponent.setPosition(new java.awt.Point(bounds.x - 1, bounds.y + 8));
        textComponent.setColor(Color.CYAN);
        textComponent.setText(Rs2Gembag.isUnknown() ? "?" : String.valueOf(Rs2Gembag.getTotalGemCount()));
        textComponent.render(graphics);
        
        final Point mousePos = ProjectX.getClient().getMouseCanvasPosition();
        if (bounds.contains(mousePos.getX(), mousePos.getY())) {
            StringBuilder tooltipBuilder = new StringBuilder("Gem Bag Contents:<br>");
            if (!Rs2Gembag.isUnknown()) {
                Rs2Gembag.getGemBagContents().forEach(item ->
                        tooltipBuilder.append(item.getQuantity())
                                .append(" ")
                                .append(item.getName())
                                .append("<br>")
                );
                ProjectX.getTooltipManager().add(new Tooltip(tooltipBuilder.toString()));
            }
        }
    }
}
