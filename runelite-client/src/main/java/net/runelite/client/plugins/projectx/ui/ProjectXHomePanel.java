package net.runelite.client.plugins.projectx.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXAccount;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXEntitlements;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManifest;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXSite;
import net.runelite.client.plugins.projectx.scriptqueue.ScriptQueuePlugin;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The first tab: who you are, your X Tokens, the scripts you have and whether they
 * are running, and the way to everything else Project X does. What makes the client
 * Project X rather than a plugin list.
 *
 * <p>Refreshes while it is showing and not otherwise; the account is re-read off the
 * EDT at most once a minute (the cache in {@link ProjectXAccount} decides).
 */
@Slf4j
public class ProjectXHomePanel extends PluginPanel
{
    private static final int REFRESH_MS = 2000;

    private final PluginManager pluginManager;
    private final ProjectXPluginManager projectxPluginManager;
    private final ProjectXPluginListPanel pluginListPanel;
    private final ProjectXEntitlements entitlements;
    private final ProjectXAccount account;
    private final ScheduledExecutorService executor;

    private final JLabel signedIn = new JLabel();
    private final JLabel balance = new JLabel();
    private final MyScriptsWindow myScripts;
    private final JLabel scriptCount = new JLabel();
    private final Timer timer;

    /** Opens the Scripts tab; set by whoever owns it. */
    private Runnable openScripts = () -> { };

    @Inject
    ProjectXHomePanel(PluginManager pluginManager, ProjectXPluginManager projectxPluginManager,
                      ProjectXPluginListPanel pluginListPanel, ProjectXEntitlements entitlements,
                      ProjectXAccount account, ScheduledExecutorService executor)
    {
        super(true);
        this.pluginManager = pluginManager;
        this.projectxPluginManager = projectxPluginManager;
        this.pluginListPanel = pluginListPanel;
        this.entitlements = entitlements;
        this.account = account;
        this.executor = executor;
        this.myScripts = new MyScriptsWindow(pluginManager, projectxPluginManager, pluginListPanel, entitlements);

        // The sidebar's own grey (obsidian, via ColorScheme), so the tab and the space below it are one.
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(left(header()));
        add(gap(12));
        add(left(balanceCard()));
        add(gap(14));

        add(left(eyebrow("Project X")));
        add(gap(6));
        JPanel actions = new JPanel(new GridLayout(0, 2, 6, 6));
        actions.setOpaque(false);
        actions.add(button("Scripts", false, () -> openScripts.run()));
        actions.add(button("Script Queue", false, () ->
        {
            if (!ScriptQueuePlugin.open())
            {
                openScripts.run();
            }
        }));
        actions.add(button("Store", false, () -> browse(ProjectXSite.store())));
        actions.add(button("Control Panel", false, () -> browse(ProjectXSite.controlPanel())));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        add(left(actions));
        add(gap(6));
        JButton myScriptsButton = button("My Scripts", true, () -> myScripts.show(this));
        myScriptsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        myScriptsButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        add(myScriptsButton);
        add(gap(4));
        scriptCount.setFont(FontManager.getRunescapeSmallFont());
        scriptCount.setForeground(ProjectXTheme.INK_FAINT);
        add(left(scriptCount));
        add(gap(8));
        JLabel footer = new JLabel("Project X " + Objects.toString(RuneLiteProperties.getProjectXVersion(), "") + "  ·  xclient.dev");
        footer.setFont(FontManager.getRunescapeSmallFont());
        footer.setForeground(ProjectXTheme.INK_FAINT);
        footer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footer.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                browse(ProjectXSite.home());
            }
        });
        add(left(footer));
        add(gap(16));

        timer = new Timer(REFRESH_MS, e -> refresh());
        timer.setInitialDelay(0);
        refresh();
    }

    /** Wires the Scripts button to the Scripts tab. */
    public void setOpenScripts(Runnable openScripts)
    {
        this.openScripts = openScripts == null ? () -> { } : openScripts;
    }

    @Override
    public void onActivate()
    {
        timer.restart();
    }

    @Override
    public void onDeactivate()
    {
        timer.stop();
    }

    // ---- the pieces

    private JPanel header()
    {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        BufferedImage crest = ImageUtil.loadImageResource(ClientUI.class, "projectx_splash.png");
        header.add(new JLabel(new ImageIcon(ImageUtil.resizeImage(crest, 42, 42))), BorderLayout.WEST);

        JPanel words = new JPanel();
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.setOpaque(false);
        JLabel title = new JLabel("PROJECT X");
        title.setFont(FontManager.getRunescapeBoldFont().deriveFont(20f));
        title.setForeground(ProjectXTheme.GOLD_HI);
        words.add(title);
        signedIn.setFont(FontManager.getRunescapeSmallFont());
        signedIn.setForeground(ProjectXTheme.INK_DIM);
        words.add(signedIn);
        header.add(words, BorderLayout.CENTER);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        return header;
    }

    private JPanel balanceCard()
    {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBackground(ProjectXTheme.OBSIDIAN_3);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, ProjectXTheme.GOLD_DEEP),
            BorderFactory.createEmptyBorder(8, 10, 8, 8)));

        JPanel words = new JPanel();
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.setOpaque(false);
        JLabel label = new JLabel("X TOKENS");
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(ProjectXTheme.GOLD_DIM);
        words.add(label);
        balance.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
        balance.setForeground(ProjectXTheme.MOLTEN_HI);
        words.add(balance);
        card.add(words, BorderLayout.CENTER);

        JButton topUp = button("Top up", true, () -> browse(ProjectXSite.tokens()));
        JPanel holder = new JPanel(new GridLayout(1, 1));
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        holder.add(topUp);
        card.add(holder, BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        return card;
    }

    // ---- refreshing

    private void refresh()
    {
        updateAccount(account.getIdentity());
        executor.submit(() ->
        {
            try
            {
                ProjectXAccount.Identity identity = account.refreshIfStale();
                SwingUtilities.invokeLater(() -> updateAccount(identity));
            }
            catch (Exception e)
            {
                log.debug("Home tab could not refresh the account", e);
            }
        });
        updateScripts();
    }

    private void updateAccount(ProjectXAccount.Identity identity)
    {
        if (identity == null)
        {
            signedIn.setText(ProjectXAccount.token() == null ? "Not signed in" : "Connecting…");
            balance.setText("—");
            return;
        }
        signedIn.setText("Signed in as " + identity.displayName());
        Integer cents = identity.balanceCents();
        balance.setText(cents == null ? "—" : ProjectXSite.formatX(cents));
    }

    private void updateScripts()
    {
        String summary = myScripts.summary();
        if (!summary.equals(scriptCount.getText()))
        {
            scriptCount.setText(summary);
        }
    }

    // ---- small helpers

    /** "3d 4h", "5h 20m", "12m". */
    static String timeLeft(Instant ends)
    {
        long minutes = Math.max(0, Duration.between(Instant.now(), ends).toMinutes());
        long d = minutes / 1440;
        long h = (minutes % 1440) / 60;
        long m = minutes % 60;
        if (d > 0)
        {
            return h > 0 ? d + "d " + h + "h" : d + "d";
        }
        if (h > 0)
        {
            return m > 0 ? h + "h " + m + "m" : h + "h";
        }
        return Math.max(1, m) + "m";
    }

    private static String descriptorName(Plugin plugin)
    {
        PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
        return d == null ? plugin.getClass().getSimpleName() : d.name().replaceAll("<[^>]*>", "").trim();
    }

    /** Opens one of the site's OSRS pages; see {@link ProjectXSite} for why they all name the game. */
    private static void browse(okhttp3.HttpUrl url)
    {
        LinkBrowser.browse(url.toString());
    }

    static JPanel card()
    {
        JPanel card = new JPanel();
        card.setBackground(ProjectXTheme.OBSIDIAN_2);
        card.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 6));
        return card;
    }

    private static JLabel eyebrow(String text)
    {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(FontManager.getRunescapeSmallFont());
        label.setForeground(ProjectXTheme.GOLD);
        return label;
    }

    /** A gold (primary) or ghost button in the site's style. */
    static JButton button(String text, boolean primary, Runnable action)
    {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setForeground(primary ? ProjectXTheme.OBSIDIAN_0 : ProjectXTheme.GOLD_HI);
        b.setBackground(primary ? ProjectXTheme.GOLD : ProjectXTheme.OBSIDIAN_3);
        Border edge = BorderFactory.createMatteBorder(1, 1, 1, 1, primary ? ProjectXTheme.GOLD_HI : ProjectXTheme.GOLD_DEEP);
        b.setBorder(BorderFactory.createCompoundBorder(edge, BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private static <T extends JComponent> T left(T c)
    {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static Component gap(int height)
    {
        return Box.createVerticalStrut(height);
    }
}
