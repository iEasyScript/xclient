package net.runelite.client.plugins.projectx.ui;

import java.awt.Color;

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
}
