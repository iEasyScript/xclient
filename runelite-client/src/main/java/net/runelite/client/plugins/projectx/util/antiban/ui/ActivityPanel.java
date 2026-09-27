package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * How the bot's rhythm varies over a session.
 *
 * <p>Eight toggles arrived as one undifferentiated column, which made it
 * impossible to see that they are really three ideas: the play style that drives
 * everything, the human traits layered on top, and the two anti-fingerprinting
 * options.
 */
public class ActivityPanel extends JPanel
{
    private final JCheckBox usePlayStyle = AntibanUi.toggle("Use play style",
        "Main component of the activity system. Play styles is to simulate different types "
            + "of play styles/attention spans.");

    private final JCheckBox dynamicActivity = AntibanUi.toggle("Detect activity changes",
        "Detects activity changes and adjusts settings accordingly. "
            + "(Required for contextual variability to work)");

    private final JCheckBox dynamicActivityIntensity = AntibanUi.toggle("Dynamic intensity",
        "Simulates dynamic intensity based on the current activity by adjusting the mouse "
            + "speed and accuracy.");

    private final JCheckBox simulateFatigue = AntibanUi.toggle("Simulate fatigue",
        "Simulates fatigue by slowing down the mouse movements the longer the player is "
            + "logged in. (This is barely noticeable to the naked eye)");

    private final JCheckBox simulateAttentionSpan = AntibanUi.toggle("Simulate attention span",
        "Simulates attention span by switching between different play styles. "
            + "(Profile switching must be enabled for this to work)");

    private final JCheckBox useBehavioralVariability = AntibanUi.toggle("Behavioural variability",
        "Randomizes the action cooldown intervals based on the current play style. "
            + "(This is recommended for human-like behavior)");

    private final JCheckBox useNonLinearIntervals = AntibanUi.toggle("Non-linear intervals",
        "Anti-fingerprinting feature. Slightly drifts the action cooldown intervals in the "
            + "current play style to avoid pattern profiling.");

    private final JCheckBox useRandomIntervals = AntibanUi.toggle("Fully random intervals",
        "Randomizes the action cooldown intervals. (Not recommended for human-like behavior)");

    public ActivityPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel driver = AntibanUi.card("Play style");
        driver.add(usePlayStyle);
        driver.add(dynamicActivity);
        driver.add(dynamicActivityIntensity);

        JPanel human = AntibanUi.card("Human traits");
        human.add(simulateFatigue);
        human.add(simulateAttentionSpan);
        human.add(useBehavioralVariability);

        JPanel timing = AntibanUi.card("Timing");
        timing.add(useNonLinearIntervals);
        timing.add(useRandomIntervals);
        timing.add(AntibanUi.hint(
            "Fully random timing is not human timing. Prefer non-linear intervals, which "
                + "drift your existing rhythm instead of replacing it."));

        add(driver);
        add(human);
        add(timing);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        usePlayStyle.addActionListener(e ->
        {
            Rs2AntibanSettings.usePlayStyle = usePlayStyle.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        useRandomIntervals.addActionListener(e ->
        {
            Rs2AntibanSettings.randomIntervals = useRandomIntervals.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        simulateFatigue.addActionListener(e ->
        {
            Rs2AntibanSettings.simulateFatigue = simulateFatigue.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        simulateAttentionSpan.addActionListener(e ->
        {
            Rs2AntibanSettings.simulateAttentionSpan = simulateAttentionSpan.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        useBehavioralVariability.addActionListener(e ->
        {
            Rs2AntibanSettings.behavioralVariability = useBehavioralVariability.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        useNonLinearIntervals.addActionListener(e ->
        {
            Rs2AntibanSettings.nonLinearIntervals = useNonLinearIntervals.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        dynamicActivityIntensity.addActionListener(e ->
        {
            Rs2AntibanSettings.dynamicIntensity = dynamicActivityIntensity.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        dynamicActivity.addActionListener(e ->
        {
            Rs2AntibanSettings.dynamicActivity = dynamicActivity.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
    }

    public void updateValues()
    {
        usePlayStyle.setSelected(Rs2AntibanSettings.usePlayStyle);
        useRandomIntervals.setSelected(Rs2AntibanSettings.randomIntervals);
        simulateFatigue.setSelected(Rs2AntibanSettings.simulateFatigue);
        simulateAttentionSpan.setSelected(Rs2AntibanSettings.simulateAttentionSpan);
        useBehavioralVariability.setSelected(Rs2AntibanSettings.behavioralVariability);
        useNonLinearIntervals.setSelected(Rs2AntibanSettings.nonLinearIntervals);
        dynamicActivityIntensity.setSelected(Rs2AntibanSettings.dynamicIntensity);
        dynamicActivity.setSelected(Rs2AntibanSettings.dynamicActivity);
    }
}
