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
        takeMicroBreaks.addActionListener(e ->
        {
            Rs2AntibanSettings.takeMicroBreaks = takeMicroBreaks.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        microBreakDurationLow.addChangeListener(e ->
        {
            Rs2AntibanSettings.microBreakDurationLow = microBreakDurationLow.getValue();
            lowValue.setText(microBreakDurationLow.getValue() + " min");
            if (!microBreakDurationLow.getValueIsAdjusting())
            {
                // Keeps the low bound from overtaking the high one.
                AntibanPlugin.validateAndSetBreakDurations();
                Rs2AntibanSettings.saveToProfile();
            }
        });
        microBreakDurationHigh.addChangeListener(e ->
        {
            Rs2AntibanSettings.microBreakDurationHigh = microBreakDurationHigh.getValue();
            highValue.setText(microBreakDurationHigh.getValue() + " min");
            if (!microBreakDurationHigh.getValueIsAdjusting())
            {
                AntibanPlugin.validateAndSetBreakDurations();
                Rs2AntibanSettings.saveToProfile();
            }
        });
        microBreakChance.addChangeListener(e ->
        {
            Rs2AntibanSettings.microBreakChance = microBreakChance.getValue() / 100.0;
            chanceValue.setText(microBreakChance.getValue() + "%");
            if (!microBreakChance.getValueIsAdjusting())
            {
                Rs2AntibanSettings.saveToProfile();
            }
        });
    }

    public void updateValues()
    {
        AntibanPlugin.validateAndSetBreakDurations();

        AntibanUi.setIndicator(activeIndicator,
            Rs2AntibanSettings.microBreakActive ? "Break running" : "No break running",
            Rs2AntibanSettings.microBreakActive);

        takeMicroBreaks.setSelected(Rs2AntibanSettings.takeMicroBreaks);

        microBreakChance.setValue((int) (Rs2AntibanSettings.microBreakChance * 100));
        chanceValue.setText(microBreakChance.getValue() + "%");

        microBreakDurationLow.setValue(Rs2AntibanSettings.microBreakDurationLow);
        lowValue.setText(microBreakDurationLow.getValue() + " min");

        microBreakDurationHigh.setValue(Rs2AntibanSettings.microBreakDurationHigh);
        highValue.setText(microBreakDurationHigh.getValue() + " min");
    }
}
