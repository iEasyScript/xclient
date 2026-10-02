package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.projectx.util.antiban.TimeOfDay;
import net.runelite.client.plugins.projectx.util.antiban.enums.PlayStyle;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;

/**
 * The pause the bot takes after an action, and how likely it is to take one.
 *
 * <p>As on the Breaks tab, whether a cooldown is running right now is a readout rather than a checkbox you
 * are forbidden to tick. The same went for "Timeout", which was drawn as a slider but was the live countdown
 * of the running pause: dragging it cut the current pause short and set nothing. It is a progress bar now,
 * and the length a pause will actually have is shown beside it.
 */
public class CooldownPanel extends JPanel
{
    private final JLabel activeIndicator = AntibanUi.indicator("Not on cooldown",
        "Displays if the action cooldown is active");

    private final JProgressBar remaining = AntibanUi.progress("Ticks left in the running pause");

    private final JLabel chanceValue = new JLabel();
    private final JSlider actionCooldownChance =
        AntibanUi.slider(0, 100, (int) Math.round(Rs2AntibanSettings.actionCooldownChance * 100));

    private final JLabel lengthValue = new JLabel();
    private final JLabel timeOfDayValue = new JLabel();

    public CooldownPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel status = AntibanUi.card("Now");
        status.add(activeIndicator);
        status.add(remaining);

        JPanel settings = AntibanUi.card("Action cooldown");
        settings.add(AntibanUi.readout(new JLabel("Chance"), chanceValue));
        settings.add(actionCooldownChance);
        settings.add(AntibanUi.gap(4));
        settings.add(AntibanUi.readout(new JLabel("Pause length"), lengthValue));
        settings.add(AntibanUi.readout(new JLabel("Time of day"), timeOfDayValue));
        settings.add(AntibanUi.hint(
            "The length comes from the play style on the Activity tab, stretched at night if "
                + "“Slow down at night” is on. One game tick is 0.6 seconds."));

        actionCooldownChance.setToolTipText("Chance to activate the action cooldown in percent");

        add(status);
        add(settings);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        AntibanUi.onUserSlide(actionCooldownChance,
            value -> chanceValue.setText(value + "%"),
            value -> Rs2AntibanSettings.userChange(() -> Rs2AntibanSettings.actionCooldownChance = value / 100.0));
    }

    public void updateValues()
    {
        boolean active = Rs2AntibanSettings.actionCooldownActive;
        AntibanUi.setIndicator(activeIndicator, active ? "Cooling down" : "Not on cooldown", active);

        int length = Math.max(1, Rs2Antiban.getLastCooldownLength());
        int left = active ? Math.min(Rs2Antiban.getTIMEOUT(), length) : 0;
        remaining.setMaximum(length);
        remaining.setValue(left);
        remaining.setString(active ? left + " of " + length + " ticks" : "Idle");

        AntibanUi.setQuietly(actionCooldownChance, (int) Math.round(Rs2AntibanSettings.actionCooldownChance * 100));
        chanceValue.setText(actionCooldownChance.getValue() + "%");

        PlayStyle style = Rs2Antiban.getPlayStyle();
        if (style == null)
        {
            lengthValue.setText(AntibanUi.NO_VALUE);
        }
        else if (Rs2AntibanSettings.behavioralVariability)
        {
            int low = Math.min(style.getPrimaryTickInterval(), style.getSecondaryTickInterval());
            int high = Math.max(style.getPrimaryTickInterval(), style.getSecondaryTickInterval());
            lengthValue.setText(low + "–" + high + " ticks");
        }
        else
        {
            lengthValue.setText(style.getPrimaryTickInterval() + " ticks");
        }

        timeOfDayValue.setText(Rs2AntibanSettings.timeOfDayAdjust
            ? AntibanUi.percentSlower(TimeOfDay.multiplier())
            : "Off");
    }
}
