package net.runelite.client.plugins.projectx;

import com.google.common.base.Strings;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.util.Map;

public class ProjectXOverlay extends OverlayPanel {
    ProjectXPlugin plugin;
    ProjectXConfig config;

    @Inject
    ProjectXOverlay(ProjectXPlugin plugin, ProjectXConfig config) {
        super(plugin);
        setPosition(OverlayPosition.TOP_RIGHT);
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        panelComponent.setPreferredSize(new Dimension(200, 300));

        for (Map.Entry<WorldPoint, Integer> dangerousTile : Rs2Tile.getDangerousGraphicsObjectTiles().entrySet()) {
            drawTile(graphics, dangerousTile.getKey(), Color.RED, dangerousTile.getValue().toString());
        }

        return super.render(graphics);
    }

    private void drawTile(Graphics2D graphics, WorldPoint point, Color color, @Nullable String label) {
        WorldPoint playerLocation = Rs2Player.getWorldLocation();

        if (point.distanceTo(playerLocation) >= 32) {
            return;
        }

        LocalPoint lp;
        if (ProjectX.getClient().getTopLevelWorldView().getScene().isInstance()) {
            WorldPoint worldPoint = WorldPoint.toLocalInstance(ProjectX.getClient().getTopLevelWorldView().getScene(), point).stream().findFirst().get();
            lp = LocalPoint.fromWorld(ProjectX.getClient().getTopLevelWorldView(), worldPoint);
        } else {
            lp = LocalPoint.fromWorld(ProjectX.getClient().getTopLevelWorldView(), point);
        }

        if (lp == null) {
            return;
        }


        Polygon poly = Perspective.getCanvasTilePoly(ProjectX.getClient(), lp);
        if (poly != null) {
            OverlayUtil.renderPolygon(graphics, poly, color, new Color(0, 0, 0, 50), new BasicStroke(2f));
        }

        if (!Strings.isNullOrEmpty(label)) {
            Point canvasTextLocation = Perspective.getCanvasTextLocation(ProjectX.getClient(), graphics, lp, label, 0);
            if (canvasTextLocation != null) {
                OverlayUtil.renderTextLocation(graphics, canvasTextLocation, label, color);
            }
        }
    }
}

