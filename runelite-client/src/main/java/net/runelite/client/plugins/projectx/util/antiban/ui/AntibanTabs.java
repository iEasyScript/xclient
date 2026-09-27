package net.runelite.client.plugins.projectx.util.antiban.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The section switcher.
 *
 * <p>It replaces a row of six 32-pixel icons -- a cog, a runner, a mask, a
 * mouse, a bed and an hourglass -- which were indistinguishable at that size and
 * whose only explanation was a tooltip. Words fit: six of them in two rows, with
 * the selected one underlined in the accent colour.
 */
class AntibanTabs extends JPanel
{
    private final Map<String, JLabel> tabs = new LinkedHashMap<>();
    private final Consumer<String> onSelect;

    private String selected;

    AntibanTabs(Consumer<String> onSelect, String... names)
    {
        this.onSelect = onSelect;

        setLayout(new GridLayout(0, 3, 1, 1));
        setBackground(AntibanUi.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

        for (String name : names)
        {
            JLabel tab = buildTab(name);
            tabs.put(name, tab);
            add(tab);
        }

        setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
    }

    private JLabel buildTab(String name)
    {
        JLabel tab = new JLabel(name, SwingConstants.CENTER);
        tab.setFont(AntibanUi.small());
        tab.setOpaque(true);
        tab.setBackground(AntibanUi.CARD);
        tab.setForeground(AntibanUi.TEXT_DIM);
        tab.setBorder(unselectedBorder());

        tab.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                select(name);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                if (!name.equals(selected))
                {
                    tab.setForeground(AntibanUi.TEXT);
                }
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                if (!name.equals(selected))
                {
                    tab.setForeground(AntibanUi.TEXT_DIM);
                }
            }
        });

        return tab;
    }

    void select(String name)
    {
        if (name.equals(selected))
        {
            return;
        }
        selected = name;

        tabs.forEach((tabName, tab) ->
        {
            boolean active = tabName.equals(name);
            tab.setForeground(active ? AntibanUi.ACCENT : AntibanUi.TEXT_DIM);
            tab.setBorder(active ? selectedBorder() : unselectedBorder());
        });

        onSelect.accept(name);
    }

    /** The underline is the selection; the two borders keep the text on the same baseline. */
    private static javax.swing.border.Border selectedBorder()
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, AntibanUi.ACCENT),
            BorderFactory.createEmptyBorder(5, 2, 3, 2));
    }

    private static javax.swing.border.Border unselectedBorder()
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, AntibanUi.DIVIDER),
            BorderFactory.createEmptyBorder(5, 2, 3, 2));
    }
}
