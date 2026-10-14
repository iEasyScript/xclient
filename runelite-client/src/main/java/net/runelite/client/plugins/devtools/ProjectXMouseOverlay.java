package net.runelite.client.plugins.devtools;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ui.ProjectXTheme;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * Where the script's mouse is, in the Project X colours: a gold reticle of three
 * arcs turning slowly round a molten X, a gold ripple on each click, and a comet
 * trail that tapers away behind it.
 *
 * Everything is drawn straight onto the frame from the clock. The cursor it
 * replaces built a new image every frame, and turned it from a thread of its own
 * that ran for as long as the client did.
 */
public class ProjectXMouseOverlay extends Overlay {
    private static final double RING_RADIUS = 11;
    private static final double ARC_EXTENT = 80;
    /** Seconds per full turn of the ring. */
    private static final double TURN_SECONDS = 3.2;
    private static final double X_ARM = 4.5;
    private static final long RIPPLE_MS = 380;
    private static final double RIPPLE_FROM = 5;
    private static final double RIPPLE_TO = 20;

    private static final Color OUTLINE = withAlpha(ProjectXTheme.OBSIDIAN_0, 190);
    private static final Stroke OUTLINE_RING = new BasicStroke(4.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke RING = new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke OUTLINE_X = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke X = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private final Client client;
    private final DevToolsPlugin plugin;

    private Point seenClick;
    private Point rippleAt;
    private long rippleStart;

    @Inject
    ProjectXMouseOverlay(Client client, DevToolsPlugin plugin) {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_LOW);
        setNaughty();
    }

    @Override
    public Dimension render(Graphics2D g) {
        var mouse = ProjectX.getMouse();
        if (!plugin.getMouseMovement().isActive()) {
            mouse.getPoints().clear();
            mouse.getTimer().stop();
            return null;
        }
        if (!mouse.getTimer().isRunning()) {
            mouse.getPoints().clear();
            mouse.getTimer().start();
        }

        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Stroke stroke = g.getStroke();
        Color color = g.getColor();
        AffineTransform transform = g.getTransform();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        try {
            long now = System.currentTimeMillis();
            drawTrail(g, mouse.getPoints().toArray(new Point[0]));
            drawRipple(g, mouse.getLastClick(), now);

            Point at = mouse.getLastMove();
            if (at != null && at.getX() >= 0 && at.getY() >= 0) {
                drawReticle(g, at.getX(), at.getY(), now);
            }
        } finally {
            g.setTransform(transform);
            g.setColor(color);
            g.setStroke(stroke);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        }
        return null;
    }

    /** Three gold arcs turning round a molten X; the X's centre is the click point. */
    private static void drawReticle(Graphics2D g, double x, double y, long now) {
        double turn = (now % (long) (TURN_SECONDS * 1000)) / (TURN_SECONDS * 1000) * 360.0;
        // A slow breath in the ring's brightness, so it reads as alive without flashing.
        double breath = 0.5 + 0.5 * Math.sin(now / 420.0);
        Color ring = blend(ProjectXTheme.GOLD, ProjectXTheme.GOLD_HI, breath);

        double r = RING_RADIUS;
        for (int i = 0; i < 3; i++) {
            Arc2D arc = new Arc2D.Double(x - r, y - r, r * 2, r * 2, -turn + i * 120, ARC_EXTENT, Arc2D.OPEN);
            g.setStroke(OUTLINE_RING);
            g.setColor(OUTLINE);
            g.draw(arc);
            g.setStroke(RING);
            g.setColor(ring);
            g.draw(arc);
        }

        Line2D a = new Line2D.Double(x - X_ARM, y - X_ARM, x + X_ARM, y + X_ARM);
        Line2D b = new Line2D.Double(x - X_ARM, y + X_ARM, x + X_ARM, y - X_ARM);
        g.setStroke(OUTLINE_X);
        g.setColor(OUTLINE);
        g.draw(a);
        g.draw(b);
        g.setStroke(X);
        g.setColor(ProjectXTheme.MOLTEN);
        g.draw(a);
        g.draw(b);
    }

    /** A gold ring that grows and fades from each new click. */
    private void drawRipple(Graphics2D g, Point click, long now) {
        if (click != null && click != seenClick) {
            if (seenClick != null && click.getX() >= 0) {
                rippleAt = click;
                rippleStart = now;
            }
            seenClick = click;
        }
        if (rippleAt == null) return;
        double t = (now - rippleStart) / (double) RIPPLE_MS;
        if (t >= 1) {
            rippleAt = null;
            return;
        }
        double eased = 1 - Math.pow(1 - t, 3);
        double r = RIPPLE_FROM + (RIPPLE_TO - RIPPLE_FROM) * eased;
        int alpha = (int) (220 * (1 - t));
        g.setStroke(new BasicStroke((float) (2.6 - 1.6 * t)));
        g.setColor(withAlpha(ProjectXTheme.GOLD_HI, alpha));
        g.draw(new Ellipse2D.Double(rippleAt.getX() - r, rippleAt.getY() - r, r * 2, r * 2));
    }

    /** Thin and faint at the tail, molten and wide at the cursor. */
    private static void drawTrail(Graphics2D g, Point[] points) {
        int n = points.length;
        if (n < 2) return;
        for (int i = 1; i < n; i++) {
            Point p = points[i - 1];
            Point q = points[i];
            if (p == null || q == null) continue;
            double t = i / (double) (n - 1); // 0 at the tail, 1 at the cursor
            g.setStroke(new BasicStroke((float) (0.6 + 2.6 * t), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(blend(ProjectXTheme.GOLD_DIM, ProjectXTheme.MOLTEN, t), (int) (20 + 200 * t * t)));
            g.draw(new Line2D.Double(p.getX(), p.getY(), q.getX(), q.getY()));
        }
    }

    private static Color blend(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
