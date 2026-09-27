package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Component;

/**
 * The switches that decide whether antiban runs at all, split into what a normal
 * user touches and what only a script author should.
 */
public class GeneralPanel extends JPanel
{
    private final JCheckBox isEnabled = AntibanUi.toggle("Enabled",
        "Enable the antiban system");

    private final JCheckBox universalAntiban = AntibanUi.toggle("Universal antiban",
        "Only enable universal antiban for plugins that hasn't implemented antiban");

    private final JCheckBox useContextualVariability = AntibanUi.toggle("Contextual variability",
        "Adjusts antiban behaviors based on the context of the players actions/activity. "
            + "This is required for the universal antiban to work properly. "
            + "Also vital for plugins that switch between different activities.");

    private final JCheckBox devDebug = AntibanUi.toggle("Dev debug",
        "Enable debug messages for the antiban system");

    private final JCheckBox overwriteScriptSetting = AntibanUi.toggle("Override every script",
        "This is a dangerous setting and should be used with caution. It will apply the "
            + "settings to all scripts, even if they have their own settings.");

    private final JButton universalAntibanSettings = new JButton("Apply universal setup");

    public GeneralPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(AntibanUi.BACKGROUND);

        JPanel system = AntibanUi.card("System");
        system.add(isEnabled);
        system.add(universalAntiban);
        system.add(useContextualVariability);

        universalAntibanSettings.setFont(AntibanUi.small());
        universalAntibanSettings.setForeground(AntibanUi.TEXT);
        universalAntibanSettings.setBackground(AntibanUi.DIVIDER);
        universalAntibanSettings.setFocusPainted(false);
        universalAntibanSettings.setAlignmentX(Component.LEFT_ALIGNMENT);
        universalAntibanSettings.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        universalAntibanSettings.setToolTipText(
            "Setups the universal antiban settings for plugins that hasn't implemented antiban");
        AntibanUi.constrainHeight(universalAntibanSettings);

        system.add(AntibanUi.gap(6));
        system.add(universalAntibanSettings);

        JPanel advanced = AntibanUi.card("Advanced");
        advanced.add(devDebug);
        advanced.add(overwriteScriptSetting);
        advanced.add(AntibanUi.hint(
            "Overriding forces these settings onto scripts that ship their own. "
                + "Leave it off unless you know the script you are running."));

        add(system);
        add(advanced);

        setupActionListeners();
    }

    private void setupActionListeners()
    {
        isEnabled.addActionListener(e ->
        {
            Rs2AntibanSettings.antibanEnabled = isEnabled.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        universalAntiban.addActionListener(e ->
        {
            Rs2AntibanSettings.universalAntiban = universalAntiban.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        useContextualVariability.addActionListener(e ->
        {
            Rs2AntibanSettings.contextualVariability = useContextualVariability.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        devDebug.addActionListener(e ->
        {
            Rs2AntibanSettings.devDebug = devDebug.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        overwriteScriptSetting.addActionListener(e ->
        {
            Rs2AntibanSettings.overwriteScriptSettings = overwriteScriptSetting.isSelected();
            Rs2AntibanSettings.saveToProfile();
        });
        universalAntibanSettings.addActionListener(e ->
        {
            Rs2Antiban.antibanSetupTemplates.applyUniversalAntibanSetup();
            Rs2AntibanSettings.saveToProfile();
        });
    }

    public void updateValues()
    {
        isEnabled.setSelected(Rs2AntibanSettings.antibanEnabled);
        universalAntiban.setSelected(Rs2AntibanSettings.universalAntiban);
        useContextualVariability.setSelected(Rs2AntibanSettings.contextualVariability);
        devDebug.setSelected(Rs2AntibanSettings.devDebug);
        overwriteScriptSetting.setSelected(Rs2AntibanSettings.overwriteScriptSettings);
    }
}
