package net.runelite.client.plugins.projectx.util.antiban;

import java.time.LocalTime;

/**
 * How much slower a person plays at a given time of day, as a multiplier on the action cooldown.
 *
 * <p>Reaction time and attention dip at night and recover through the morning, so the same player pauses
 * longer at 3am than at 3pm. The curve below is a coarse version of that: no slowdown through the working
 * day, a little in the evening, most in the small hours. Points in between are interpolated so the cooldown
 * drifts across the evening instead of stepping at the hour.
 */
public final class TimeOfDay {
    /** {hour of day, multiplier} -- must start at 0 and end at 24 with matching multipliers. */
    private static final double[][] CURVE = {
            {0, 1.30},
            {3, 1.45},
            {6, 1.30},
            {9, 1.05},
            {12, 1.00},
            {18, 1.00},
            {21, 1.10},
            {24, 1.30},
    };

    private TimeOfDay() {
    }

    /** The multiplier for the local time now. */
    public static double multiplier() {
        return multiplier(LocalTime.now());
    }

    static double multiplier(LocalTime time) {
        double hour = time.toSecondOfDay() / 3600.0;
        for (int i = 0; i < CURVE.length - 1; i++) {
            double[] from = CURVE[i];
            double[] to = CURVE[i + 1];
            if (hour <= to[0]) {
                double t = (hour - from[0]) / (to[0] - from[0]);
                return from[1] + t * (to[1] - from[1]);
            }
        }
        return CURVE[CURVE.length - 1][1];
    }

    /**
     * Stretches a cooldown by the multiplier for now. A zero-tick cooldown stays zero, and a stretched one is
     * never shorter than the original.
     */
    public static int applyTo(int ticks) {
        return applyTo(ticks, LocalTime.now());
    }

    static int applyTo(int ticks, LocalTime time) {
        if (ticks <= 0) return ticks;
        return Math.max(ticks, (int) Math.round(ticks * multiplier(time)));
    }

    /** A word for the part of the day, for the panel. */
    public static String describe(LocalTime time) {
        int hour = time.getHour();
        if (hour < 5) return "Night";
        if (hour < 9) return "Early morning";
        if (hour < 12) return "Morning";
        if (hour < 18) return "Afternoon";
        if (hour < 21) return "Evening";
        return "Late evening";
    }
}
