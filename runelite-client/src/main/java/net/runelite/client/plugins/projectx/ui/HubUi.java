package net.runelite.client.plugins.projectx.ui;

import net.runelite.client.ui.ColorScheme;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;

/**
 * The shared look of the plugin hub and the tabs above it.
 *
 * <p>Written for the same reasons as the antiban panel's {@code AntibanUi}: the
 * decisions live in one place so the panel cannot drift, and every colour is
 * stated rather than inherited. The old panel set almost no foregrounds at all
 * and took whatever the look and feel handed it, which is how the plugin names
 * ended up the same yellow as the tab labels.
 *
 * <p>State is shown with a stripe down the left edge rather than a coloured box
 * around the whole row. A full outline in green or orange made every installed
 * plugin shout at the same volume as everything else, and with a long list that
 * is just noise; a stripe is read at a glance and leaves the text alone.
 */
final class HubUi
{
	/** Behind the list. */
	static final Color BACKGROUND = ColorScheme.DARK_GRAY_COLOR;

	/** A row. Sits a shade above the background so the list has edges. */
	static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;

	/** The same row under the pointer. */
	static final Color CARD_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;

	static final Color TEXT = Color.WHITE;
	static final Color TEXT_DIM = ColorScheme.LIGHT_GRAY_COLOR;
	static final Color TEXT_FAINT = ColorScheme.MEDIUM_GRAY_COLOR;

	static final Color DIVIDER = ColorScheme.DARK_GRAY_COLOR;

	/** Installed and on the published build. */
	static final Color INSTALLED = ColorScheme.PROGRESS_COMPLETE_COLOR;

	/** Installed, but a newer build has been published. */
	static final Color UPDATE = ColorScheme.BRAND_ORANGE;

	/** Removing something. Only ever on the control that does it. */
	static final Color DANGER = ColorScheme.PROGRESS_ERROR_COLOR;

	private static final int STRIPE = 3;

	private HubUi()
	{
	}

	/**
	 * A row's border: a state stripe on the left, a hairline underneath, and
	 * room to breathe inside.
	 *
	 * @param state the stripe colour, or null when the row has nothing to say
	 */
	static Border row(Color state)
	{
		// The stripe is always drawn, in the row's own colour when there is no
		// state to show, so nothing shifts sideways as a plugin is installed.
		Border edges = BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER),
			BorderFactory.createMatteBorder(0, STRIPE, 0, 0, state == null ? CARD : state));

		return BorderFactory.createCompoundBorder(edges,
			BorderFactory.createEmptyBorder(7, 8, 7, 7));
	}
}
