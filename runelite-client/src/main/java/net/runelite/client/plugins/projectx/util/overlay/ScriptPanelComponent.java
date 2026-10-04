package net.runelite.client.plugins.projectx.util.overlay;

import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * A {@link ScriptPanel} as a child of an {@code OverlayPanel}, for the overlays that also carry
 * components of their own -- a Pause button, say, which needs an OverlayPanel parent to know where
 * it is on screen.
 *
 * <p>Set the panel each frame with {@link #setPanel}, add this before the other children, and give
 * the parent no background or border so the panel's own frame is the only one drawn.
 */
public final class ScriptPanelComponent implements LayoutableRenderableEntity {
	private final Rectangle bounds = new Rectangle();
	private final Point location = new Point();
	private ScriptPanel panel;

	public void setPanel(ScriptPanel panel) {
		this.panel = panel;
	}

	@Override
	public Dimension render(Graphics2D graphics) {
		if (panel == null) return new Dimension();
		graphics.translate(location.x, location.y);
		Dimension size;
		try {
			size = panel.render(graphics);
		} finally {
			graphics.translate(-location.x, -location.y);
		}
		bounds.setBounds(location.x, location.y, size.width, size.height);
		return size;
	}

	@Override
	public Rectangle getBounds() {
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point position) {
		location.setLocation(position);
	}

	@Override
	public void setPreferredSize(Dimension dimension) {
		// The panel sizes itself from its content.
	}
}
