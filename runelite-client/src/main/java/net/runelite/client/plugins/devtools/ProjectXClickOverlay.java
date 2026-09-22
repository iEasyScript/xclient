package net.runelite.client.plugins.devtools;

import net.runelite.api.Client;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;

public class ProjectXClickOverlay extends Overlay {
    private final Client client;
    private final DevToolsPlugin plugin;

    @Inject
    ProjectXClickOverlay(Client client, DevToolsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_LOW);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D g) {
        if (plugin.getMouseClick().isActive()) {

            g.setFont(new Font("Tahoma", Font.BOLD, 18));

            // Get the FontMetrics for the current font
            FontMetrics metrics = g.getFontMetrics(g.getFont());

            // Get the width and height of the character
            int charWidth = metrics.stringWidth("X");
            int charHeight = metrics.getAscent(); // ascent gives the height of the character above the baseline

            int x = ProjectX.getMouse().getLastClick().getX() - (charWidth / 2);
            int y = ProjectX.getMouse().getLastClick().getY() + (charHeight / 2);

            OverlayUtil.renderTextLocation(g, new net.runelite.api.Point(x, y), "X", Color.WHITE);

            x = ProjectX.getMouse().getLastClick2().getX() - (charWidth / 2);
            y = ProjectX.getMouse().getLastClick2().getY() + (charHeight / 2);

            OverlayUtil.renderTextLocation(g, new net.runelite.api.Point(x, y), "X", Color.GREEN);



        }

        return null;
    }
}

