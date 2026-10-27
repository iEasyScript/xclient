package net.runelite.client.plugins.projectx.ui;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXEntitlements;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManifest;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The "My Scripts" pop-out: every installed store script and every local dev plugin, with
 * a search box and a Start/Stop button on each. Non-modal; the same window is brought to
 * the front when reopened, and keeps its size and place for the session.
 *
 * <p>All of it runs on the EDT. It reads plugin state the same way the side panel's old
 * inline list did (no client or game state), and refreshes on a timer while it is showing.
 */
final class MyScriptsWindow
{
    private static final int REFRESH_MS = 1000;

    /** One script, as a list shows it. */
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

    private final PluginManager pluginManager;
    private final ProjectXPluginManager projectxPluginManager;
    private final ProjectXPluginListPanel pluginListPanel;
    private final ProjectXEntitlements entitlements;

    private JDialog dialog;
    private final JTextField search = new JTextField();
    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel storeList = listPanel();
    private final JPanel localList = listPanel();
    private final Timer timer = new Timer(REFRESH_MS, e -> refresh());

    private List<Row> storeRows = new ArrayList<>();
    private List<Row> localRows = new ArrayList<>();
    private String shown;
    private java.awt.Rectangle remembered;

    MyScriptsWindow(PluginManager pluginManager, ProjectXPluginManager projectxPluginManager,
                    ProjectXPluginListPanel pluginListPanel, ProjectXEntitlements entitlements)
    {
        this.pluginManager = pluginManager;
        this.projectxPluginManager = projectxPluginManager;
        this.pluginListPanel = pluginListPanel;
        this.entitlements = entitlements;
    }

    /** Opens the window, or brings the open one to the front. EDT only. */
    void show(Component anchor)
    {
        if (dialog == null)
        {
            build(anchor);
        }
        refresh();
        if (!dialog.isVisible())
        {
            dialog.setVisible(true);
            timer.start();
        }
        dialog.toFront();
        dialog.requestFocus();
    }

    /** Collects the rows (cheap, EDT) and returns the one-line summary for the side panel. */
    String summary()
    {
        collect();
        long running = storeRows.stream().filter(r -> r.running).count()
            + localRows.stream().filter(r -> r.running).count();
        int total = storeRows.size() + localRows.size();
        if (total == 0)
        {
            return "";
        }
        return total + " installed" + (running > 0 ? " · " + running + " running" : "");
    }

    private void build(Component anchor)
    {
        Window owner = anchor == null ? null : SwingUtilities.getWindowAncestor(anchor);
        dialog = new JDialog(owner, "My Scripts");
        dialog.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dialog.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                remember();
                timer.stop();
            }

            @Override
            public void windowClosed(java.awt.event.WindowEvent e)
            {
                timer.stop();
            }
        });
        dialog.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            @Override
            public void componentHidden(java.awt.event.ComponentEvent e)
            {
                timer.stop();
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(ProjectXTheme.OBSIDIAN_1);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        search.setBackground(ProjectXTheme.OBSIDIAN_3);
        search.setForeground(ProjectXTheme.INK);
        search.setCaretColor(ProjectXTheme.GOLD_HI);
        search.setToolTipText("Search your scripts by name");
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, ProjectXTheme.GOLD_DEEP),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                shown = null;
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                insertUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                insertUpdate(e);
            }
        });
        root.add(search, BorderLayout.NORTH);

        tabs.setBackground(ProjectXTheme.OBSIDIAN_2);
        tabs.setForeground(ProjectXTheme.GOLD_HI);
        tabs.addTab("Store scripts", scroll(storeList));
        tabs.addTab("Local scripts", scroll(localList));
        root.add(tabs, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(340, 300));
        if (remembered != null)
        {
            dialog.setBounds(remembered);
        }
        else
        {
            dialog.setSize(420, 600);
            dialog.setLocationRelativeTo(owner);
        }
    }

    private void remember()
    {
        if (dialog != null)
        {
            remembered = dialog.getBounds();
        }
    }

    private static JPanel listPanel()
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(ProjectXTheme.OBSIDIAN_1);
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return p;
    }

    private static JScrollPane scroll(JPanel content)
    {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(ProjectXTheme.OBSIDIAN_1);
        wrap.add(content, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(wrap, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(ProjectXTheme.OBSIDIAN_1);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ---- data

    private void collect()
    {
        List<Row> store = new ArrayList<>();
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
                    String left = ProjectXHomePanel.timeLeft(ends.get());
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
            store.add(new Row(plugin, internalName, name, running, detail, color, needsAccess));
        }
        store.sort(order());

        List<Row> local = new ArrayList<>();
        for (Plugin plugin : projectxPluginManager.getDevPlugins())
        {
            boolean running = pluginManager.isPluginActive(plugin);
            String jar = projectxPluginManager.getDevJarName(plugin);
            String detail = (running ? "Running" : "Stopped") + " · Local" + (jar == null ? "" : " · from " + jar);
            local.add(new Row(plugin, plugin.getClass().getName(), descriptorName(plugin), running, detail, ProjectXTheme.INK_FAINT, false));
        }
        local.sort(order());
        storeRows = store;
        localRows = local;
    }

    private static Comparator<Row> order()
    {
        return Comparator.comparing((Row r) -> !r.running).thenComparing(r -> r.name.toLowerCase(Locale.ROOT));
    }

    // ---- view

    private void refresh()
    {
        if (dialog == null)
        {
            return;
        }
        collect();
        String q = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Row> store = filter(storeRows, q);
        List<Row> local = filter(localRows, q);

        StringBuilder key = new StringBuilder(q).append('#').append(storeRows.size()).append('/').append(localRows.size());
        store.forEach(r -> key.append('\n').append(r.key()));
        key.append("\n--");
        local.forEach(r -> key.append('\n').append(r.key()));
        if (key.toString().equals(shown))
        {
            return;
        }
        shown = key.toString();

        tabs.setTitleAt(0, "Store scripts (" + storeRows.size() + ")");
        tabs.setTitleAt(1, "Local scripts (" + localRows.size() + ")");

        fill(storeList, store, storeRows.isEmpty()
            ? "You haven't added any scripts yet. Get some from the Store."
            : "No store scripts match your search.");
        fill(localList, local, localRows.isEmpty()
            ? "Put your plugin jars in .runelite\\projectx-plugins\\dev and restart the client."
            : "No local scripts match your search.");
    }

    private static List<Row> filter(List<Row> rows, String q)
    {
        if (q.isEmpty())
        {
            return rows;
        }
        List<Row> out = new ArrayList<>();
        for (Row r : rows)
        {
            if (r.name.toLowerCase(Locale.ROOT).contains(q))
            {
                out.add(r);
            }
        }
        return out;
    }

    private void fill(JPanel list, List<Row> rows, String emptyText)
    {
        list.removeAll();
        if (rows.isEmpty())
        {
            JLabel hint = new JLabel("<html><div style='width:280px'>" + emptyText + "</div></html>");
            hint.setForeground(ProjectXTheme.INK_DIM);
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(hint);
        }
        for (Row row : rows)
        {
            JPanel line = scriptRow(row);
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            list.add(line);
            list.add(Box.createVerticalStrut(4));
        }
        list.revalidate();
        list.repaint();
    }

    private JPanel scriptRow(Row row)
    {
        JPanel line = ProjectXHomePanel.card();
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
            action = ProjectXHomePanel.button("Stop", false, () -> pluginListPanel.stopPlugin(row.plugin));
        }
        else if (row.needsAccess)
        {
            action = ProjectXHomePanel.button("Get", true, () -> ProjectXStoreDialog.show(dialog, row.internalName));
        }
        else
        {
            action = ProjectXHomePanel.button("Start", true, () -> pluginListPanel.startPlugin(row.plugin));
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

    private static String descriptorName(Plugin plugin)
    {
        PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
        return d == null ? plugin.getClass().getSimpleName() : d.name().replaceAll("<[^>]*>", "").trim();
    }
}
