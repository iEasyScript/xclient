package net.runelite.client.plugins.projectx.scriptqueue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Script Queue sidebar: build a list of scripts and when to move on from each,
 * then start it. Saved as you go, so the queue is there again next time.
 */
class ScriptQueuePanel extends PluginPanel
{
    static final String GROUP = "projectxscriptqueue";

    private final ConfigManager configManager;
    private final PluginManager pluginManager;
    private final Gson gson;

    private final List<QueueStep> steps = new ArrayList<>();
    private final JPanel stepList = new JPanel();
    private final JLabel status = new JLabel("Not running.");
    private final JButton startStop = new JButton("Start queue");
    private final JComboBox<ScriptChoice> scriptBox = new JComboBox<>();
    private final JComboBox<QueueStep.Until> untilBox = new JComboBox<>(QueueStep.Until.values());
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(60, 1, 24 * 60, 5));
    private final JComboBox<String> skillBox = new JComboBox<>(Arrays.stream(Skill.values())
        .map(s -> s.getName()).toArray(String[]::new));
    private final JSpinner level = new JSpinner(new SpinnerNumberModel(70, 2, 99, 1));
    private final JCheckBox loop = new JCheckBox("Loop the queue");
    private final JCheckBox logout = new JCheckBox("Log out when it finishes");
    private final JSpinner pauseMin = new JSpinner(new SpinnerNumberModel(20, 0, 600, 5));
    private final JSpinner pauseMax = new JSpinner(new SpinnerNumberModel(90, 0, 900, 5));

    private ScriptQueueRunner runner;
    private Thread runnerThread;

    /** A script in the dropdown: the plugin's class and its name as shown. */
    private static final class ScriptChoice
    {
        final String className;
        final String name;

        ScriptChoice(String className, String name)
        {
            this.className = className;
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    ScriptQueuePanel(ConfigManager configManager, PluginManager pluginManager, Gson gson)
    {
        super(false);
        this.configManager = configManager;
        this.pluginManager = pluginManager;
        this.gson = gson;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel title = new JLabel("Script Queue");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(ColorScheme.BRAND_ORANGE);
        content.add(left(title));
        content.add(left(wrap("Runs your scripts one after another. Each moves on when its time is up, its goal is reached, or it stops by itself.")));
        content.add(gap());

        stepList.setLayout(new BoxLayout(stepList, BoxLayout.Y_AXIS));
        stepList.setOpaque(false);
        content.add(left(stepList));
        content.add(gap());

        // ---- add a step
        JPanel add = new JPanel(new GridLayout(0, 1, 0, 4));
        add.setOpaque(false);
        add.setBorder(BorderFactory.createTitledBorder("Add a script"));
        add.add(scriptBox);
        add.add(untilBox);
        JPanel minutesRow = row(new JLabel("Minutes"), minutes);
        JPanel levelRow = row(skillBox, level);
        add.add(minutesRow);
        add.add(levelRow);
        JButton addButton = new JButton("Add to queue");
        add.add(addButton);
        content.add(left(add));
        untilBox.addActionListener(e ->
        {
            QueueStep.Until u = (QueueStep.Until) untilBox.getSelectedItem();
            minutesRow.setVisible(u == QueueStep.Until.MINUTES);
            levelRow.setVisible(u == QueueStep.Until.LEVEL);
            revalidate();
        });
        levelRow.setVisible(false);
        addButton.addActionListener(e -> addStep());
        content.add(gap());

        // ---- options
        JPanel options = new JPanel(new GridLayout(0, 1, 0, 4));
        options.setOpaque(false);
        options.setBorder(BorderFactory.createTitledBorder("Options"));
        options.add(loop);
        options.add(logout);
        options.add(row(new JLabel("Pause between (s)"), row(pauseMin, pauseMax)));
        content.add(left(options));
        content.add(gap());

        startStop.addActionListener(e -> toggle());
        content.add(left(startStop));
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        content.add(left(status));

        add(content, BorderLayout.NORTH);

        for (JCheckBox box : new JCheckBox[]{loop, logout})
        {
            box.setOpaque(false);
            box.addActionListener(e -> save());
        }
        pauseMin.addChangeListener(e -> save());
        pauseMax.addChangeListener(e -> save());

        load();
        refreshScripts();
        rebuildList();
    }

    /** Fills the dropdown with the Project X scripts installed right now. */
    void refreshScripts()
    {
        Object selected = scriptBox.getSelectedItem();
        scriptBox.removeAllItems();
        List<ScriptChoice> choices = pluginManager.getPlugins().stream()
            .filter(p -> p.getClass().getName().startsWith("net.runelite.client.plugins.projectx."))
            .filter(p -> !(p instanceof ScriptQueuePlugin))
            .filter(p -> !p.getClass().getSimpleName().equals("ProjectXPlugin"))
            .map(p -> new ScriptChoice(p.getClass().getName(), nameOf(p)))
            .filter(c -> c.name != null)
            .sorted(Comparator.comparing(c -> c.name.toLowerCase()))
            .collect(Collectors.toList());
        for (ScriptChoice c : choices)
        {
            scriptBox.addItem(c);
        }
        if (selected != null)
        {
            scriptBox.setSelectedItem(selected);
        }
    }

    private static String nameOf(Plugin plugin)
    {
        PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
        if (d == null || d.hidden())
        {
            return null;
        }
        return d.name().replaceAll("<[^>]*>", "").trim();
    }

    private void addStep()
    {
        ScriptChoice choice = (ScriptChoice) scriptBox.getSelectedItem();
        if (choice == null)
        {
            return;
        }
        QueueStep step = new QueueStep();
        step.setPluginClass(choice.className);
        step.setPluginName(choice.name);
        step.setUntil((QueueStep.Until) untilBox.getSelectedItem());
        step.setMinutes((Integer) minutes.getValue());
        step.setSkill((String) skillBox.getSelectedItem());
        step.setLevel((Integer) level.getValue());
        steps.add(step);
        save();
        rebuildList();
    }

    private void rebuildList()
    {
        stepList.removeAll();
        if (steps.isEmpty())
        {
            stepList.add(left(wrap("No scripts yet. Add some below.")));
        }
        for (int i = 0; i < steps.size(); i++)
        {
            final int index = i;
            JPanel line = new JPanel(new BorderLayout(4, 0));
            line.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            line.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 4));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            line.add(new JLabel((i + 1) + ". " + steps.get(i).describe()), BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            buttons.setOpaque(false);
            buttons.add(small("▲", "Move up", () -> moveStep(index, -1)));
            buttons.add(small("▼", "Move down", () -> moveStep(index, 1)));
            buttons.add(small("✕", "Remove", () ->
            {
                steps.remove(index);
                save();
                rebuildList();
            }));
            line.add(buttons, BorderLayout.EAST);
            stepList.add(line);
            stepList.add(javax.swing.Box.createVerticalStrut(3));
        }
        boolean running = runner != null;
        stepList.setEnabled(!running);
        startStop.setEnabled(running || !steps.isEmpty());
        stepList.revalidate();
        stepList.repaint();
    }

    private void moveStep(int index, int by)
    {
        int to = index + by;
        if (to < 0 || to >= steps.size())
        {
            return;
        }
        QueueStep s = steps.remove(index);
        steps.add(to, s);
        save();
        rebuildList();
    }

    private void toggle()
    {
        if (runner != null)
        {
            runner.requestStop();
            if (runnerThread != null)
            {
                runnerThread.interrupt();
            }
            return;
        }
        if (steps.isEmpty())
        {
            return;
        }
        runner = new ScriptQueueRunner(steps, loop.isSelected(), logout.isSelected(),
            (Integer) pauseMin.getValue(), (Integer) pauseMax.getValue(), this::setStatus);
        runnerThread = new Thread(() ->
        {
            try
            {
                runner.run();
            }
            finally
            {
                SwingUtilities.invokeLater(() ->
                {
                    runner = null;
                    runnerThread = null;
                    startStop.setText("Start queue");
                    rebuildList();
                });
            }
        }, "projectx-script-queue");
        runnerThread.setDaemon(true);
        runnerThread.start();
        startStop.setText("Stop queue");
        rebuildList();
    }

    void shutdown()
    {
        if (runner != null)
        {
            runner.requestStop();
            if (runnerThread != null)
            {
                runnerThread.interrupt();
            }
        }
    }

    private void setStatus(String text)
    {
        SwingUtilities.invokeLater(() -> status.setText("<html><div style='width:190px'>" + text + "</div></html>"));
    }

    // ---- persistence

    private void save()
    {
        configManager.setConfiguration(GROUP, "steps", gson.toJson(steps));
        configManager.setConfiguration(GROUP, "loop", loop.isSelected());
        configManager.setConfiguration(GROUP, "logout", logout.isSelected());
        configManager.setConfiguration(GROUP, "pauseMin", (Integer) pauseMin.getValue());
        configManager.setConfiguration(GROUP, "pauseMax", (Integer) pauseMax.getValue());
    }

    private void load()
    {
        String json = configManager.getConfiguration(GROUP, "steps");
        if (json != null && !json.isEmpty())
        {
            try
            {
                List<QueueStep> saved = gson.fromJson(json, new TypeToken<List<QueueStep>>() { }.getType());
                if (saved != null)
                {
                    steps.addAll(saved);
                }
            }
            catch (RuntimeException ignored)
            {
                // An unreadable saved queue is just an empty one.
            }
        }
        loop.setSelected(Boolean.parseBoolean(configManager.getConfiguration(GROUP, "loop")));
        logout.setSelected(Boolean.parseBoolean(configManager.getConfiguration(GROUP, "logout")));
        pauseMin.setValue(intOr(configManager.getConfiguration(GROUP, "pauseMin"), 20));
        pauseMax.setValue(intOr(configManager.getConfiguration(GROUP, "pauseMax"), 90));
    }

    private static int intOr(String value, int fallback)
    {
        try
        {
            return value == null ? fallback : Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return fallback;
        }
    }

    // ---- small layout helpers

    private static JPanel row(Component a, Component b)
    {
        JPanel p = new JPanel(new GridLayout(1, 2, 4, 0));
        p.setOpaque(false);
        p.add(a);
        p.add(b);
        return p;
    }

    private static JLabel wrap(String text)
    {
        JLabel l = new JLabel("<html><div style='width:190px'>" + text + "</div></html>");
        l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        return l;
    }

    private static <T extends javax.swing.JComponent> T left(T c)
    {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    private static Component gap()
    {
        return javax.swing.Box.createVerticalStrut(8);
    }

    private static JButton small(String text, String tip, Runnable action)
    {
        JButton b = new JButton(text);
        b.setToolTipText(tip);
        b.setMargin(new java.awt.Insets(0, 4, 0, 4));
        b.addActionListener(e ->
        {
            if (SwingUtilities.getAncestorOfClass(ScriptQueuePanel.class, b) instanceof ScriptQueuePanel)
            {
                action.run();
            }
        });
        return b;
    }
}
