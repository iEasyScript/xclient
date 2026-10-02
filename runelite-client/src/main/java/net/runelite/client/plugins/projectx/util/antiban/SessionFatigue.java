package net.runelite.client.plugins.projectx.util.antiban;

/**
 * Slows waits down the longer a script session runs, the way a tired player does.
 *
 * <p>Always on while a session runs, as detection hardening rather than an antiban option -- the
 * "Simulate fatigue" toggle governs mouse fatigue only. How fast it builds and where it stops are the
 * {@code fatigueSlowdownPerHour} and {@code fatigueMaxSlowdown} settings, shared with the mouse;
 * {@link #HOURLY_SLOWDOWN} and {@link #MAX_MULTIPLIER} are their defaults.
 */
public final class SessionFatigue {
	static final double HOURLY_SLOWDOWN = 0.04;
	static final double MAX_MULTIPLIER = 1.30;

	private static volatile long sessionStartNanos = 0L;

	private SessionFatigue() {
	}

	public static void startSession() {
		sessionStartNanos = System.nanoTime();
	}

	public static void endSession() {
		sessionStartNanos = 0L;
	}

	public static boolean isActive() {
		return sessionStartNanos != 0L;
	}

	public static double multiplier() {
		long start = sessionStartNanos;
		if (start == 0L) return 1.0;
		double elapsedMinutes = (System.nanoTime() - start) / 1_000_000_000.0 / 60.0;
		return forSettings(elapsedMinutes);
	}

	/** The multiplier after this many minutes, at the strength the user has set. */
	static double forSettings(double elapsedMinutes) {
		return computeMultiplier(elapsedMinutes,
				Rs2AntibanSettings.fatigueSlowdownPerHour / 100.0,
				1.0 + Rs2AntibanSettings.fatigueMaxSlowdown / 100.0);
	}

	static double computeMultiplier(double elapsedMinutes) {
		return computeMultiplier(elapsedMinutes, HOURLY_SLOWDOWN, MAX_MULTIPLIER);
	}

	static double computeMultiplier(double elapsedMinutes, double hourlySlowdown, double maxMultiplier) {
		if (elapsedMinutes <= 0.0 || hourlySlowdown <= 0.0 || maxMultiplier <= 1.0) return 1.0;
		double extra = Math.min(maxMultiplier - 1.0, elapsedMinutes / 60.0 * hourlySlowdown);
		return 1.0 + extra;
	}

	public static int applyTo(int baseMs) {
		if (baseMs <= 0) return baseMs;
		return (int) Math.round(baseMs * multiplier());
	}
}
