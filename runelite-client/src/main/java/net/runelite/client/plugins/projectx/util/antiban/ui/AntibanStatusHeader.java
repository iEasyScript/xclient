package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

/**
 * What the antiban system is doing, at the top of the panel.
 *
 * <p>This is the part the old layout got backwards. The live state -- play
 * style, current activity, whether the bot is mid-action -- was in a box at the
 * very bottom labelled "Additional Info", under a hundred pixels of empty space
 * and an animated duck, while the top of the panel was a banner that never said
 * anything. The state is the reason to open this panel at all, so it goes first,
 * and the decoration is gone.
 */
class AntibanStatusHeader extends JPanel
{
    private final JLabel state = new JLabel();

    private final JLabel playStyle = value();
    private final JLabel nextSwitch = value();
    private final JLabel category = value();
    private final JLabel activity = value();
    private final JLabel intensity = value();
    private final JLabel busy = value();

    AntibanStatusHeader()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.CARD);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, AntibanUi.BACKGROUND),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        add(titleRow());
        add(AntibanUi.gap(8));
        add(grid());
    }

    private JPanel titleRow()
    {
        JLabel title = new JLabel("ANTIBAN");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(AntibanUi.TEXT);

        state.setFont(AntibanUi.small());
        state.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(AntibanUi.CARD);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(title, BorderLayout.WEST);
        row.add(state, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    /**
     * Six live values in two columns. A grid rather than a stack of sentences:
     * the labels are fixed and the values change, so lining the values up makes
     * a changed one obvious at a glance.
     */
    private JPanel grid()
    {
        JPanel grid = new JPanel(new GridLayout(0, 2, 6, 3));
        grid.setBackground(AntibanUi.CARD);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        grid.add(caption("Play style"));
        grid.add(playStyle);
        grid.add(caption("Switches in"));
        grid.add(nextSwitch);
        grid.add(caption("Category"));
        grid.add(category);
        grid.add(caption("Activity"));
        grid.add(activity);
        grid.add(caption("Intensity"));
        grid.add(intensity);
        grid.add(caption("Busy"));
        grid.add(busy);

        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, grid.getPreferredSize().height));
        return grid;
    }

    private static JLabel caption(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(AntibanUi.small());
        label.setForeground(AntibanUi.TEXT_DIM);
        return label;
    }

    private static JLabel value()
    {
        JLabel label = new JLabel(AntibanUi.NO_VALUE);
        label.setFont(AntibanUi.small());
        label.setForeground(AntibanUi.TEXT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    /**
     * Called on the plugin's 600ms refresh, so it only ever sets text on labels
     * that already exist.
     */
    void refresh()
    {
        boolean enabled = Rs2AntibanSettings.antibanEnabled;
        state.setText(enabled ? "● ACTIVE" : "○ OFF");
        state.setForeground(enabled ? AntibanUi.ON : AntibanUi.OFF);

        // Off the login screen every one of these is genuinely unknown rather
        // than zero, and the old panel said "null" six times over.
        if (!ProjectX.isLoggedIn())
        {
            reset();
            return;
        }

        playStyle.setText(Rs2Antiban.getPlayStyle() != null
            ? Rs2Antiban.getPlayStyle().getName()
            : AntibanUi.NO_VALUE);

        nextSwitch.setText(Rs2Antiban.getPlayStyle() != null
            ? String.valueOf(Rs2Antiban.getPlayStyle().getTimeLeftUntilNextSwitch())
            : AntibanUi.NO_VALUE);

        category.setText(Rs2Antiban.getCategory() != null
            ? Rs2Antiban.getCategory().getName()
            : AntibanUi.NO_VALUE);

        activity.setText(Rs2Antiban.getActivity() != null
            ? Rs2Antiban.getActivity().getMethod()
            : AntibanUi.NO_VALUE);

        intensity.setText(Rs2Antiban.getActivityIntensity() != null
            ? Rs2Antiban.getActivityIntensity().getName()
            : AntibanUi.NO_VALUE);

        if (Rs2Antiban.getCategory() == null)
        {
            busy.setText(AntibanUi.NO_VALUE);
            busy.setForeground(AntibanUi.TEXT);
        }
        else
        {
            boolean isBusy = Rs2Antiban.getCategory().isBusy();
            busy.setText(isBusy ? "Yes" : "No");
            busy.setForeground(isBusy ? AntibanUi.ACCENT : AntibanUi.TEXT);
        }
    }

    private void reset()
    {
        for (JLabel label : new JLabel[]{playStyle, nextSwitch, category, activity, intensity, busy})
        {
            label.setText(AntibanUi.NO_VALUE);
            label.setForeground(AntibanUi.TEXT);
        }
    }
}
