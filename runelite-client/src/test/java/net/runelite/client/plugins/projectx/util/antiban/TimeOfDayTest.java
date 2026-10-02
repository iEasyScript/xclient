package net.runelite.client.plugins.projectx.util.antiban;

import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TimeOfDayTest {

	@Test
	public void noSlowdownThroughTheDay() {
		for (int hour = 12; hour <= 18; hour++) {
			assertEquals(1.0, TimeOfDay.multiplier(LocalTime.of(hour, 0)), 1e-9);
		}
	}

	@Test
	public void slowestInTheSmallHours() {
		assertEquals(1.45, TimeOfDay.multiplier(LocalTime.of(3, 0)), 1e-9);
		for (int minute = 0; minute < 24 * 60; minute += 15) {
			double m = TimeOfDay.multiplier(LocalTime.of(minute / 60, minute % 60));
			assertTrue("multiplier " + m + " at minute " + minute, m >= 1.0 && m <= 1.45 + 1e-9);
		}
	}

	@Test
	public void continuousAcrossMidnight() {
		double justBefore = TimeOfDay.multiplier(LocalTime.of(23, 59, 59));
		double midnight = TimeOfDay.multiplier(LocalTime.MIDNIGHT);
		assertEquals(midnight, justBefore, 0.001);
	}

	@Test
	public void interpolatesBetweenPoints() {
		// Halfway from 18:00 (1.00) to 21:00 (1.10).
		assertEquals(1.05, TimeOfDay.multiplier(LocalTime.of(19, 30)), 1e-9);
	}

	@Test
	public void applyToStretchesButNeverShortens() {
		assertEquals(0, TimeOfDay.applyTo(0, LocalTime.of(3, 0)));
		assertEquals(10, TimeOfDay.applyTo(10, LocalTime.NOON));
		assertEquals(15, TimeOfDay.applyTo(10, LocalTime.of(3, 0)));
		assertEquals(1, TimeOfDay.applyTo(1, LocalTime.of(9, 0)));
	}
}
