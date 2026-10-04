package net.runelite.client.plugins.projectx.util.overlay;

import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A script's on-screen panel, in the Project X house style: a title bar with the version and the
 * run time, a status line, then sections of label/value rows and progress bars.
 *
 * <p>Built afresh every frame -- it is a list of lines, cheap to make -- and drawn in one go:
 *
 * <pre>{@code
 * ScriptPanel.create("Agility", version)
 *         .startedAt(script.getStartedAt())
 *         .status(script.getStatus())
 *         .section("EXPERIENCE").skill(skills, Skill.AGILITY)
 *         .section("SESSION").row("Laps", laps + " (" + ScriptPanel.perHour(laps, skills.elapsedMs()) + "/h)")
 *         .render(graphics);
 * }</pre>
 *
 * <p>Usually used through {@link ScriptPanelOverlay}, which owns the overlay boilerplate and keeps a
 * mistake in one script's panel from taking the whole overlay down.
 */
public final class ScriptPanel {

	/** Colour schemes. Same layout, different accent, so scripts can be told apart at a glance. */
	public enum Theme {
		ORANGE(new Color(232, 128, 48), new Color(58, 52, 46), new Color(34, 31, 28), new Color(70, 52, 34)),
		PURPLE(new Color(214, 92, 204), new Color(128, 50, 120), new Color(78, 28, 76), new Color(96, 44, 92)),
		GREEN(new Color(110, 200, 100), new Color(46, 70, 44), new Color(28, 40, 28), new Color(52, 80, 48)),
		BLUE(new Color(96, 168, 240), new Color(40, 60, 92), new Color(26, 36, 56), new Color(46, 70, 104)),
		RED(new Color(230, 86, 76), new Color(92, 40, 36), new Color(54, 24, 22), new Color(104, 46, 40)),
		GOLD(new Color(236, 196, 88), new Color(82, 66, 34), new Color(46, 38, 22), new Color(96, 78, 40));

		final Color accent;
		final Color headerTop;
		final Color headerBottom;
		final Color border;

		Theme(Color accent, Color headerTop, Color headerBottom, Color border) {
			this.accent = accent;
			this.headerTop = headerTop;
			this.headerBottom = headerBottom;
			this.border = border;
		}
	}

	private static final int PAD = 8;
	private static final int HEADER_HEIGHT = 26;
	private static final int LINE = 16;
	private static final int SECTION_GAP = 6;
	private static final int ARC = 6;

	private static final Color BACKGROUND = new Color(24, 22, 22, 228);
	private static final Color LABEL = new Color(200, 196, 190);
	private static final Color VALUE = Color.WHITE;
	private static final Color MUTED = new Color(150, 146, 140);
	private static final Color BAR_BACK = new Color(255, 255, 255, 40);
	private static final Color SHADOW = new Color(0, 0, 0, 200);

	/** For values that need to stand out: a warning, or something going well. */
	public static final Color WARN = new Color(255, 170, 80);
	public static final Color GOOD = new Color(120, 220, 120);
	public static final Color BAD = new Color(240, 100, 90);

	private final String title;
	private final List<Line> lines = new ArrayList<>();
	private Theme theme = Theme.ORANGE;
	private BufferedImage icon;
	private long startedAt;
	private String status;
	private int width = 300;
	private int labelWidth = 104;

	private ScriptPanel(String title) {
		this.title = title;
	}

	/** A panel titled "{@code name} V {@code version}". */
	public static ScriptPanel create(String name, String version) {
		return new ScriptPanel(version == null || version.isEmpty() ? name : name + " V " + version);
	}

	public ScriptPanel theme(Theme theme) {
		this.theme = theme;
		return this;
	}

	/** A small image in the title bar, such as the item being made. Null for none. */
	public ScriptPanel icon(BufferedImage icon) {
		this.icon = icon;
		return this;
	}

	/** When the run began, in epoch millis. Drives the clock in the title bar; 0 shows 00:00:00. */
	public ScriptPanel startedAt(long startedAt) {
		this.startedAt = startedAt;
		return this;
	}

	/** One line under the title saying what the script is doing. Null or blank shows "Idle". */
	public ScriptPanel status(String status) {
		this.status = status;
		return this;
	}

	public ScriptPanel width(int width) {
		this.width = Math.max(220, width);
		return this;
	}

	/** Where values start, in pixels from the left edge. Widen it for long labels. */
	public ScriptPanel labelWidth(int labelWidth) {
		this.labelWidth = labelWidth;
		return this;
	}

	/** A heading in the accent colour, with a rule after it. */
	public ScriptPanel section(String title) {
		lines.add(new Line(Kind.SECTION, title, null, null, 0));
		return this;
	}

	/** A label and its value. A null value shows a muted dash. */
	public ScriptPanel row(String label, Object value) {
		return row(label, value, null);
	}

	/** A label and its value, the value in the given colour. */
	public ScriptPanel row(String label, Object value, Color color) {
		lines.add(new Line(Kind.ROW, label == null ? "" : label, value == null ? null : String.valueOf(value), color, 0));
		return this;
	}

	/** A progress bar, {@code fraction} between 0 and 1, with text to its right such as "64%". */
	public ScriptPanel bar(String label, double fraction, String text) {
		return bar(label, fraction, text, GOOD);
	}

	public ScriptPanel bar(String label, double fraction, String text, Color fill) {
		lines.add(new Line(Kind.BAR, label == null ? "" : label, text, fill, Math.max(0, Math.min(1, fraction))));
		return this;
	}

	/** Two rows for one skill: XP gained and XP/hr, then levels gained and time to level. */
	public ScriptPanel skill(SkillTracker tracker, Skill skill) {
		row(skill.getName(), "XP: " + compact(tracker.gained(skill)) + " (" + compact(tracker.perHour(skill)) + "/hr)");
		long ttl = tracker.msToLevel(skill);
		String ttlText = tracker.level(skill) >= 99 ? "max" : ttl < 0 ? "-" : duration(ttl);
		return row("", "LVLS +" + tracker.levelsGained(skill) + " · TTL: " + ttlText);
	}

	/**
	 * One row per skill trained this run, most XP first -- for scripts that train whatever the
	 * player is using, like combat. Shows "None yet" until something gains XP.
	 */
	public ScriptPanel trainedSkills(SkillTracker tracker, int limit) {
		return trainedSkills(tracker, limit, new Skill[0]);
	}

	/** As {@link #trainedSkills(SkillTracker, int)}, leaving out skills already shown elsewhere. */
	public ScriptPanel trainedSkills(SkillTracker tracker, int limit, Skill... except) {
		List<Skill> trained = tracker.trained();
		trained.removeAll(java.util.Arrays.asList(except));
		if (trained.isEmpty()) return row("Experience", null);
		for (Skill skill : trained.subList(0, Math.min(limit, trained.size()))) {
			int levels = tracker.levelsGained(skill);
			row(skill.getName(), compact(tracker.gained(skill)) + " xp (" + compact(tracker.perHour(skill)) + "/hr)"
					+ (levels > 0 ? " · +" + levels : ""));
		}
		return this;
	}

	// ------------------------------------------------------------------ drawing

	public Dimension render(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Font plain = FontManager.getRunescapeFont();
		Font bold = FontManager.getRunescapeBoldFont();

		int height = HEADER_HEIGHT + LINE + 6 + PAD;
		for (Line line : lines) height += line.kind == Kind.SECTION ? SECTION_GAP + LINE : LINE;

		g.setColor(BACKGROUND);
		g.fillRoundRect(0, 0, width, height, ARC, ARC);
		g.setPaint(new GradientPaint(0, 0, theme.headerTop, 0, HEADER_HEIGHT, theme.headerBottom));
		g.fillRoundRect(0, 0, width, HEADER_HEIGHT, ARC, ARC);
		g.fillRect(0, HEADER_HEIGHT - ARC, width, ARC);
		g.setColor(theme.accent);
		g.drawLine(0, HEADER_HEIGHT, width - 1, HEADER_HEIGHT);
		g.setColor(theme.border);
		g.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC);

		int titleX = PAD;
		if (icon != null) {
			g.drawImage(icon, PAD - 2, 1, 27, 24, null);
			titleX = PAD + 28;
		}
		g.setFont(plain);
		String clock = clock(startedAt > 0 ? System.currentTimeMillis() - startedAt : 0);
		int clockWidth = g.getFontMetrics().stringWidth(clock);
		text(g, clock, width - PAD - clockWidth, 18, VALUE);
		g.setFont(bold);
		text(g, fit(title, width - titleX - clockWidth - 2 * PAD, g.getFontMetrics()), titleX, 18, VALUE);

		int y = HEADER_HEIGHT + 15;
		g.setFont(plain);
		String shown = status == null || status.isBlank() ? "Idle" : status;
		text(g, fit(shown, width - 2 * PAD, g.getFontMetrics()), PAD, y, LABEL);
		y += 6;

		for (Line line : lines) {
			switch (line.kind) {
				case SECTION:
					y += SECTION_GAP + LINE;
					g.setFont(bold);
					text(g, line.label, PAD, y - 3, theme.accent);
					int ruleX = PAD + g.getFontMetrics().stringWidth(line.label) + 8;
					g.setColor(new Color(theme.accent.getRed(), theme.accent.getGreen(), theme.accent.getBlue(), 170));
					g.drawLine(ruleX, y - 8, width - PAD, y - 8);
					break;
				case ROW:
					y += LINE;
					g.setFont(plain);
					if (!line.label.isEmpty()) {
						text(g, fit(line.label, labelWidth - PAD - 4, g.getFontMetrics()), PAD, y - 3, LABEL);
					}
					String value = line.value == null ? "-" : line.value;
					Color color = line.value == null ? MUTED : line.color != null ? line.color : VALUE;
					text(g, fit(value, width - labelWidth - PAD, g.getFontMetrics()), labelWidth, y - 3, color);
					break;
				case BAR:
					y += LINE;
					g.setFont(plain);
					text(g, line.label, PAD, y - 3, LABEL);
					String barText = line.value == null ? "" : line.value;
					int textWidth = g.getFontMetrics().stringWidth(barText);
					int barX = labelWidth, barW = width - labelWidth - PAD - textWidth - 8, barY = y - 11, barH = 7;
					g.setColor(BAR_BACK);
					g.fillRoundRect(barX, barY, barW, barH, barH, barH);
					g.setColor(line.color != null ? line.color : GOOD);
					g.fillRoundRect(barX, barY, (int) Math.max(barH, barW * line.fraction), barH, barH, barH);
					text(g, barText, width - PAD - textWidth, y - 3, VALUE);
					break;
			}
		}
		return new Dimension(width, height);
	}

	// ------------------------------------------------------------------ formatting

	/** 950, 54.5k, 1.2M. */
	public static String compact(long n) {
		if (n < 1_000) return Long.toString(n);
		if (n < 1_000_000) return String.format("%.1fk", n / 1_000d);
		return String.format("%.1fM", n / 1_000_000d);
	}

	/** How many an hour, given a count and how long it took. */
	public static long perHour(long count, long elapsedMs) {
		return elapsedMs > 0 ? Math.round(count * 3_600_000d / elapsedMs) : 0;
	}

	/** "count (n/h)", the usual way to show a tally on the panel. */
	public static String withRate(long count, long elapsedMs) {
		return String.format("%,d", count) + " (" + compact(perHour(count, elapsedMs)) + "/h)";
	}

	/** 24:04:09. */
	public static String clock(long ms) {
		long s = Math.max(0, ms / 1000);
		return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
	}

	/** 6h 4m, or 4m 12s under an hour. */
	public static String duration(long ms) {
		long s = Math.max(0, ms / 1000);
		if (s >= 3600) return (s / 3600) + "h " + ((s / 60) % 60) + "m";
		return (s / 60) + "m " + (s % 60) + "s";
	}

	private static void text(Graphics2D g, String s, int x, int y, Color color) {
		g.setColor(SHADOW);
		g.drawString(s, x + 1, y + 1);
		g.setColor(color);
		g.drawString(s, x, y);
	}

	private static String fit(String s, int maxWidth, FontMetrics fm) {
		if (s == null) return "";
		if (fm.stringWidth(s) <= maxWidth) return s;
		int end = s.length();
		while (end > 0 && fm.stringWidth(s.substring(0, end) + "...") > maxWidth) end--;
		return s.substring(0, end) + "...";
	}

	private enum Kind { SECTION, ROW, BAR }

	private static final class Line {
		final Kind kind;
		final String label;
		final String value;
		final Color color;
		final double fraction;

		Line(Kind kind, String label, String value, Color color, double fraction) {
			this.kind = kind;
			this.label = label;
			this.value = value;
			this.color = color;
			this.fraction = fraction;
		}
	}
}
