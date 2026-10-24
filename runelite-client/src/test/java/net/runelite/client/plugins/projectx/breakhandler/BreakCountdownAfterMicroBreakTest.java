package net.runelite.client.plugins.projectx.breakhandler;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Micro breaks must not restart the countdown to the next normal break. They did, and with the
 * antiban's micro breaks coming every few minutes a normal break (and its logout) never came.
 */
public class BreakCountdownAfterMicroBreakTest
{
	@Test
	public void normalBreakRestartsTheCountdown()
	{
		assertTrue(BreakHandlerScript.restartCountdownAfterBreak(false, 1200));
		assertTrue(BreakHandlerScript.restartCountdownAfterBreak(false, 0));
	}

	@Test
	public void microBreakKeepsTheCountdownGoing()
	{
		assertFalse(BreakHandlerScript.restartCountdownAfterBreak(true, 1200));
	}

	@Test
	public void microBreakAfterTheCountdownRanOutStartsANewOne()
	{
		assertTrue(BreakHandlerScript.restartCountdownAfterBreak(true, 0));
		assertTrue(BreakHandlerScript.restartCountdownAfterBreak(true, -1));
	}
}
