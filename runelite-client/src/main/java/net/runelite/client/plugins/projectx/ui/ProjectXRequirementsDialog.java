package net.runelite.client.plugins.projectx.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.AlwaysOnPlugins;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.requirements.Requirement;
import net.runelite.client.plugins.projectx.util.requirements.RequirementCheck;
import net.runelite.client.plugins.projectx.util.requirements.RequirementCheckConfig;
import net.runelite.client.plugins.projectx.util.requirements.RequirementOutcome;
import net.runelite.client.plugins.projectx.util.requirements.RequirementReport;
import net.runelite.client.plugins.projectx.util.requirements.RequirementSeverity;
import net.runelite.client.plugins.projectx.util.requirements.RequirementStatus;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.Text;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The pre-start checklist: before a script starts from the plugin list, check what it needs
 * and, if something is missing, show what with Start anyway and Cancel. When nothing is
 * declared or everything is met, the script starts as before, without a window.
 *
 * <p>Game state is read on the client thread; the window is shown on the Swing thread.
 */
@Slf4j
final class ProjectXRequirementsDialog
{
    /** If the client thread hasn't answered by then, start without the check rather than hang. */
    private static final int CHECK_TIMEOUT_MS = 5_000;
    private static final int MAX_NOTES = 8;

    /** Scripts whose check is running, so a double click doesn't start two checks. Swing thread only. */
    private static final Set<Plugin> PENDING = new HashSet<>();

    private ProjectXRequirementsDialog()
    {
    }

    /**
     * Checks the script's requirements before it starts, when there is anything to check.
     *
     * @param start starts the script; run on the Swing thread once the check passes or the
     *              player chooses Start anyway, and never if they cancel
     * @return true if the start was handed to the check (the caller must not start it now);
     *         false if there is nothing to check, so the caller starts it as usual
     */
    static boolean deferStart(Component parent, Plugin plugin, PluginManager pluginManager, Runnable start)
    {
        if (plugin == null || !SwingUtilities.isEventDispatchThread())
        {
            return false;
        }
        if (PENDING.contains(plugin))
        {
            return true;
        }
        try
        {
            if (AlwaysOnPlugins.isLocked(plugin.getClass().getAnnotation(PluginDescriptor.class))
                || pluginManager.isPluginActive(plugin))
            {
                return false;
            }
            RequirementCheckConfig config = config();
            if (config == null || !config.enabled() || isSkipped(config, plugin))
            {
                return false;
            }
            // Logged out, levels, quests and items read as nothing: let the script start and log in.
            if (!ProjectX.isLoggedIn() || ProjectX.getClientThread() == null)
            {
                return false;
            }
            List<Requirement> requirements = RequirementCheck.collect(plugin);
            if (requirements.isEmpty())
            {
                return false;
            }
            runCheck(parent, plugin, requirements, config.warnRecommended(), start);
            return true;
        }
        catch (RuntimeException e)
        {
            log.warn("Pre-start check for {} failed; starting without it", plugin.getClass().getSimpleName(), e);
            return false;
        }
    }

    private static void runCheck(Component parent, Plugin plugin, List<Requirement> requirements,
                                 boolean warnRecommended, Runnable start)
    {
        String name = RequirementCheck.scriptName(plugin);
        AtomicBoolean settled = new AtomicBoolean(false);
        PENDING.add(plugin);

        Timer timeout = new Timer(CHECK_TIMEOUT_MS, e ->
        {
            if (settled.compareAndSet(false, true))
            {
                PENDING.remove(plugin);
                log.warn("Pre-start check for {} got no answer from the client thread; starting without it", name);
                start.run();
            }
        });
        timeout.setRepeats(false);
        timeout.start();

        ProjectX.getClientThread().invoke(() ->
        {
            RequirementReport report;
            try
            {
                report = RequirementCheck.evaluateOnClientThread(name, requirements);
            }
            catch (RuntimeException ex)
            {
                log.warn("Pre-start check for {} failed; starting without it", name, ex);
                report = null;
            }
            RequirementReport finished = report;
            SwingUtilities.invokeLater(() ->
            {
                if (!settled.compareAndSet(false, true))
                {
                    return;
                }
                timeout.stop();
                PENDING.remove(plugin);
                if (finished == null || !finished.needsAttention(warnRecommended))
                {
                    start.run();
                    return;
                }
                log.info("Pre-start check: {}", finished);
                show(parent, plugin, finished, start);
            });
        });
    }

    // ---- settings

    private static RequirementCheckConfig config()
    {
        ConfigManager cm = ProjectX.getConfigManager();
        return cm == null ? null : cm.getConfig(RequirementCheckConfig.class);
    }

    private static boolean isSkipped(RequirementCheckConfig config, Plugin plugin)
    {
        String csv = config.skippedScripts();
        if (csv == null || csv.isBlank())
        {
            return false;
        }
        String simple = plugin.getClass().getSimpleName();
        return Text.fromCSV(csv).stream().anyMatch(s -> s.equalsIgnoreCase(simple));
    }

    private static void skip(Plugin plugin)
    {
        ConfigManager cm = ProjectX.getConfigManager();
        if (cm == null)
        {
            return;
        }
        String csv = cm.getConfiguration(RequirementCheckConfig.GROUP, RequirementCheckConfig.SKIPPED_SCRIPTS);
        List<String> names = new ArrayList<>(csv == null || csv.isBlank() ? List.of() : Text.fromCSV(csv));
        String simple = plugin.getClass().getSimpleName();
        if (names.stream().noneMatch(s -> s.equalsIgnoreCase(simple)))
        {
            names.add(simple);
        }
        cm.setConfiguration(RequirementCheckConfig.GROUP, RequirementCheckConfig.SKIPPED_SCRIPTS, Text.toCSV(names));
    }

    // ---- the window

    private static void show(Component parent, Plugin plugin, RequirementReport report, Runnable start)
    {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Before you start: " + report.getScriptName(), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBackground(ProjectXTheme.OBSIDIAN_1);
        content.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, ProjectXTheme.GOLD_DEEP),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(ProjectXTheme.header(report.getScriptName(), report.summary(), null));
        JLabel lead = html(leadText(report), ProjectXTheme.INK, 300);
        lead.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        lead.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(lead);
        content.add(top, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(ProjectXTheme.OBSIDIAN_2);
        rows.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        for (RequirementOutcome o : report.getOutcomes())
        {
            rows.add(row(o));
        }
        JScrollPane scroll = new JScrollPane(rows);
        scroll.setBorder(BorderFactory.createLineBorder(ProjectXTheme.GOLD_DEEP));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        Dimension pref = rows.getPreferredSize();
        scroll.setPreferredSize(new Dimension(Math.max(340, pref.width + 20), Math.min(360, pref.height + 4)));
        content.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JCheckBox dontAsk = new JCheckBox("Don't check this script again");
        dontAsk.setOpaque(false);
        dontAsk.setForeground(ProjectXTheme.INK_DIM);
        dontAsk.setToolTipText("Turn it back on in Pre-start checks settings");
        bottom.add(dontAsk, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton startAnyway = new JButton("Start anyway");
        ProjectXTheme.stylePrimary(startAnyway);
        buttons.add(cancel);
        buttons.add(startAnyway);
        bottom.add(buttons, BorderLayout.SOUTH);
        content.add(bottom, BorderLayout.SOUTH);

        AtomicReference<Boolean> choice = new AtomicReference<>(false);
        cancel.addActionListener(e -> dialog.dispose());
        startAnyway.addActionListener(e ->
        {
            choice.set(true);
            dialog.dispose();
        });
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        cancel.requestFocusInWindow();
        dialog.setVisible(true); // modal: returns when closed

        if (dontAsk.isSelected())
        {
            skip(plugin);
        }
        if (Boolean.TRUE.equals(choice.get()))
        {
            start.run();
        }
    }

    private static String leadText(RequirementReport report)
    {
        if (report.hasUnmetRequired())
        {
            return "This script needs some things you don't have yet. It may not work until you get them.";
        }
        if (report.count(RequirementStatus.UNKNOWN, RequirementSeverity.REQUIRED) > 0)
        {
            return "Some things this script needs couldn't be checked yet.";
        }
        return "Some recommended things are missing. The script may still work.";
    }

    private static JPanel row(RequirementOutcome o)
    {
        JPanel line = new JPanel(new BorderLayout(8, 0));
        line.setOpaque(false);
        line.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(new StatusIcon(o.getStatus()));
        icon.setVerticalAlignment(SwingConstants.TOP);
        icon.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        line.add(icon, BorderLayout.WEST);

        JPanel words = new JPanel();
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.setOpaque(false);

        boolean recommended = o.getSeverity() == RequirementSeverity.RECOMMENDED;
        JLabel title = html(esc(o.getDescription())
            + (recommended ? " <span style='color:" + hex(ProjectXTheme.INK_FAINT) + "'>(recommended)</span>" : ""),
            o.getStatus() == RequirementStatus.MET ? ProjectXTheme.INK_DIM : ProjectXTheme.INK, 260);
        words.add(title);

        if (!o.getDetail().isEmpty())
        {
            JLabel detail = html(esc(o.getDetail()), detailColor(o.getStatus()), 260);
            detail.setFont(FontManager.getRunescapeSmallFont());
            words.add(detail);
        }
        List<String> notes = o.getResult().getNotes();
        if (!notes.isEmpty() && o.getStatus() != RequirementStatus.MET)
        {
            StringBuilder sb = new StringBuilder();
            int shown = Math.min(MAX_NOTES, notes.size());
            for (int i = 0; i < shown; i++)
            {
                sb.append(i == 0 ? "" : "<br>").append("&middot; ").append(esc(notes.get(i)));
            }
            if (notes.size() > shown)
            {
                sb.append("<br>&middot; and ").append(notes.size() - shown).append(" more");
            }
            JLabel list = html(sb.toString(), ProjectXTheme.INK_FAINT, 260);
            list.setFont(FontManager.getRunescapeSmallFont());
            words.add(list);
        }
        line.add(words, BorderLayout.CENTER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, line.getPreferredSize().height));
        return line;
    }

    private static Color detailColor(RequirementStatus status)
    {
        switch (status)
        {
            case MET:
                return ProjectXTheme.INK_FAINT;
            case UNMET:
                return ProjectXTheme.DANGER;
            default:
                return ProjectXTheme.GOLD;
        }
    }

    private static JLabel html(String body, Color color, int width)
    {
        JLabel l = new JLabel("<html><div style='width:" + width + "px'>" + body + "</div></html>");
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static String hex(Color c)
    {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String esc(String s)
    {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** A drawn tick, cross or question mark, so it shows whatever the font. */
    private static final class StatusIcon implements Icon
    {
        private static final int SIZE = 14;
        private final RequirementStatus status;

        StatusIcon(RequirementStatus status)
        {
            this.status = status;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            try
            {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                switch (status)
                {
                    case MET:
                        g2.setColor(ProjectXTheme.RUNNING);
                        g2.drawPolyline(new int[]{x + 2, x + 5, x + 12}, new int[]{y + 7, y + 11, y + 3}, 3);
                        break;
                    case UNMET:
                        g2.setColor(ProjectXTheme.DANGER);
                        g2.drawLine(x + 3, y + 3, x + 11, y + 11);
                        g2.drawLine(x + 11, y + 3, x + 3, y + 11);
                        break;
                    default:
                        g2.setColor(ProjectXTheme.GOLD);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawOval(x + 1, y + 1, SIZE - 3, SIZE - 3);
                        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 10f));
                        FontMetrics fm = g2.getFontMetrics();
                        String q = "?";
                        g2.drawString(q, x + (SIZE - 1 - fm.stringWidth(q)) / 2, y + (SIZE - 1 + fm.getAscent() - fm.getDescent()) / 2);
                        break;
                }
            }
            finally
            {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth()
        {
            return SIZE;
        }

        @Override
        public int getIconHeight()
        {
            return SIZE;
        }
    }
}
