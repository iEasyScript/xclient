package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The shared look of the antiban panel.
 *
 * <p>Every section used to lay itself out by hand with its own GridBagLayout and
 * its own idea of spacing, which is why the old panel drifted: the same control
 * appeared at three sizes depending on which tab you were on. The widgets here
 * are the only ones the sections build, so the spacing is decided once.
 *
 * <p>Two constraints shape all of it. The sidebar is about 225 pixels wide, so
 * nothing may assume it can grow sideways; and the whole panel is refreshed
 * every 600ms by the plugin's timer, so nothing may be rebuilt on refresh --
 * widgets are created once and only their text changes.
 */
final class AntibanUi
{
    /** The panel background, a shade below the cards that sit on it. */
    static final Color BACKGROUND = ColorScheme.DARKER_GRAY_COLOR;

    /** Raised surface for a group of related controls. */
    static final Color CARD = ColorScheme.DARK_GRAY_COLOR;

    static final Color ACCENT = ColorScheme.BRAND_ORANGE;
    static final Color TEXT = ColorScheme.LIGHT_GRAY_COLOR;
    static final Color TEXT_DIM = ColorScheme.MEDIUM_GRAY_COLOR;
    static final Color DIVIDER = ColorScheme.DARKER_GRAY_HOVER_COLOR;

    static final Color ON = ColorScheme.PROGRESS_COMPLETE_COLOR;
    static final Color OFF = ColorScheme.PROGRESS_ERROR_COLOR;

    /** Shown wherever a live value is not available yet. Never the word "null". */
    static final String NO_VALUE = "—";

    /**
     * Marks a control whose value the refresh is setting, so its listener can tell that apart from the user.
     *
     * <p>Sliders and combo boxes notify their listeners when their value is set in code, not only when the
     * user moves them, and the panel sets every value every 600ms. Without this, a script changing a setting
     * looked to the panel like the user changing it, and the script's value was saved as the user's.
     */
    private static final String QUIET = "antiban.quiet";

    private AntibanUi()
    {
    }

    /**
     * A titled group of controls.
     *
     * <p>Returns the card itself; callers add their controls to it directly, so
     * the title and the content share one container and cannot drift apart.
     */
    static JPanel card(String title)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BACKGROUND),
            BorderFactory.createEmptyBorder(8, 10, 10, 10)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel(title.toUpperCase());
        heading.setFont(FontManager.getRunescapeSmallFont());
        heading.setForeground(ACCENT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        card.add(heading);

        return card;
    }

    /** One setting. The explanation lives in the tooltip; the sidebar has no room for it. */
    static JCheckBox toggle(String text, String tooltip)
    {
        JCheckBox box = new JCheckBox(text);
        box.setToolTipText(tooltip);
        box.setBackground(CARD);
        box.setForeground(TEXT);
        box.setFont(FontManager.getRunescapeSmallFont());
        box.setFocusPainted(false);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        box.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        constrainHeight(box);
        return box;
    }

    /**
     * A toggle the user cannot change: these report what the antiban system is
     * doing right now rather than configuring it. Disabling a checkbox greys its
     * label into near-invisibility, so the state is drawn as a lamp instead.
     */
    static JLabel indicator(String text, String tooltip)
    {
        JLabel label = new JLabel(text);
        label.setToolTipText(tooltip);
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(TEXT_DIM);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
        constrainHeight(label);
        return label;
    }

    /** Paints an indicator as on or off, without changing its text. */
    static void setIndicator(JLabel label, String text, boolean active)
    {
        label.setText((active ? "●  " : "○  ") + text);
        label.setForeground(active ? ON : TEXT_DIM);
    }

    /**
     * Saves a checkbox's new state as the user's choice when the user clicks it. Checkboxes do not notify
     * action listeners when set in code, so the refresh needs no guard here.
     */
    static void onUserToggle(JCheckBox box, Consumer<Boolean> commit)
    {
        box.addActionListener(e ->
        {
            boolean selected = box.isSelected();
            Rs2AntibanSettings.userChange(() -> commit.accept(selected));
        });
    }

    /**
     * Wires a slider: {@code display} on every movement, so its readout follows the drag, and {@code commit}
     * once when the user lets go -- never for a value the refresh set.
     */
    static void onUserSlide(JSlider slider, IntConsumer display, IntConsumer commit)
    {
        slider.addChangeListener(e ->
        {
            display.accept(slider.getValue());
            if (isQuiet(slider) || slider.getValueIsAdjusting())
            {
                return;
            }
            commit.accept(slider.getValue());
        });
    }

    /** Moves a slider to a value from the settings, unless the user is dragging it right now. */
    static void setQuietly(JSlider slider, int value)
    {
        if (slider.getValueIsAdjusting())
        {
            return;
        }
        slider.putClientProperty(QUIET, Boolean.TRUE);
        try
        {
            slider.setValue(value);
        }
        finally
        {
            slider.putClientProperty(QUIET, null);
        }
    }

    /** Selects an item from the settings without it counting as the user's choice. */
    static <T> void setQuietly(JComboBox<T> combo, T item)
    {
        if (combo.isPopupVisible() || Objects.equals(combo.getSelectedItem(), item))
        {
            return;
        }
        combo.putClientProperty(QUIET, Boolean.TRUE);
        try
        {
            combo.setSelectedItem(item);
        }
        finally
        {
            combo.putClientProperty(QUIET, null);
        }
    }

    static boolean isQuiet(JComponent component)
    {
        return component.getClientProperty(QUIET) != null;
    }

    /** A full-width button in the panel's flat style. */
    static JButton button(String text, String tooltip)
    {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(small());
        button.setForeground(TEXT);
        button.setBackground(DIVIDER);
        button.setFocusPainted(false);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        constrainHeight(button);
        return button;
    }

    /** A drop-down that fills the card's width. */
    @SafeVarargs
    static <T> JComboBox<T> combo(String tooltip, T... items)
    {
        JComboBox<T> combo = new JComboBox<>(items);
        combo.setToolTipText(tooltip);
        combo.setFont(small());
        combo.setForeground(TEXT);
        combo.setBackground(DIVIDER);
        combo.setFocusable(false);
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        constrainHeight(combo);
        return combo;
    }

    /** A thin bar for something counting down, with its value written on it. */
    static JProgressBar progress(String tooltip)
    {
        JProgressBar bar = new JProgressBar();
        bar.setToolTipText(tooltip);
        bar.setFont(small());
        bar.setForeground(ACCENT);
        bar.setBackground(DIVIDER);
        bar.setStringPainted(true);
        bar.setBorder(BorderFactory.createEmptyBorder());
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setPreferredSize(new Dimension(0, 16));
        constrainHeight(bar);
        return bar;
    }

    /** A caption with its current value on the right, above the slider it describes. */
    static JPanel readout(JLabel caption, JLabel value)
    {
        caption.setFont(FontManager.getRunescapeSmallFont());
        caption.setForeground(TEXT);
        value.setFont(FontManager.getRunescapeSmallFont());
        value.setForeground(ACCENT);
        value.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(CARD);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createEmptyBorder(6, 0, 1, 0));
        row.add(caption, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        constrainHeight(row);
        return row;
    }

    /**
     * A slider with no tick marks or printed labels.
     *
     * <p>The old panel painted both on every slider, which cost around twenty
     * vertical pixels each and rendered a row of numbers too small to read at
     * this width. The value is in the readout above instead.
     */
    static JSlider slider(int min, int max, int value)
    {
        JSlider slider = new JSlider(min, max, clamp(value, min, max));
        slider.setBackground(CARD);
        slider.setForeground(TEXT);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setFocusable(false);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        constrainHeight(slider);
        return slider;
    }

    /** A line of explanation, for the few places where a tooltip is not enough. */
    static JLabel hint(String text)
    {
        JLabel label = new JLabel("<html><body style='width:170px'>" + text + "</body></html>");
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(TEXT_DIM);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
        return label;
    }

    /** A slowdown multiplier as the panel shows it: "+12%", or "None" below half a percent. */
    static String percentSlower(double multiplier)
    {
        long percent = Math.round((multiplier - 1.0) * 100);
        return percent <= 0 ? "None" : "+" + percent + "%";
    }

    static Component gap(int height)
    {
        return Box.createVerticalStrut(height);
    }

    static Font small()
    {
        return FontManager.getRunescapeSmallFont();
    }

    /**
     * Stops a BoxLayout stretching a component to fill leftover height, which is
     * what left the old panel with a checkbox floating in the middle of a gap.
     */
    static void constrainHeight(JComponent component)
    {
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
    }

    private static int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }
}
