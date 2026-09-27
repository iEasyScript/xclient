package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * The pause the bot takes after an action, and how likely it is to take one.
 *
 * <p>As on the Breaks tab, whether a cooldown is running right now is a readout
 * rather than a checkbox you are forbidden to tick.
 */
public class CooldownPanel extends JPanel
{
    private final JLabel activeIndicator = AntibanUi.indicator("Not on cooldown",
        "Displays if the action cooldown is active");

    private final JLabel chanceValue = new JLabel();
    private final JSlider actionCooldownChance =
        AntibanUi.slider(0, 100, (int) (Rs2AntibanSettings.actionCooldownChance * 100));

    private final JLabel timeoutValue = new JLabel();
    private final JSlider timeout = AntibanUi.slider(0, 60, Rs2Antiban.getTIMEOUT());

    public CooldownPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel status = AntibanUi.card("Now");
        status.add(activeIndicator);

        JPanel settings = AntibanUi.card("Action cooldown");
        settings.add(AntibanUi.readout(new JLabel("Chance"), chanceValue));
        settings.add(actionCooldownChance);
        settings.add(AntibanUi.gap(4));
        settings.add(AntibanUi.readout(new JLabel("Timeout"), timeoutValue));
        settings.add(timeout);
        settings.add(AntibanUi.hint(
            "One game tick is 0.6 seconds, so 10 ticks is about six seconds of pause."));

        actionCooldownChance.setToolTipText("Chance to activate the action cooldown in percent");
        timeout.setToolTipText("The amount of ticks left before the action cooldown is deactivated");

        add(status);
        add(settings);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        actionCooldownChance.addChangeListener(e ->
        {
            Rs2AntibanSettings.actionCooldownChance = actionCooldownChance.getValue() / 100.0;
            chanceValue.setText(actionCooldownChance.getValue() + "%");
            if (!actionCooldownChance.getValueIsAdjusting())
            {
                Rs2AntibanSettings.saveToProfile();
            }
        });
        timeout.addChangeListener(e ->
        {
            Rs2Antiban.setTIMEOUT(timeout.getValue());
            timeoutValue.setText(timeout.getValue() + " ticks");
        });
    }

    public void updateValues()
    {
        AntibanUi.setIndicator(activeIndicator,
            Rs2AntibanSettings.actionCooldownActive ? "Cooling down" : "Not on cooldown",
            Rs2AntibanSettings.actionCooldownActive);

        actionCooldownChance.setValue((int) (Rs2AntibanSettings.actionCooldownChance * 100));
        chanceValue.setText(actionCooldownChance.getValue() + "%");

        timeout.setValue(Rs2Antiban.getTIMEOUT());
        timeoutValue.setText(timeout.getValue() + " ticks");
    }
}
