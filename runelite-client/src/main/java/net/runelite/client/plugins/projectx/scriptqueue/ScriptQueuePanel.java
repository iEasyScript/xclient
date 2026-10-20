package net.runelite.client.plugins.projectx.scriptqueue;

import com.google.gson.Gson;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
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
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The Script Queue sidebar: named plans, each a list of scripts and when to move on from
 * each, then start one. Saved as you go, so the plans are there again next time.
 */
class ScriptQueuePanel extends PluginPanel
{
    static final String GROUP = "projectxscriptqueue";

    /** In the "until" dropdown: no condition at all, so the step runs until its script stops. */
    private static final String UNTIL_STOPS = QueueStep.Until.STOPS.toString();

    private final ConfigManager configManager;
    private final PluginManager pluginManager;
    private final EventBus eventBus;
    private final ScriptQueueStore store;

    private List<QueuePlan> plans = new ArrayList<>();
    /** The plan showing, and the one the start button runs. */
    private QueuePlan plan;

    // ---- plans
    private final JComboBox<String> planBox = new JComboBox<>();
    private boolean fillingPlanBox;
    private final JButton newPlan = new JButton("New");
    private final JButton copyPlan = new JButton("Copy");
    private final JButton renamePlan = new JButton("Rename");
    private final JButton deletePlan = new JButton("Delete");

    private final JPanel stepList = new JPanel();
    private final JLabel status = new JLabel("Not running.");
    private final JButton startStop = new JButton("Start queue");

    // ---- the step editor
    private final JPanel editor = new JPanel();
    private final TitledBorder editorBorder = BorderFactory.createTitledBorder("Add a script");
    private final JComboBox<ScriptChoice> scriptBox = new JComboBox<>();
    private final JComboBox<Object> untilBox = new JComboBox<>();
    /** The inputs for each kind of condition; only the chosen kind's show. */
    private final Map<Object, List<JComponent>> inputRows = new LinkedHashMap<>();
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(60, 1, 24 * 60, 5));
    private final JComboBox<String> levelSkill = skillBox();
    private final JSpinner level = new JSpinner(new SpinnerNumberModel(70, 2, 99, 1));
    private final JComboBox<String> xpSkill = skillBox();
    private final JSpinner xp = new JSpinner(new SpinnerNumberModel(50_000, 1, 200_000_000, 1_000));
    private final JSpinner totalLevel = new JSpinner(new SpinnerNumberModel(500, 1, Skill.values().length * 99, 25));
    private final JSpinner combatLevel = new JSpinner(new SpinnerNumberModel(70, 3, 126, 1));
    private final JTextField itemName = new JTextField();
    private final JSpinner itemCount = new JSpinner(new SpinnerNumberModel(1_000, 1, Integer.MAX_VALUE, 100));
    private final JComboBox<StopCondition.Place> itemPlace = new JComboBox<>(StopCondition.Place.values());
    private final JSpinner clockHour = new JSpinner(new SpinnerNumberModel(23, 0, 23, 1));
    private final JSpinner clockMinute = new JSpinner(new SpinnerNumberModel(0, 0, 59, 5));
    private final JButton addCondition = new JButton("Add condition");
    private final JPanel pendingList = new JPanel();
    private final List<StopCondition> pending = new ArrayList<>();
    private final JComboBox<QueueStep.OnFailure> failureBox = new JComboBox<>(QueueStep.OnFailure.values());
    private final JButton addButton = new JButton("Add to queue");
    private final JButton saveEdit = new JButton("Save step");
    private final JButton cancelEdit = new JButton("Cancel");
    private final JPanel editButtons = new JPanel(new GridLayout(1, 2, 4, 0));
    private JPanel addCell;
    private JPanel editCell;
    /** The step being edited, or -1 when the editor adds a new one. */
    private int editing = -1;

    // ---- options
    private final JCheckBox loop = new JCheckBox("Loop the queue");
    private final JSpinner rounds = new JSpinner(new SpinnerNumberModel(0, 0, 999, 1));
    private final JCheckBox logout = new JCheckBox("Log out when it finishes");
    private final JCheckBox breakBetween = new JCheckBox("Take a break between scripts");
    private final JSpinner pauseMin = new JSpinner(new SpinnerNumberModel(20, 0, 600, 5));
    private final JSpinner pauseMax = new JSpinner(new SpinnerNumberModel(90, 0, 900, 5));
    private boolean loading;

    private ScriptQueueRunner runner;
    private Thread runnerThread;
    /** Under each step while a run goes on and after it: its progress, or how it ended. */
    private final Map<Integer, Note> notes = new HashMap<>();
    private final Map<Integer, JLabel> noteLabels = new HashMap<>();

    private static final class Note
    {
        final ScriptQueueRunner.StepState state;
        final String text;

        Note(ScriptQueueRunner.StepState state, String text)
        {
            this.state = state;
            this.text = text;
        }
    }

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

    ScriptQueuePanel(ConfigManager configManager, PluginManager pluginManager, Gson gson, EventBus eventBus)
    {
        super(false);
        this.configManager = configManager;
        this.pluginManager = pluginManager;
        this.eventBus = eventBus;
        this.store = new ScriptQueueStore(new ScriptQueueStore.Config()
        {
            @Override
            public String get(String key)
            {
                return configManager.getConfiguration(GROUP, key);
            }

            @Override
            public void set(String key, String value)
            {
                configManager.setConfiguration(GROUP, key, value);
            }
        }, gson);

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
        content.add(left(wrap("Runs your scripts one after another. Each moves on at the first of its goals, or when it stops by itself. BreakHandler breaks pause the queue. Click a step to edit it.")));
        content.add(gap());

        // ---- plans
        JPanel planPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        planPanel.setOpaque(false);
        planPanel.setBorder(BorderFactory.createTitledBorder("Plan"));
        planPanel.add(planBox);
        JPanel planButtons = new JPanel(new GridLayout(1, 4, 2, 0));
        planButtons.setOpaque(false);
        for (JButton b : new JButton[]{newPlan, copyPlan, renamePlan, deletePlan})
        {
            b.setMargin(new java.awt.Insets(1, 1, 1, 1));
            b.setFont(FontManager.getRunescapeSmallFont());
            planButtons.add(b);
        }
        newPlan.setToolTipText("Start a new, empty plan");
        copyPlan.setToolTipText("Save this plan under a new name");
        renamePlan.setToolTipText("Rename this plan");
        deletePlan.setToolTipText("Delete this plan");
        planPanel.add(planButtons);
        content.add(left(planPanel));
        content.add(gap());
        planBox.addActionListener(e ->
        {
            if (!fillingPlanBox)
            {
                switchTo((String) planBox.getSelectedItem());
            }
        });
        newPlan.addActionListener(e -> newPlan());
        copyPlan.addActionListener(e -> copyPlan());
        renamePlan.addActionListener(e -> renamePlan());
        deletePlan.addActionListener(e -> deletePlan());

        stepList.setLayout(new BoxLayout(stepList, BoxLayout.Y_AXIS));
        stepList.setOpaque(false);
        content.add(left(stepList));
        content.add(gap());

        // ---- add or edit a step
        buildEditor();
        content.add(left(editor));
        content.add(gap());

        // ---- options
        JPanel options = new JPanel(new GridLayout(0, 1, 0, 4));
        options.setOpaque(false);
        options.setBorder(BorderFactory.createTitledBorder("Options"));
        options.add(loop);
        JPanel roundsRow = new JPanel(new BorderLayout(4, 0));
        roundsRow.setOpaque(false);
        roundsRow.add(new JLabel("Rounds (0 = endless)"), BorderLayout.WEST);
        roundsRow.add(rounds, BorderLayout.CENTER);
        roundsRow.setToolTipText("With Loop the queue: how many times through the plan in all.");
        options.add(roundsRow);
        options.add(logout);
        options.add(breakBetween);
        breakBetween.setToolTipText("<html>Before each next script, take a BreakHandler break.<br>"
            + "Its settings decide how long, whether to log out, and the world to log back in to.<br>"
            + "Turns the BreakHandler on if it is off.</html>");
        options.add(row(new JLabel("Pause between (s)"), row(pauseMin, pauseMax)));
        content.add(left(options));
        content.add(gap());

        startStop.addActionListener(e -> toggle());
        content.add(left(startStop));
        status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        content.add(left(status));

        // A plan with many steps outgrows the sidebar: let it scroll.
        JPanel top = new FitWidth();
        top.setOpaque(false);
        top.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(top, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        for (JCheckBox box : new JCheckBox[]{loop, logout, breakBetween})
        {
            box.setOpaque(false);
            box.addActionListener(e -> optionsChanged());
        }
        rounds.addChangeListener(e -> optionsChanged());
        pauseMin.addChangeListener(e -> optionsChanged());
        pauseMax.addChangeListener(e -> optionsChanged());

        load();
        refreshScripts();
        fillPlanBox();
        rebuildList();
        rebuildPending();
    }

    private void buildEditor()
    {
        // Rows stacked, not a grid: a grid keeps a cell for every hidden row.
        editor.setLayout(new BoxLayout(editor, BoxLayout.Y_AXIS));
        editor.setOpaque(false);
        editor.setBorder(editorBorder);

        untilBox.addItem(StopCondition.Type.MINUTES);
        untilBox.addItem(UNTIL_STOPS);
        for (StopCondition.Type t : StopCondition.Type.values())
        {
            if (t != StopCondition.Type.MINUTES)
            {
                untilBox.addItem(t);
            }
        }

        clockMinute.setEditor(new JSpinner.NumberEditor(clockMinute, "00"));
        clockHour.setEditor(new JSpinner.NumberEditor(clockHour, "00"));
        itemName.setToolTipText("The item's exact name, as in game: Nature rune");
        itemPlace.setToolTipText("<html>The bank is known from the last time it was open.<br>"
            + "Until then, a bank count is never reached.</html>");
        xp.setToolTipText("Experience to gain in the skill, from when the step starts");

        inputRows.put(StopCondition.Type.MINUTES, List.of(cell(row(new JLabel("Minutes"), minutes))));
        inputRows.put(UNTIL_STOPS, List.of());
        inputRows.put(StopCondition.Type.LEVEL, List.of(cell(row(levelSkill, level))));
        inputRows.put(StopCondition.Type.XP, List.of(cell(row(xpSkill, xp))));
        inputRows.put(StopCondition.Type.TOTAL_LEVEL, List.of(cell(row(new JLabel("Total level"), totalLevel))));
        inputRows.put(StopCondition.Type.COMBAT_LEVEL, List.of(cell(row(new JLabel("Combat level"), combatLevel))));
        inputRows.put(StopCondition.Type.ITEM, List.of(cell(new JLabel("Item name, exactly as in game")), cell(itemName),
            cell(row(new JLabel("How many"), itemCount)), cell(itemPlace)));
        inputRows.put(StopCondition.Type.CLOCK, List.of(cell(row(new JLabel("Time (24h)"), row(clockHour, clockMinute)))));

        editor.add(cell(scriptBox));
        editor.add(cell(untilBox));
        for (List<JComponent> rows : inputRows.values())
        {
            for (JComponent r : rows)
            {
                editor.add(r);
            }
        }
        addCondition.setToolTipText("Add this condition to the step. With several, it moves on at whichever comes first.");
        editor.add(cell(addCondition));
        pendingList.setLayout(new BoxLayout(pendingList, BoxLayout.Y_AXIS));
        pendingList.setOpaque(false);
        editor.add(cell(pendingList));
        editor.add(cell(new JLabel("If it fails")));
        editor.add(cell(failureBox));
        failureBox.setToolTipText("<html>When the script can't start, fails its requirements, or stops with an error.<br>"
            + "A script that simply stops by itself has not failed: the queue moves on.</html>");
        addCell = cell(addButton);
        editor.add(addCell);
        editButtons.setOpaque(false);
        editButtons.add(saveEdit);
        editButtons.add(cancelEdit);
        editCell = cell(editButtons);
        editor.add(editCell);
        editCell.setVisible(false);

        untilBox.addActionListener(e -> showInputs());
        addCondition.addActionListener(e -> addPendingCondition());
        addButton.addActionListener(e -> saveStep());
        saveEdit.addActionListener(e -> saveStep());
        cancelEdit.addActionListener(e ->
        {
            stopEditing();
            rebuildList();
        });
        showInputs();
    }

    private void showInputs()
    {
        Object selected = untilBox.getSelectedItem();
        for (Map.Entry<Object, List<JComponent>> e : inputRows.entrySet())
        {
            for (JComponent r : e.getValue())
            {
                r.setVisible(e.getKey().equals(selected));
            }
        }
        addCondition.setEnabled(runner == null && selected != UNTIL_STOPS);
        revalidate();
        repaint();
    }

    /** Fills the dropdown with the Project X scripts installed right now. */
    void refreshScripts()
    {
        Object selected = scriptBox.getSelectedItem();
        scriptBox.removeAllItems();
        List<ScriptChoice> choices = pluginManager.getPlugins().stream()
            .filter(p -> p.getClass().getName().startsWith("net.runelite.client.plugins.projectx."))
            .filter(p -> !(p instanceof ScriptQueuePlugin))
            // Breaks are an option of the queue, not a step in it.
            .filter(p -> !p.getClass().getName().contains(".breakhandler."))
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
            selectScript(((ScriptChoice) selected).className, ((ScriptChoice) selected).name);
        }
    }

    /** Selects a script in the dropdown, adding it for the edit if it isn't installed any more. */
    private void selectScript(String className, String name)
    {
        for (int i = 0; i < scriptBox.getItemCount(); i++)
        {
            ScriptChoice c = scriptBox.getItemAt(i);
            if (c.className.equals(className))
            {
                scriptBox.setSelectedIndex(i);
                return;
            }
        }
        if (className != null && name != null)
        {
            ScriptChoice missing = new ScriptChoice(className, name);
            scriptBox.addItem(missing);
            scriptBox.setSelectedItem(missing);
        }
    }

    static String nameOf(Plugin plugin)
    {
        PluginDescriptor d = plugin.getClass().getAnnotation(PluginDescriptor.class);
        if (d == null || d.hidden())
        {
            return null;
        }
        return d.name().replaceAll("<[^>]*>", "").trim();
    }

    // ---- the step editor

    /** The condition the inputs show, or null for "until it stops"; null with a message shown if it isn't complete. */
    private StopCondition shownCondition()
    {
        Object selected = untilBox.getSelectedItem();
        if (!(selected instanceof StopCondition.Type))
        {
            return null;
        }
        switch ((StopCondition.Type) selected)
        {
            case MINUTES:
                return StopCondition.minutes((Integer) minutes.getValue());
            case LEVEL:
                return StopCondition.level((String) levelSkill.getSelectedItem(), (Integer) level.getValue());
            case XP:
                return StopCondition.xp((String) xpSkill.getSelectedItem(), (Integer) xp.getValue());
            case TOTAL_LEVEL:
                return StopCondition.totalLevel((Integer) totalLevel.getValue());
            case COMBAT_LEVEL:
                return StopCondition.combatLevel((Integer) combatLevel.getValue());
            case ITEM:
            {
                String name = itemName.getText().trim();
                if (name.isEmpty())
                {
                    JOptionPane.showMessageDialog(this, "Type the item's name first, exactly as in game: Nature rune.",
                        "Script Queue", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
                return StopCondition.item(name, (Integer) itemCount.getValue(), (StopCondition.Place) itemPlace.getSelectedItem());
            }
            case CLOCK:
                return StopCondition.clock(String.format(Locale.ROOT, "%02d:%02d", (Integer) clockHour.getValue(), (Integer) clockMinute.getValue()));
            default:
                return null;
        }
    }

    private void addPendingCondition()
    {
        StopCondition c = shownCondition();
        if (c == null)
        {
            return;
        }
        if (!pending.contains(c))
        {
            pending.add(c);
        }
        rebuildPending();
    }

    private void rebuildPending()
    {
        pendingList.removeAll();
        if (pending.isEmpty())
        {
            pendingList.add(left(wrap("Adding the step uses the condition above. To stop at whichever of several comes first, add each with Add condition.")));
        }
        else
        {
            pendingList.add(left(wrap("Moves on at the first of:")));
            for (StopCondition c : new ArrayList<>(pending))
            {
                JPanel line = new JPanel(new BorderLayout(4, 0));
                line.setOpaque(false);
                JLabel text = new JLabel("<html><div style='width:130px'>• " + escape(c.describe()) + "</div></html>");
                text.setForeground(ColorScheme.TEXT_COLOR);
                line.add(text, BorderLayout.CENTER);
                JButton remove = small("✕", "Remove this condition", () ->
                {
                    pending.remove(c);
                    rebuildPending();
                });
                remove.setEnabled(runner == null);
                line.add(remove, BorderLayout.EAST);
                pendingList.add(left(line));
            }
        }
        pendingList.revalidate();
        pendingList.repaint();
        revalidate();
    }

    /** Adds the step being built, or saves the one being edited. */
    private void saveStep()
    {
        ScriptChoice choice = (ScriptChoice) scriptBox.getSelectedItem();
        if (choice == null)
        {
            return;
        }
        List<StopCondition> conditions = new ArrayList<>(pending);
        if (conditions.isEmpty() && untilBox.getSelectedItem() != UNTIL_STOPS)
        {
            StopCondition shown = shownCondition();
            if (shown == null)
            {
                return;
            }
            conditions.add(shown);
        }
        QueueStep step = editing >= 0 && editing < plan.steps().size() ? plan.steps().get(editing) : new QueueStep();
        step.setPluginClass(choice.className);
        step.setPluginName(choice.name);
        step.setConditions(conditions);
        step.setOnFailure((QueueStep.OnFailure) failureBox.getSelectedItem());
        if (editing < 0 || editing >= plan.steps().size())
        {
            plan.steps().add(step);
        }
        notes.clear();
        save();
        stopEditing();
        rebuildList();
    }

    private void editStep(int index)
    {
        if (runner != null || index < 0 || index >= plan.steps().size())
        {
            return;
        }
        QueueStep step = plan.steps().get(index);
        editing = index;
        editorBorder.setTitle("Edit step " + (index + 1));
        selectScript(step.getPluginClass(), step.getPluginName());
        pending.clear();
        pending.addAll(step.conditions());
        if (pending.isEmpty())
        {
            untilBox.setSelectedItem(UNTIL_STOPS);
        }
        else
        {
            showCondition(pending.get(0));
        }
        failureBox.setSelectedItem(step.onFailure());
        addCell.setVisible(false);
        editCell.setVisible(true);
        rebuildPending();
        rebuildList();
    }

    /** Puts a condition's values in the inputs, so it is easy to add a variation of it. */
    private void showCondition(StopCondition c)
    {
        if (c.getType() == null)
        {
            return;
        }
        switch (c.getType())
        {
            case MINUTES:
                minutes.setValue(clamp(minutes, c.getMinutes()));
                break;
            case LEVEL:
                levelSkill.setSelectedItem(c.getSkill());
                level.setValue(clamp(level, c.getLevel()));
                break;
            case XP:
                xpSkill.setSelectedItem(c.getSkill());
                xp.setValue(clamp(xp, c.getXp()));
                break;
            case TOTAL_LEVEL:
                totalLevel.setValue(clamp(totalLevel, c.getLevel()));
                break;
            case COMBAT_LEVEL:
                combatLevel.setValue(clamp(combatLevel, c.getLevel()));
                break;
            case ITEM:
                itemName.setText(c.getItem() == null ? "" : c.getItem());
                itemCount.setValue(clamp(itemCount, c.getCount()));
                itemPlace.setSelectedItem(c.getPlace() == null ? StopCondition.Place.BANK : c.getPlace());
                break;
            case CLOCK:
            {
                java.time.LocalTime t = StepGoals.timeOf(c.getTime());
                if (t != null)
                {
                    clockHour.setValue(t.getHour());
                    clockMinute.setValue(t.getMinute());
                }
                break;
            }
            default:
                break;
        }
        untilBox.setSelectedItem(c.getType());
    }

    private static int clamp(JSpinner spinner, int value)
    {
        SpinnerNumberModel m = (SpinnerNumberModel) spinner.getModel();
        int min = ((Number) m.getMinimum()).intValue();
        int max = ((Number) m.getMaximum()).intValue();
        return Math.max(min, Math.min(max, value));
    }

    private void stopEditing()
    {
        editing = -1;
        editorBorder.setTitle("Add a script");
        pending.clear();
        addCell.setVisible(true);
        editCell.setVisible(false);
        rebuildPending();
        editor.repaint();
    }

    // ---- the list

    private void rebuildList()
    {
        stepList.removeAll();
        noteLabels.clear();
        boolean running = runner != null;
        List<QueueStep> steps = plan.steps();
        if (steps.isEmpty())
        {
            stepList.add(left(wrap("No scripts yet. Add some below.")));
        }
        for (int i = 0; i < steps.size(); i++)
        {
            final int index = i;
            QueueStep step = steps.get(i);
            Note note = notes.get(i);
            boolean live = note != null && note.state == ScriptQueueRunner.StepState.RUNNING;

            JPanel line = new JPanel(new BorderLayout(4, 2));
            line.setBackground(live ? ColorScheme.DARKER_GRAY_HOVER_COLOR
                : index == editing ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARKER_GRAY_COLOR);
            line.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 4));
            JLabel text = new JLabel("<html><div style='width:95px'>" + (i + 1) + ". " + escape(step.describe()) + "</div></html>");
            line.add(text, BorderLayout.CENTER);
            if (!running)
            {
                text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                text.setToolTipText("Click to edit this step");
                text.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        editStep(index);
                    }
                });
            }
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            buttons.setOpaque(false);
            buttons.add(small("▲", "Move up", () -> moveStep(index, -1)));
            buttons.add(small("▼", "Move down", () -> moveStep(index, 1)));
            buttons.add(small("✕", "Remove", () -> removeStep(index)));
            for (Component b : buttons.getComponents())
            {
                b.setEnabled(!running);
            }
            line.add(buttons, BorderLayout.EAST);

            JLabel noteLabel = new JLabel();
            noteLabel.setFont(FontManager.getRunescapeSmallFont());
            noteLabels.put(i, noteLabel);
            showNote(i, step, noteLabel);
            line.add(noteLabel, BorderLayout.SOUTH);

            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, line.getPreferredSize().height));
            stepList.add(line);
            stepList.add(javax.swing.Box.createVerticalStrut(3));
        }
        setEditable(!running);
        startStop.setEnabled(running || !steps.isEmpty());
        stepList.revalidate();
        stepList.repaint();
    }

    /** What shows under a step: how its run is going or went, and its failure setting if not the usual. */
    private void showNote(int index, QueueStep step, JLabel label)
    {
        Note note = notes.get(index);
        List<String> lines = new ArrayList<>();
        if (note != null)
        {
            switch (note.state)
            {
                case RUNNING:
                    lines.add(colored(ColorScheme.BRAND_ORANGE, note.text));
                    break;
                case FAILED:
                    lines.add(colored(ColorScheme.PROGRESS_ERROR_COLOR, "Failed: " + note.text));
                    break;
                default:
                    lines.add(colored(ColorScheme.PROGRESS_COMPLETE_COLOR, "Done: " + note.text));
                    break;
            }
        }
        if (step.onFailure() != QueueStep.OnFailure.SKIP)
        {
            lines.add(colored(ColorScheme.LIGHT_GRAY_COLOR, "If it fails: " + step.onFailure().toString().toLowerCase(Locale.ROOT)));
        }
        label.setVisible(!lines.isEmpty());
        label.setText(lines.isEmpty() ? "" : "<html><div style='width:160px'>" + String.join("<br>", lines) + "</div></html>");
    }

    private static String colored(Color color, String text)
    {
        return String.format("<span style='color:#%02x%02x%02x'>%s</span>", color.getRed(), color.getGreen(), color.getBlue(), escape(text));
    }

    private void moveStep(int index, int by)
    {
        int to = index + by;
        if (runner != null || to < 0 || to >= plan.steps().size())
        {
            return;
        }
        QueueStep s = plan.steps().remove(index);
        plan.steps().add(to, s);
        notes.clear();
        if (editing >= 0)
        {
            stopEditing();
        }
        save();
        rebuildList();
    }

    private void removeStep(int index)
    {
        if (runner != null || index < 0 || index >= plan.steps().size())
        {
            return;
        }
        plan.steps().remove(index);
        notes.clear();
        if (editing >= 0)
        {
            stopEditing();
        }
        save();
        rebuildList();
    }

    /** While a plan runs it is locked: the run works from a copy, and the list shows its progress. */
    private void setEditable(boolean editable)
    {
        for (JComponent c : new JComponent[]{planBox, newPlan, copyPlan, renamePlan, deletePlan,
            scriptBox, untilBox, failureBox, addButton, saveEdit, cancelEdit, loop})
        {
            c.setEnabled(editable);
        }
        rounds.setEnabled(editable && loop.isSelected());
        addCondition.setEnabled(editable && untilBox.getSelectedItem() != UNTIL_STOPS);
    }

    // ---- plans

    private void fillPlanBox()
    {
        fillingPlanBox = true;
        try
        {
            planBox.removeAllItems();
            for (QueuePlan p : plans)
            {
                planBox.addItem(p.getName());
            }
            planBox.setSelectedItem(plan.getName());
        }
        finally
        {
            fillingPlanBox = false;
        }
    }

    private void switchTo(String name)
    {
        QueuePlan found = ScriptQueueStore.find(plans, name);
        if (found == null || found == plan || runner != null)
        {
            return;
        }
        plan = found;
        showPlanOptions();
        notes.clear();
        stopEditing();
        save();
        rebuildList();
    }

    private void showPlanOptions()
    {
        loading = true;
        try
        {
            loop.setSelected(plan.isLoop());
            rounds.setValue(Math.max(0, Math.min(999, plan.getRounds())));
            rounds.setEnabled(runner == null && plan.isLoop());
        }
        finally
        {
            loading = false;
        }
    }

    private String askName(String message, String suggestion, QueuePlan renaming)
    {
        Object answer = JOptionPane.showInputDialog(this, message, "Script Queue", JOptionPane.PLAIN_MESSAGE, null, null, suggestion);
        if (answer == null)
        {
            return null;
        }
        String name = ScriptQueueStore.validName(plans, answer.toString(), renaming);
        if (name == null)
        {
            JOptionPane.showMessageDialog(this, answer.toString().trim().isEmpty()
                    ? "A plan needs a name." : "There's already a plan called " + answer.toString().trim() + ".",
                "Script Queue", JOptionPane.INFORMATION_MESSAGE);
        }
        return name;
    }

    private String freeName(String base)
    {
        String name = base;
        for (int n = 2; ScriptQueueStore.find(plans, name) != null; n++)
        {
            name = base + " " + n;
        }
        return name;
    }

    private void newPlan()
    {
        String name = askName("Name for the new plan:", freeName("Plan"), null);
        if (name != null)
        {
            addPlan(new QueuePlan(name));
        }
    }

    private void copyPlan()
    {
        String name = askName("Save a copy of " + plan.getName() + " as:", freeName(plan.getName() + " copy"), null);
        if (name != null)
        {
            QueuePlan copy = store.copy(plan);
            copy.setName(name);
            addPlan(copy);
        }
    }

    private void addPlan(QueuePlan added)
    {
        plans.add(added);
        plan = added;
        fillPlanBox();
        showPlanOptions();
        notes.clear();
        stopEditing();
        save();
        rebuildList();
    }

    private void renamePlan()
    {
        String name = askName("New name for " + plan.getName() + ":", plan.getName(), plan);
        if (name != null)
        {
            plan.setName(name);
            fillPlanBox();
            save();
        }
    }

    private void deletePlan()
    {
        int answer = JOptionPane.showConfirmDialog(this, "Delete the plan " + plan.getName() + " and its "
                + plan.steps().size() + (plan.steps().size() == 1 ? " step?" : " steps?"),
            "Script Queue", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION)
        {
            return;
        }
        plans.remove(plan);
        if (plans.isEmpty())
        {
            plans.add(new QueuePlan(QueuePlan.DEFAULT_NAME));
        }
        plan = plans.get(0);
        fillPlanBox();
        showPlanOptions();
        notes.clear();
        stopEditing();
        save();
        rebuildList();
    }

    // ---- running

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
        if (plan.steps().isEmpty())
        {
            return;
        }
        if (editing >= 0)
        {
            stopEditing();
        }
        notes.clear();
        ScriptQueueRunner.Listener listener = new ScriptQueueRunner.Listener()
        {
            @Override
            public void status(String text)
            {
                setStatus(text);
            }

            @Override
            public void step(int index, ScriptQueueRunner.StepState state, String text)
            {
                SwingUtilities.invokeLater(() -> stepNote(index, state, text));
            }
        };
        runner = new ScriptQueueRunner(store.copy(plan), logout.isSelected(), breakBetween.isSelected(),
            (Integer) pauseMin.getValue(), (Integer) pauseMax.getValue(),
            new LiveQueueHost(eventBus), new LiveQueueGameState(), ScriptQueuePlugin.getPreStartCheck(), listener);
        ScriptQueueRunner started = runner;
        runnerThread = new Thread(() ->
        {
            try
            {
                started.run();
            }
            finally
            {
                SwingUtilities.invokeLater(() ->
                {
                    runner = null;
                    runnerThread = null;
                    startStop.setText("Start queue");
                    // What was running is over: keep how each step ended, drop the live line.
                    notes.values().removeIf(n -> n.state == ScriptQueueRunner.StepState.RUNNING);
                    rebuildList();
                });
            }
        }, "projectx-script-queue");
        runnerThread.setDaemon(true);
        runnerThread.start();
        startStop.setText("Stop queue");
        rebuildList();
    }

    /** A step's progress or result from the runner. Only the step's own line is redrawn while it just ticks along. */
    private void stepNote(int index, ScriptQueueRunner.StepState state, String text)
    {
        if (runner == null || index < 0 || index >= plan.steps().size())
        {
            return;
        }
        Note before = notes.put(index, new Note(state, text));
        if (state == ScriptQueueRunner.StepState.RUNNING)
        {
            // A new round: the steps after this one haven't run in it yet.
            notes.keySet().removeIf(i -> i > index && notes.get(i).state != ScriptQueueRunner.StepState.RUNNING);
        }
        JLabel label = noteLabels.get(index);
        if (label != null && before != null && before.state == state)
        {
            showNote(index, plan.steps().get(index), label);
            return;
        }
        rebuildList();
    }

    /**
     * Start queue, from outside the panel (website control). Swing thread.
     *
     * @param planName the plan to switch to first, or null or blank for the one showing
     * @return null once it is running; otherwise why not
     */
    String startFromOutside(String planName)
    {
        if (runner != null)
        {
            return "the queue is already running";
        }
        if (planName != null && !planName.trim().isEmpty())
        {
            QueuePlan found = ScriptQueueStore.find(plans, planName.trim());
            if (found == null)
            {
                return "there's no plan called " + planName.trim() + " in this client";
            }
            switchTo(found.getName());
            fillPlanBox();
        }
        if (plan.steps().isEmpty())
        {
            return "the plan " + plan.getName() + " has no steps";
        }
        toggle();
        return runner != null ? null : "the queue didn't start";
    }

    /** Stop queue, from outside the panel (website control). Swing thread. */
    String stopFromOutside()
    {
        if (runner == null)
        {
            return "the queue isn't running";
        }
        toggle();
        return null;
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
        SwingUtilities.invokeLater(() -> status.setText("<html><div style='width:170px'>" + escape(text) + "</div></html>"));
    }

    // ---- persistence

    private void optionsChanged()
    {
        if (loading)
        {
            return;
        }
        plan.setLoop(loop.isSelected());
        plan.setRounds((Integer) rounds.getValue());
        rounds.setEnabled(runner == null && loop.isSelected());
        save();
    }

    private void save()
    {
        store.save(plans, plan.getName());
        configManager.setConfiguration(GROUP, "logout", logout.isSelected());
        configManager.setConfiguration(GROUP, "breakBetween", breakBetween.isSelected());
        configManager.setConfiguration(GROUP, "pauseMin", (Integer) pauseMin.getValue());
        configManager.setConfiguration(GROUP, "pauseMax", (Integer) pauseMax.getValue());
    }

    private void load()
    {
        ScriptQueueStore.Plans saved = store.load();
        plans = new ArrayList<>(saved.plans);
        plan = saved.activePlan();
        loading = true;
        try
        {
            logout.setSelected(Boolean.parseBoolean(configManager.getConfiguration(GROUP, "logout")));
            breakBetween.setSelected(Boolean.parseBoolean(configManager.getConfiguration(GROUP, "breakBetween")));
            pauseMin.setValue(intOr(configManager.getConfiguration(GROUP, "pauseMin"), 20));
            pauseMax.setValue(intOr(configManager.getConfiguration(GROUP, "pauseMax"), 90));
        }
        finally
        {
            loading = false;
        }
        showPlanOptions();
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

    private static JComboBox<String> skillBox()
    {
        return new JComboBox<>(Arrays.stream(Skill.values()).map(Skill::getName).toArray(String[]::new));
    }

    /** The scrolling content: as wide as the sidebar, never wider, so nothing is cut off on the right. */
    private static final class FitWidth extends JPanel implements Scrollable
    {
        FitWidth()
        {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize()
        {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction)
        {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction)
        {
            return Math.max(16, visible.height - 32);
        }

        @Override
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight()
        {
            return false;
        }
    }

    /** One row of the editor: full width, its own height, a little space around it. */
    private static JPanel cell(JComponent c)
    {
        JPanel p = new JPanel(new BorderLayout())
        {
            @Override
            public Dimension getMaximumSize()
            {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

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
        JLabel l = new JLabel("<html><div style='width:170px'>" + text + "</div></html>");
        l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        return l;
    }

    /** Text from a script name or an item name, safe inside the labels' HTML. */
    private static String escape(String text)
    {
        return text == null ? "" : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static <T extends JComponent> T left(T c)
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
