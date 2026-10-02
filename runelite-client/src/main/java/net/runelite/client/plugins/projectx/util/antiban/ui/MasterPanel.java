package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.plugins.projectx.util.antiban.AntibanPlugin;
import net.runelite.client.plugins.projectx.util.antiban.Rs2AntibanSettings;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;

/**
 * The antiban panel: what the system is doing, and every setting that shapes it.
 *
 * <p>The layout is three fixed parts and one scrolling one. The header reports
 * live state, the tabs pick a section, the chosen section scrolls in the middle,
 * and Reset stays reachable at the bottom. Nothing is centred in dead space and
 * nothing is animated -- the previous version spent roughly a third of its
 * height on a banner and a bouncing GIF, which pushed the six settings that
 * matter below the fold on a short sidebar.
 *
 * <p>Refreshed wholesale every 600ms by {@code AntibanPlugin}'s timer, so
 * {@link #loadSettings()} only ever updates the text and selection of widgets
 * that already exist.
 */
public class MasterPanel extends PluginPanel
{
    private static final String GENERAL = "General";
    private static final String ACTIVITY = "Activity";
    private static final String PROFILE = "Profile";
    private static final String MOUSE = "Mouse";
    private static final String BREAKS = "Breaks";
    private static final String COOLDOWN = "Cooldown";

    private final AntibanStatusHeader header = new AntibanStatusHeader();

    private final GeneralPanel generalPanel = new GeneralPanel();
    private final ActivityPanel activityPanel = new ActivityPanel();
    private final ProfilePanel profilePanel = new ProfilePanel();
    private final MousePanel mousePanel = new MousePanel();
    private final MicroBreakPanel microBreakPanel = new MicroBreakPanel();
    private final CooldownPanel cooldownPanel = new CooldownPanel();

    private final JPanel sections = new JPanel(new CardLayout());

    public MasterPanel()
    {
        // The default PluginPanel wrapper adds its own scroll pane and padding;
        // this panel manages its own, so the header and tabs can stay put while
        // only the section scrolls.
        super(false);

        setLayout(new BorderLayout());
        setBackground(AntibanUi.BACKGROUND);

        sections.setBackground(AntibanUi.BACKGROUND);
        sections.add(wrap(generalPanel), GENERAL);
        sections.add(wrap(activityPanel), ACTIVITY);
        sections.add(wrap(profilePanel), PROFILE);
        sections.add(wrap(mousePanel), MOUSE);
        sections.add(wrap(microBreakPanel), BREAKS);
        sections.add(wrap(cooldownPanel), COOLDOWN);

        AntibanTabs tabs = new AntibanTabs(
            name -> ((CardLayout) sections.getLayout()).show(sections, name),
            GENERAL, ACTIVITY, PROFILE, MOUSE, BREAKS, COOLDOWN);

        // Both must share one alignment. The header's children are left-aligned, which makes the header
        // itself report 0.0, while the tabs report the default 0.5; BoxLayout lines mixed alignments up on
        // a common axis, which pushed the header 84px right and left a dead strip down its left side.
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(AntibanUi.BACKGROUND);
        top.add(header);
        top.add(tabs);

        JScrollPane scroller = new JScrollPane(sections,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setBackground(AntibanUi.BACKGROUND);
        scroller.getViewport().setBackground(AntibanUi.BACKGROUND);
        scroller.getVerticalScrollBar().setUnitIncrement(16);

        add(top, BorderLayout.NORTH);
        add(scroller, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        tabs.select(GENERAL);
        loadSettings();
    }

    /**
     * Pins a section to the top of its scroll area. Without this a short section
     * is centred by the CardLayout and its controls float in the middle of the
     * panel, which is the specific thing that made the old General tab look
     * broken.
     */
    private static JPanel wrap(JPanel section)
    {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(AntibanUi.BACKGROUND);
        wrapper.add(section, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel buildFooter()
    {
        JButton reset = new JButton("Reset all antiban settings");
        reset.setFont(AntibanUi.small());
        reset.setForeground(AntibanUi.TEXT);
        reset.setBackground(AntibanUi.CARD);
        reset.setFocusPainted(false);
        reset.setBorder(BorderFactory.createEmptyBorder(7, 8, 7, 8));
        reset.setToolTipText("Puts every setting on every tab back to its default, the override included.");
        reset.addActionListener(e ->
        {
            // Settings only: the running activity and play style are a script's working state, and
            // clearing them mid-run left scripts with no play style to take a cooldown from.
            Rs2AntibanSettings.userChange(Rs2AntibanSettings::resetEverything);
            AntibanPlugin.validateAndSetBreakDurations();
            loadSettings();
        });

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(AntibanUi.BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        footer.add(reset, BorderLayout.CENTER);
        footer.setMaximumSize(new Dimension(Integer.MAX_VALUE, footer.getPreferredSize().height));
        return footer;
    }

    /**
     * Pulls every control back into line with the settings object.
     *
     * <p>Called on a timer, so the settings may have been changed by a script
     * rather than by the user -- the panel is a view of {@link Rs2AntibanSettings},
     * never the owner of it.
     */
    public void loadSettings()
    {
        generalPanel.updateValues();
        activityPanel.updateValues();
        profilePanel.updateValues();
        mousePanel.updateValues();
        microBreakPanel.updateValues();
        cooldownPanel.updateValues();

        header.refresh();
    }
}
