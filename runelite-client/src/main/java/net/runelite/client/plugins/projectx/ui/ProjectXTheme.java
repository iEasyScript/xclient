package net.runelite.client.plugins.projectx.ui;

import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * The Project X palette, as on xclient.dev and in the launcher: worked gold, cracked
 * obsidian, molten fire. Keep these in step with {@code web/src/app/globals.css}.
 *
 * <p>The client-wide accent and greys live in {@link net.runelite.client.ui.ColorScheme},
 * which the look-and-feel is built from; these are for Project X's own panels.
 */
public final class ProjectXTheme
{
    public static final Color OBSIDIAN_0 = new Color(0x07080b);
    public static final Color OBSIDIAN_1 = new Color(0x0d0f14);
    public static final Color OBSIDIAN_2 = new Color(0x13161d);
    public static final Color OBSIDIAN_3 = new Color(0x1a1e27);

    public static final Color GOLD = new Color(0xc8a04a);
    public static final Color GOLD_HI = new Color(0xecd08a);
    public static final Color GOLD_DIM = new Color(0x8a6f34);
    public static final Color GOLD_DEEP = new Color(0x5a4520);

    public static final Color MOLTEN = new Color(0xff6a1f);
    public static final Color MOLTEN_HI = new Color(0xff9a52);

    public static final Color INK = new Color(0xece7dc);
    public static final Color INK_DIM = new Color(0xa7a194);
    public static final Color INK_FAINT = new Color(0x736e63);

    public static final Color RUNNING = new Color(0x6ee7a0);
    public static final Color DANGER = new Color(0xf87171);

    private ProjectXTheme()
    {
    }

    /**
     * The heading a Project X sidebar panel opens with: its icon, a gold title, and a
     * line saying what it is for. The same shape as the home tab's, so the tabs read
     * as one product.
     */
    public static JPanel header(String title, String subtitle, BufferedImage icon)
    {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, GOLD_DEEP),
            BorderFactory.createEmptyBorder(0, 0, 8, 0)));
        if (icon != null)
        {
            header.add(new JLabel(new ImageIcon(ImageUtil.resizeImage(icon, 28, 28))), BorderLayout.WEST);
        }
        JPanel words = new JPanel();
        words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
        words.setOpaque(false);
        JLabel name = new JLabel(title.toUpperCase());
        name.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
        name.setForeground(GOLD_HI);
        words.add(name);
        if (subtitle != null)
        {
            JLabel line = new JLabel(subtitle);
            line.setFont(FontManager.getRunescapeSmallFont());
            line.setForeground(INK_DIM);
            words.add(line);
        }
        header.add(words, BorderLayout.CENTER);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return header;
    }

    /** A section's titled border: a dim gold edge and a gold title. */
    public static TitledBorder section(String title)
    {
        TitledBorder border = new TitledBorder(BorderFactory.createLineBorder(GOLD_DEEP), title);
        border.setTitleColor(GOLD);
        return border;
    }

    /** The one primary action on a panel: gold, like the site's. */
    public static void stylePrimary(JButton button)
    {
        button.setFocusPainted(false);
        button.setForeground(OBSIDIAN_0);
        button.setBackground(GOLD);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, GOLD_HI),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }
}
