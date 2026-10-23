package net.runelite.client.plugins.projectx.util.antiban;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * An action cooldown waits while the player is busy, but only for a while. Mining guardian fragments
 * never ends by itself, so a cooldown that waited for "not busy" never ran, and with the universal
 * antiban every script stayed paused while the player mined on.
 */
public class ActionCooldownBusyCapTest
{
	@Test
	public void countsDownWhenNotBusy()
	{
		assertTrue(AntibanPlugin.cooldownCountsDown(false, 0));
	}

	@Test
	public void waitsWhileBusyUpToTheCap()
	{
		assertFalse(AntibanPlugin.cooldownCountsDown(true, 0));
		assertFalse(AntibanPlugin.cooldownCountsDown(true, AntibanPlugin.MAX_BUSY_TICKS_PER_COOLDOWN - 1));
	}

	@Test
	public void countsDownOnceBusyForTooLong()
	{
		assertTrue(AntibanPlugin.cooldownCountsDown(true, AntibanPlugin.MAX_BUSY_TICKS_PER_COOLDOWN));
	}
}
