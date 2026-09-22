package net.runelite.client.plugins.projectx.ui;

import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.client.ui.Activatable;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public abstract class ProjectXPluginPanel extends JPanel implements Activatable
{
    public static final int PANEL_WIDTH = 235;
    public static final int SCROLLBAR_WIDTH = 17;
    public static final int BORDER_OFFSET = 6;
    private static final EmptyBorder BORDER_PADDING = new EmptyBorder(BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET, BORDER_OFFSET);
    private static final Dimension OUTER_PREFERRED_SIZE = new Dimension(net.runelite.client.ui.PluginPanel.PANEL_WIDTH + SCROLLBAR_WIDTH, 0);

    @Getter(AccessLevel.PROTECTED)
    private final JScrollPane scrollPane;

    @Getter
    private final JPanel wrappedPanel;

    protected ProjectXPluginPanel()
    {
        this(true);
    }

    protected ProjectXPluginPanel(boolean wrap)
    {
        super();
        if (wrap)
        {
            setBorder(BORDER_PADDING);
            setLayout(new DynamicGridLayout(0, 1, 0, 3));
            setBackground(ColorScheme.DARK_GRAY_COLOR);

            final JPanel northPanel = new JPanel();
            northPanel.setLayout(new BorderLayout());
            northPanel.add(this, BorderLayout.NORTH);
            northPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

            scrollPane = new JScrollPane(northPanel);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            wrappedPanel = new JPanel();

            // Adjust the preferred size to expand to width of scrollbar to
            // prevent scrollbar overlapping over contents
            wrappedPanel.setPreferredSize(OUTER_PREFERRED_SIZE);
            wrappedPanel.setLayout(new BorderLayout());
            wrappedPanel.add(scrollPane, BorderLayout.CENTER);
        }
        else
        {
            scrollPane = null;
            wrappedPanel = this;
        }
    }

    @Override
    public Dimension getPreferredSize()
    {
        int width = this == wrappedPanel ? PANEL_WIDTH + SCROLLBAR_WIDTH : PANEL_WIDTH;
        return new Dimension(width, super.getPreferredSize().height);
    }

    @Override
    public Dimension getMinimumSize()
    {
        int width = this == wrappedPanel ? PANEL_WIDTH + SCROLLBAR_WIDTH : PANEL_WIDTH;
        return new Dimension(width, super.getMinimumSize().height);
    }
}

