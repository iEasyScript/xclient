package net.runelite.client.plugins.projectx.util.misc;

import java.time.Duration;
import java.time.Instant;

public class TimeUtils {
    /**
     * Get formatted duration between two instants
     *
     * @param start
     * @param finish
     * @return duration as string formatted to d:mm:ss
     */
	public static String getFormattedDurationBetween(Instant start, Instant finish) {
		Duration duration = Duration.between(start, finish);
		return getFormattedDuration(duration);
	}

    public static long getDurationInSeconds(Instant scriptStartTime, Instant now) {
        Duration duration = Duration.between(scriptStartTime, now);
        return duration.getSeconds();
    }

	public static String getFormattedDuration(Duration duration) {
		boolean isNegative = duration.isNegative();
		duration = duration.abs();
		String formattedDuration = String.format("%d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
		return (isNegative ? "-" : "") + formattedDuration;
	}
}