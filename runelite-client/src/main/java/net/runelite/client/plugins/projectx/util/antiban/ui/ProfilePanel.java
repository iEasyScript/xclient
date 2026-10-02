package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.projectx.util.antiban.TimeOfDay;
import net.runelite.client.plugins.projectx.util.antiban.enums.PlaySchedule;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Component;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * How play changes over a day: switching play styles, slowing down at night, and when to play at all.
 *
 * <p>Two of these used to say on their face that they did nothing. Time of day now lengthens cooldowns by
 * the local clock, and the play schedule is the Break Handler's own, edited from here instead of duplicated.
 */
public class ProfilePanel extends JPanel
{
    private static final DateTimeFormatter HOURS = DateTimeFormatter.ofPattern("HH:mm");

    private final JCheckBox enableProfileSwitching = AntibanUi.toggle("Switch play styles",
        "Enables profile(Play style) switching for the antiban system");

    private final JCheckBox adjustForTimeOfDay = AntibanUi.toggle("Slow down at night",
        "Makes action cooldowns longer late at night and early in the morning, by your computer's clock.");

    private final JLabel timeOfDayValue = new JLabel();

    private final JCheckBox usePlaySchedule = AntibanUi.toggle("Only play during schedule",
        "Takes a break outside the chosen hours. Enforced by the Break Handler.");

    private final JComboBox<PlaySchedule> schedule = AntibanUi.combo(
        "The hours to play in", PlaySchedule.values());

    private final JLabel breakHandlerValue = new JLabel();

    public ProfilePanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel switching = AntibanUi.card("Profile switching");
        switching.add(enableProfileSwitching);
        switching.add(AntibanUi.hint(
            "Required by “Simulate attention span” on the Activity tab."));

        JPanel timeOfDay = AntibanUi.card("Time of day");
        timeOfDay.add(adjustForTimeOfDay);
        timeOfDay.add(AntibanUi.readout(new JLabel("Right now"), timeOfDayValue));
        timeOfDay.add(AntibanUi.hint(
            "Normal through the day, a little slower in the evening, up to 45% slower in the "
                + "small hours."));

        schedule.setRenderer(new ScheduleRenderer());
        JPanel playSchedule = AntibanUi.card("Play schedule");
        playSchedule.add(usePlaySchedule);
        playSchedule.add(AntibanUi.gap(2));
        playSchedule.add(schedule);
        playSchedule.add(AntibanUi.readout(new JLabel("Break Handler"), breakHandlerValue));
        playSchedule.add(AntibanUi.hint(
            "These are the Break Handler's own settings. It enforces the schedule, so it has to be on."));

        add(switching);
        add(timeOfDay);
        add(playSchedule);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        AntibanUi.onUserToggle(enableProfileSwitching, on -> Rs2AntibanSettings.profileSwitching = on);
        AntibanUi.onUserToggle(adjustForTimeOfDay, on -> Rs2AntibanSettings.timeOfDayAdjust = on);

        // The schedule belongs to the Break Handler, so these write its config, not the antiban settings.
        usePlaySchedule.addActionListener(e -> BreakHandlerSchedule.setEnabled(usePlaySchedule.isSelected()));
        schedule.addActionListener(e ->
        {
            if (!AntibanUi.isQuiet(schedule) && schedule.getSelectedItem() != null)
            {
                BreakHandlerSchedule.setSchedule((PlaySchedule) schedule.getSelectedItem());
            }
        });
    }

    public void updateValues()
    {
        enableProfileSwitching.setSelected(Rs2AntibanSettings.profileSwitching);
        adjustForTimeOfDay.setSelected(Rs2AntibanSettings.timeOfDayAdjust);

        LocalTime now = LocalTime.now();
        timeOfDayValue.setText(Rs2AntibanSettings.timeOfDayAdjust
            ? TimeOfDay.describe(now) + ", " + AntibanUi.percentSlower(TimeOfDay.multiplier())
            : "Off");

        usePlaySchedule.setSelected(BreakHandlerSchedule.isEnabled());
        AntibanUi.setQuietly(schedule, BreakHandlerSchedule.schedule());
        schedule.setEnabled(usePlaySchedule.isSelected());

        boolean running = BreakHandlerSchedule.isBreakHandlerRunning();
        breakHandlerValue.setText(running ? "On" : "Off");
        breakHandlerValue.setForeground(running || !usePlaySchedule.isSelected() ? AntibanUi.ACCENT : AntibanUi.OFF);
    }

    /** "Medium day  08:00–18:00" rather than the enum's MEDIUM_DAY. */
    private static final class ScheduleRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus)
        {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PlaySchedule)
            {
                PlaySchedule schedule = (PlaySchedule) value;
                String name = schedule.name().replace('_', ' ').toLowerCase();
                setText(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "  "
                    + HOURS.format(schedule.getStartTime()) + "–" + HOURS.format(schedule.getEndTime()));
            }
            setFont(AntibanUi.small());
            return this;
        }
    }
}
