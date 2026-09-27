package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

/**
 * Switching between play styles over the course of a day.
 *
 * <p>Two of these three are honestly labelled as unfinished in their tooltips.
 * That belongs on the face of the panel rather than hidden in a hover, so the
 * unimplemented ones say so where you can see them.
 */
public class ProfilePanel extends JPanel
{
    private final JCheckBox enableProfileSwitching = AntibanUi.toggle("Switch play styles",
        "Enables profile(Play style) switching for the antiban system");

    private final JCheckBox adjustForTimeOfDay = AntibanUi.toggle("Adjust for time of day",
        "Adjusts the antiban behavior based on the time of day. (Not fully implemented)");

    private final JCheckBox simulatePlaySchedule = AntibanUi.toggle("Simulate play schedule",
        "(Not implemented!) Simulates a play schedule by switching between different "
            + "activities at different times of the day and only during certain days of the week.");

    public ProfilePanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel switching = AntibanUi.card("Profile switching");
        switching.add(enableProfileSwitching);
        switching.add(AntibanUi.hint(
            "Required by “Simulate attention span” on the Activity tab."));

        JPanel scheduling = AntibanUi.card("Scheduling");
        scheduling.add(adjustForTimeOfDay);
        scheduling.add(simulatePlaySchedule);
        scheduling.add(AntibanUi.hint(
            "Time of day is only partly wired up, and play schedules do nothing yet. "
                + "Both are here because the settings persist, not because they work."));

        add(switching);
        add(scheduling);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        enableProfileSwitching.addActionListener(e ->
        {
            Rs2AntibanSettings.profileSwitching = enableProfileSwitching.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        adjustForTimeOfDay.addActionListener(e ->
        {
            Rs2AntibanSettings.timeOfDayAdjust = adjustForTimeOfDay.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        simulatePlaySchedule.addActionListener(e ->
        {
            Rs2AntibanSettings.playSchedule = simulatePlaySchedule.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
    }

    public void updateValues()
    {
        enableProfileSwitching.setSelected(Rs2AntibanSettings.profileSwitching);
        adjustForTimeOfDay.setSelected(Rs2AntibanSettings.timeOfDayAdjust);
        simulatePlaySchedule.setSelected(Rs2AntibanSettings.playSchedule);
    }
}
