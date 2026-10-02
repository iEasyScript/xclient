package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.AntibanPlugin;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Short AFK breaks.
 *
 * <p>"Micro break active" is a readout, not a setting -- the old panel made it a
 * disabled checkbox, which greys out identically to a setting you are not
 * allowed to change. Here it is a lamp that lights while a break is running.
 *
 * <p>Dragging the shortest break past the longest used to throw both back to their defaults of 3 and 15
 * minutes. Now one pushes the other along, the way a range control should.
 */
public class MicroBreakPanel extends JPanel
{
    private final JLabel activeIndicator = AntibanUi.indicator("No break running",
        "Indicator if a micro break is active");

    private final JCheckBox takeMicroBreaks = AntibanUi.toggle("Take micro breaks",
        "Micro breaks are short breaks that are taken at random intervals to simulate afk behavior");

    private final JLabel chanceValue = new JLabel();
    private final JSlider microBreakChance =
        AntibanUi.slider(0, 100, (int) (Rs2AntibanSettings.microBreakChance * 100));

    private final JLabel lowValue = new JLabel();
    private final JSlider microBreakDurationLow =
        AntibanUi.slider(1, 10, Rs2AntibanSettings.microBreakDurationLow);

    private final JLabel highValue = new JLabel();
    private final JSlider microBreakDurationHigh =
        AntibanUi.slider(1, 30, Rs2AntibanSettings.microBreakDurationHigh);

    public MicroBreakPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel status = AntibanUi.card("Now");
        status.add(activeIndicator);

        JPanel behaviour = AntibanUi.card("Breaks");
        behaviour.add(takeMicroBreaks);
        behaviour.add(AntibanUi.readout(new JLabel("Chance"), chanceValue));
        behaviour.add(microBreakChance);

        JPanel duration = AntibanUi.card("Duration");
        duration.add(AntibanUi.readout(new JLabel("Shortest"), lowValue));
        duration.add(microBreakDurationLow);
        duration.add(AntibanUi.readout(new JLabel("Longest"), highValue));
        duration.add(microBreakDurationHigh);
        duration.add(AntibanUi.hint("A break lasts somewhere between these two."));

        microBreakChance.setToolTipText("The chance of taking a micro break");
        microBreakDurationLow.setToolTipText("The minimum duration of a micro break");
        microBreakDurationHigh.setToolTipText("The maximum duration of a micro break");

        add(status);
        add(behaviour);
        add(duration);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        AntibanUi.onUserToggle(takeMicroBreaks, on -> Rs2AntibanSettings.takeMicroBreaks = on);

        AntibanUi.onUserSlide(microBreakDurationLow,
            value -> lowValue.setText(value + " min"),
            value -> Rs2AntibanSettings.userChange(() ->
            {
                Rs2AntibanSettings.microBreakDurationLow = value;
                if (Rs2AntibanSettings.microBreakDurationHigh < value)
                {
                    Rs2AntibanSettings.microBreakDurationHigh = value;
                }
            }));
        AntibanUi.onUserSlide(microBreakDurationHigh,
            value -> highValue.setText(value + " min"),
            value -> Rs2AntibanSettings.userChange(() ->
            {
                Rs2AntibanSettings.microBreakDurationHigh = value;
                if (Rs2AntibanSettings.microBreakDurationLow > value)
                {
                    Rs2AntibanSettings.microBreakDurationLow = value;
                }
            }));
        AntibanUi.onUserSlide(microBreakChance,
            value -> chanceValue.setText(value + "%"),
            value -> Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.microBreakChance = value / 100.0));
    }

    public void updateValues()
    {
        AntibanPlugin.validateAndSetBreakDurations();

        AntibanUi.setIndicator(activeIndicator,
            Rs2AntibanSettings.microBreakActive ? "Break running" : "No break running",
            Rs2AntibanSettings.microBreakActive);

        takeMicroBreaks.setSelected(Rs2AntibanSettings.takeMicroBreaks);

        AntibanUi.setQuietly(microBreakChance, (int) Math.round(Rs2AntibanSettings.microBreakChance * 100));
        chanceValue.setText(microBreakChance.getValue() + "%");

        AntibanUi.setQuietly(microBreakDurationLow, Rs2AntibanSettings.microBreakDurationLow);
        lowValue.setText(microBreakDurationLow.getValue() + " min");

        AntibanUi.setQuietly(microBreakDurationHigh, Rs2AntibanSettings.microBreakDurationHigh);
        highValue.setText(microBreakDurationHigh.getValue() + " min");
    }
}
