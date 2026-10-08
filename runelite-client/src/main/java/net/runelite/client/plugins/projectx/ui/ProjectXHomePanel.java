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
    private final JPanel scriptList = new JPanel();
    private final JLabel scriptCount = new JLabel();
    private final Timer timer;

    /** Opens the Scripts tab; set by whoever owns it. */
    private Runnable openScripts = () -> { };
    /** What the list shows now, so it is rebuilt only when something changed. Null until first built. */
    private List<String> shown;

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

        // The sidebar's own grey (obsidian, via ColorScheme), so the tab and the space below it are one.
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(left(header()));
        add(gap(12));
        add(left(balanceCard()));
        add(gap(14));

        JPanel scriptsHeading = new JPanel(new BorderLayout());
        scriptsHeading.setOpaque(false);
        scriptsHeading.add(eyebrow("My scripts"), BorderLayout.WEST);
        scriptCount.setFont(FontManager.getRunescapeSmallFont());
        scriptCount.setForeground(ProjectXTheme.INK_FAINT);
        scriptsHeading.add(scriptCount, BorderLayout.EAST);
        scriptsHeading.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        add(left(scriptsHeading));
        add(gap(6));

        scriptList.setLayout(new BoxLayout(scriptList, BoxLayout.Y_AXIS));
        scriptList.setOpaque(false);
        add(left(scriptList));
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
        actions.add(button("Dashboard", false, () -> browse(ProjectXSite.dashboard())));
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        add(left(actions));
        add(gap(14));

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
        Integer tokens = identity.tokenBalance();
        balance.setText(tokens == null ? "—" : tokens + " X");
    }

    /** One installed store script, as the list shows it. */
    private static final class Row
    {
        final Plugin plugin;
        final String internalName;
        final String name;
        final boolean running;
        final String detail;
        final Color detailColor;
        final boolean needsAccess;

        Row(Plugin plugin, String internalName, String name, boolean running, String detail, Color detailColor, boolean needsAccess)
        {
            this.plugin = plugin;
            this.internalName = internalName;
            this.name = name;
            this.running = running;
            this.detail = detail;
            this.detailColor = detailColor;
            this.needsAccess = needsAccess;
        }

        String key()
        {
            return internalName + "|" + running + "|" + detail + "|" + needsAccess;
        }
    }

    private void updateScripts()
    {
        List<Row> rows = new ArrayList<>();
        for (Plugin plugin : projectxPluginManager.getInstalledPlugins())
        {
            String internalName = plugin.getClass().getSimpleName();
            ProjectXPluginManifest manifest = projectxPluginManager.getManifestMap().get(internalName);
            if (manifest == null)
            {
                // RuneLite Plugin Hub plugins (117 HD and the like) are external too; only store scripts belong here.
                continue;
            }
            String name = manifest.getDisplayName() != null ? manifest.getDisplayName() : descriptorName(plugin);
            boolean running = pluginManager.isPluginActive(plugin);

            String access;
            Color color = ProjectXTheme.INK_FAINT;
            boolean needsAccess = false;
            if (manifest.isBroken())
            {
                access = "Not working right now";
                color = ProjectXTheme.DANGER;
            }
            else if (entitlements.isPaid(internalName))
            {
                Optional<Instant> ends = entitlements.accessEndsAt(internalName);
                if (ends.isPresent() && entitlements.isEntitled(internalName))
                {
                    String left = timeLeft(ends.get());
                    access = entitlements.isOnTrial(internalName) ? "Trial · " + left + " left" : left + " left";
                    color = Duration.between(Instant.now(), ends.get()).toDays() < 3 ? ProjectXTheme.MOLTEN_HI : ProjectXTheme.GOLD;
                }
                else
                {
                    access = "No access";
                    color = ProjectXTheme.MOLTEN_HI;
                    needsAccess = true;
                }
            }
            else
            {
                access = "Free";
            }

            String detail = (running ? "Running" : "Stopped") + " · " + access;
            rows.add(new Row(plugin, internalName, name, running, detail, color, needsAccess));
        }
        rows.sort(Comparator.comparing((Row r) -> !r.running).thenComparing(r -> r.name.toLowerCase()));

        List<String> keys = new ArrayList<>();
        rows.forEach(r -> keys.add(r.key()));
        if (keys.equals(shown))
        {
            return;
        }
        shown = keys;

        long running = rows.stream().filter(r -> r.running).count();
        scriptCount.setText(rows.isEmpty() ? "" : running > 0 ? running + " running" : rows.size() + " installed");

        scriptList.removeAll();
        if (rows.isEmpty())
        {
            JPanel empty = card();
            empty.setLayout(new BorderLayout(0, 8));
            JLabel text = new JLabel("<html><div style='width:150px'>You haven't added any scripts yet. "
                + "Most are free, and paid ones come with a free trial.</div></html>");
            text.setForeground(ProjectXTheme.INK_DIM);
            empty.add(text, BorderLayout.CENTER);
            empty.add(button("Browse scripts", true, () -> openScripts.run()), BorderLayout.SOUTH);
            empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            scriptList.add(left(empty));
        }
        for (Row row : rows)
        {
            scriptList.add(left(scriptRow(row)));
            scriptList.add(Box.createVerticalStrut(4));
        }
        scriptList.revalidate();
        scriptList.repaint();
    }

    private JPanel scriptRow(Row row)
    {
        JPanel line = card();
        line.setLayout(new BorderLayout(6, 0));
        if (row.running)
        {
            line.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, ProjectXTheme.RUNNING),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        }

        JPanel words = new JPanel();
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.setOpaque(false);
        JLabel name = new JLabel(row.name);
        name.setForeground(ProjectXTheme.INK);
        words.add(name);
        JLabel detail = new JLabel(row.detail);
        detail.setFont(FontManager.getRunescapeSmallFont());
        detail.setForeground(row.running ? ProjectXTheme.RUNNING : row.detailColor);
        words.add(detail);
        line.add(words, BorderLayout.CENTER);

        JButton action;
        if (row.running)
        {
            action = button("Stop", false, () -> pluginListPanel.stopPlugin(row.plugin));
        }
        else if (row.needsAccess)
        {
            action = button("Get", true, () -> ProjectXStoreDialog.show(this, row.internalName));
        }
        else
        {
            action = button("Start", true, () -> pluginListPanel.startPlugin(row.plugin));
        }
        action.setPreferredSize(new Dimension(58, 24));
        JPanel holder = new JPanel(new GridLayout(1, 1));
        holder.setOpaque(false);
        holder.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        holder.add(action);
        line.add(holder, BorderLayout.EAST);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return line;
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

    private static JPanel card()
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
    private static JButton button(String text, boolean primary, Runnable action)
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
