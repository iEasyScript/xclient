package net.runelite.client.plugins.projectx.util.antiban.enums;

import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Regression test for the action-cooldown stall.
 *
 * <p>evolvePlayStyle() drifts primaryTickInterval and secondaryTickInterval independently, so over
 * time they can cross (primary greater than secondary). getRandomTickInterval() must not throw
 * IllegalArgumentException from ThreadLocalRandom.nextInt(origin, bound) when that happens; before
 * the fix it did, which left ProjectX.pauseAllScripts stuck true and froze running scripts.
 */
public class PlayStyleRandomTickIntervalTest {

	private static void setInterval(PlayStyle style, String field, int value) throws Exception {
		Field f = PlayStyle.class.getDeclaredField(field);
		f.setAccessible(true);
		f.setInt(style, value);
	}

	private static int getInterval(PlayStyle style, String field) throws Exception {
		Field f = PlayStyle.class.getDeclaredField(field);
		f.setAccessible(true);
		return f.getInt(style);
	}

	@Test
	public void getRandomTickIntervalDoesNotThrowWhenIntervalsCross() throws Exception {
		// A specific constant (not values()[0]) so enum reordering cannot change what is tested.
		// The intervals are restored in the finally block, so the shared enum singleton is left
		// untouched for other tests; the unit-test suite runs single-threaded.
		PlayStyle style = PlayStyle.MODERATE;
		int origPrimary = getInterval(style, "primaryTickInterval");
		int origSecondary = getInterval(style, "secondaryTickInterval");
		try {
			// Crossed bounds (primary greater than secondary): this is what previously threw.
			setInterval(style, "primaryTickInterval", 10);
			setInterval(style, "secondaryTickInterval", 5);
			for (int i = 0; i < 10_000; i++) {
				int v = style.getRandomTickInterval();
				assertTrue("interval " + v + " must fall within [5,10]", v >= 5 && v <= 10);
			}

			// Equal bounds edge case (primary == secondary).
			setInterval(style, "primaryTickInterval", 7);
			setInterval(style, "secondaryTickInterval", 7);
			assertEquals(7, style.getRandomTickInterval());
		} catch (IllegalArgumentException e) {
			fail("getRandomTickInterval threw on crossed intervals: " + e.getMessage());
		} finally {
			setInterval(style, "primaryTickInterval", origPrimary);
			setInterval(style, "secondaryTickInterval", origSecondary);
		}
	}
}
