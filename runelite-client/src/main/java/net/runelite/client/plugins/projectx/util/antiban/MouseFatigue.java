package net.runelite.client.plugins.projectx.util.antiban;

import java.util.Random;

/**
 * Slows mouse movement the longer the player has been active since logging in.
 *
 * <p>It used to add a fixed 0.005ms per tick, which nobody could tune and which bore no relation to the
 * session fatigue applied to waits. It now uses the same curve and the same two settings as
 * {@link SessionFatigue}, measured in active play: {@code AntibanPlugin} winds {@code ticksSinceLogin} back
 * over idle stretches while fatigue is on, so a rest recovers some of it.
 */
public class MouseFatigue {

    private final Random random = new Random();
    public double noiseAmplitude = 5.0;

    /** How much slower mouse movement is right now; 1.0 is not at all. */
    public static double currentMultiplier() {
        double activeMinutes = AntibanPlugin.ticksSinceLogin * 0.6 / 60.0;
        return SessionFatigue.forSettings(activeMinutes);
    }

    // Method to calculate the base time with noise
    public int calculateBaseTimeWithNoise(int initialBaseTimeMs, int maxBaseTimeMs) {
        double noise = random.nextGaussian() * noiseAmplitude;
        int newBaseTimeMs = (int) Math.round(initialBaseTimeMs * currentMultiplier() + noise);
        return Math.min(newBaseTimeMs, maxBaseTimeMs);
    }
}
