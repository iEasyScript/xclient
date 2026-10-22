package net.runelite.client.plugins.projectx.util.walker;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2WalkerProximityWaitTest
{
	private static final WorldPoint BEFORE = new WorldPoint(3200, 3200, 0);
	private static final WorldPoint CLICKED = new WorldPoint(3212, 3200, 0);

	@Test
	public void nullPlayerLocationKeepsWaitingWithoutThrowing()
	{
		assertFalse(Rs2Walker.proximityWaitSatisfied(null, CLICKED, BEFORE, 3, 16));
	}

	@Test
	public void wakesWhenWithinProximityOfClickedTarget()
	{
		assertTrue(Rs2Walker.proximityWaitSatisfied(new WorldPoint(3210, 3200, 0), CLICKED, BEFORE, 3, 16));
	}

	@Test
	public void keepsWaitingWhileFarFromTargetAndBelowProgressCap()
	{
		assertFalse(Rs2Walker.proximityWaitSatisfied(new WorldPoint(3204, 3200, 0), CLICKED, BEFORE, 3, 16));
	}

	@Test
	public void wakesWhenProgressCapReached()
	{
		assertTrue(Rs2Walker.proximityWaitSatisfied(new WorldPoint(3200, 3216, 0), CLICKED, BEFORE, 3, 16));
	}
}
