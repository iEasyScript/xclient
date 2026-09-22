package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.runenergy.RunEnergyPlugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;
import java.awt.*;
import java.util.stream.IntStream;

public class ETAOverlayPanel extends OverlayPanel {
    
    private final ShortestPathPlugin plugin;

    @Inject
    ETAOverlayPanel(ShortestPathPlugin plugin) {
        this.plugin = plugin;
        setPosition(OverlayPosition.CANVAS_TOP_RIGHT);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        try {
            panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));
            panelComponent.setPreferredSize(new Dimension(160, 100));

            if (ShortestPathPlugin.getPathfinder() != null && ShortestPathPlugin.getPathfinder().isDone()) {
                List<WorldPoint> path = ShortestPathPlugin.getPathfinder().getPath();
                WorldPoint playerLocation = Rs2Player.getWorldLocation();

                int progressIndex = findClosestPointIndex(playerLocation, path);

                int remainingPathLength = path.size() - progressIndex;

                String remainingTime = RunEnergyPlugin.calculateTravelTime(remainingPathLength, plugin.getConfig().showInSeconds());

                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Est. Time till Arrival:")
                        .right(remainingTime)
                        .build());
            } else {
                if (!panelComponent.getChildren().isEmpty()) {
                    panelComponent.getChildren().clear();
                }
            }
        } catch (Exception ex) {
            System.out.println("Error in render: " + ex.getMessage());
        }
        return super.render(graphics);
    }

    private int findClosestPointIndex(WorldPoint playerLocation, List<WorldPoint> path) {
        return IntStream.range(0, path.size())
                .boxed()
                .min(Comparator.comparingInt(i -> playerLocation.distanceTo(path.get(i))))
                .orElse(0); 
    }
}
