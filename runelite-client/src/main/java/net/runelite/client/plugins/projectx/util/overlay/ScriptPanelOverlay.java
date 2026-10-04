package net.runelite.client.plugins.projectx.util.overlay;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.Dimension;
import java.awt.Graphics2D;

/**
 * An overlay that draws a {@link ScriptPanel}. A script's overlay extends this and fills the panel
 * in {@link #build}; position, layering and drawing are handled here.
 *
 * <p>A mistake in {@code build} -- a null nobody expected, a widget that is not loaded -- does not
 * take the overlay down: a bare panel is drawn instead, with the error as its status line, and the
 * error is logged at most once a minute.
 */
@Slf4j
public abstract class ScriptPanelOverlay extends Overlay {

	private long lastErrorLogged = 0;

	protected ScriptPanelOverlay(Plugin plugin) {
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		setNaughty();
	}

	/**
	 * Fills in the panel for this frame. Runs on the client thread, every frame: read state, do not
	 * wait for anything.
	 */
	protected abstract ScriptPanel build();

	@Override
	public final Dimension render(Graphics2D graphics) {
		ScriptPanel panel;
		try {
			panel = build();
		} catch (RuntimeException e) {
			long now = System.currentTimeMillis();
			if (now - lastErrorLogged > 60_000) {
				lastErrorLogged = now;
				log.warn("{} could not build its panel", getClass().getSimpleName(), e);
			}
			panel = ScriptPanel.create(getClass().getSimpleName().replace("Overlay", ""), null)
					.status("Panel error: " + e.getClass().getSimpleName());
		}
		return panel == null ? null : panel.render(graphics);
	}
}
