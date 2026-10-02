package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.AntibanPlugin;
import net.runelite.client.plugins.projectx.util.antiban.AntibanPreset;
import net.runelite.client.plugins.projectx.util.antiban.MouseFatigue;
import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.Duration;
import java.time.Instant;

/**
 * The switches that decide whether antiban runs at all, whose settings are in use, a quick way to set every
 * tab at once, and what antiban has done this session.
 *
 * <p>"Contextual variability" used to be here. Nothing read it -- not universal antiban, despite its tooltip
 * -- so it is gone from the panel; the field stays because scripts set it.
 */
public class GeneralPanel extends JPanel
{
    private final JLabel owner = AntibanUi.indicator("Using your settings",
        "Whether the values on these tabs are yours or a running script's");

    private final JButton restore = AntibanUi.button("Use my settings again",
        "Puts your settings back over the running script's. The script may set its own again.");

    private final JCheckBox isEnabled = AntibanUi.toggle("Enabled",
        "Enable the antiban system");

    private final JCheckBox universalAntiban = AntibanUi.toggle("Universal antiban",
        "Takes action cooldowns and micro breaks on behalf of scripts that have no antiban of their "
            + "own, triggered by your XP drops. Turn on Detect activity changes on the Activity tab "
            + "so it knows what you are doing.");

    private final JComboBox<AntibanPreset> preset = AntibanUi.combo(
        "Light, Balanced and Cautious are general strengths. The \"Tuned for\" entries are the "
            + "templates scripts use for that skill.",
        AntibanPreset.values());

    private final JButton applyPreset = AntibanUi.button("Apply preset",
        "Replaces the settings on the other tabs with the chosen preset");

    private final JLabel sessionLength = new JLabel();
    private final JLabel cooldowns = new JLabel();
    private final JLabel microBreaks = new JLabel();
    private final JLabel fatigue = new JLabel();

    private final JCheckBox devDebug = AntibanUi.toggle("Dev debug",
        "Enable debug messages for the antiban system");

    private final JCheckBox overwriteScriptSetting = AntibanUi.toggle("Override every script",
        "Your settings win over every script's. Anything a script changes is put back within a "
            + "game tick. Scripts still choose the activity.");

    public GeneralPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel system = AntibanUi.card("System");
        system.add(isEnabled);
        system.add(universalAntiban);
        system.add(AntibanUi.gap(6));
        system.add(owner);
        system.add(restore);
        system.add(AntibanUi.hint(
            "Scripts set their own values when they start. Yours come back when the script stops."));

        preset.setSelectedItem(AntibanPreset.BALANCED);
        JPanel presets = AntibanUi.card("Presets");
        presets.add(preset);
        presets.add(AntibanUi.gap(4));
        presets.add(applyPreset);
        presets.add(AntibanUi.hint(
            "Sets every other tab at once. Universal antiban, the override and dev debug are left "
                + "as they are, except by the Universal preset."));

        JPanel session = AntibanUi.card("This session");
        session.add(AntibanUi.readout(new JLabel("Logged in for"), sessionLength));
        session.add(AntibanUi.readout(new JLabel("Action cooldowns"), cooldowns));
        session.add(AntibanUi.readout(new JLabel("Micro breaks"), microBreaks));
        session.add(AntibanUi.readout(new JLabel("Mouse fatigue"), fatigue));

        JPanel advanced = AntibanUi.card("Advanced");
        advanced.add(devDebug);
        advanced.add(overwriteScriptSetting);
        advanced.add(AntibanUi.hint(
            "Overriding forces your settings onto scripts that ship their own. Leave it off unless "
                + "you know the script you are running."));

        add(system);
        add(presets);
        add(session);
        add(advanced);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        AntibanUi.onUserToggle(isEnabled, on -> Rs2AntibanSettings.antibanEnabled = on);
        AntibanUi.onUserToggle(universalAntiban, on -> Rs2AntibanSettings.universalAntiban = on);
        AntibanUi.onUserToggle(devDebug, on -> Rs2AntibanSettings.devDebug = on);
        AntibanUi.onUserToggle(overwriteScriptSetting, on -> Rs2AntibanSettings.overwriteScriptSettings = on);

        restore.addActionListener(e -> Rs2AntibanSettings.restoreUserSettings());

        applyPreset.addActionListener(e ->
        {
            AntibanPreset chosen = (AntibanPreset) preset.getSelectedItem();
            if (chosen != null)
            {
                Rs2AntibanSettings.userChange(chosen::apply);
                AntibanPlugin.validateAndSetBreakDurations();
            }
        });
    }

    public void updateValues()
    {
        isEnabled.setSelected(Rs2AntibanSettings.antibanEnabled);
        universalAntiban.setSelected(Rs2AntibanSettings.universalAntiban);
        devDebug.setSelected(Rs2AntibanSettings.devDebug);
        overwriteScriptSetting.setSelected(Rs2AntibanSettings.overwriteScriptSettings);

        boolean overriding = Rs2AntibanSettings.isOverriding();
        boolean scriptControlled = Rs2AntibanSettings.isScriptControlled();
        if (overriding)
        {
            AntibanUi.setIndicator(owner, "Your settings, forced on scripts", true);
        }
        else if (scriptControlled)
        {
            AntibanUi.setIndicator(owner, "A script's settings are in use", false);
            // Unlit, but this is the state worth noticing; dim grey would hide it.
            owner.setForeground(AntibanUi.ACCENT);
        }
        else
        {
            AntibanUi.setIndicator(owner, "Using your settings", true);
        }
        // Hidden rather than disabled: a disabled button looks the same as an enabled one in this theme.
        restore.setVisible(scriptControlled && !overriding);

        Instant start = AntibanPlugin.getSessionStart();
        sessionLength.setText(start == null ? AntibanUi.NO_VALUE : formatDuration(Duration.between(start, Instant.now())));
        cooldowns.setText(String.valueOf(Rs2Antiban.getCooldownsThisSession()));
        microBreaks.setText(String.valueOf(Rs2Antiban.getMicroBreaksThisSession()));
        fatigue.setText(Rs2AntibanSettings.simulateFatigue
            ? AntibanUi.percentSlower(MouseFatigue.currentMultiplier())
            : "Off");
    }

    private static String formatDuration(Duration duration)
    {
        long minutes = duration.toMinutes();
        return minutes < 60
            ? minutes + "m"
            : String.format("%dh %02dm", minutes / 60, minutes % 60);
    }
}
