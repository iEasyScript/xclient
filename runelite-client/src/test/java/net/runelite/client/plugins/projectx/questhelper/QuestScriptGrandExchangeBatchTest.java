package net.runelite.client.plugins.projectx.questhelper;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuestScriptGrandExchangeBatchTest
{
	@Test
	public void freeToPlaySlotsBuyFirstThreeOfSix()
	{
		List<String> toBuy = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", "f"));
		assertTrue(QuestScript.trimToGrandExchangeSlots(toBuy, 3));
		assertEquals(Arrays.asList("a", "b", "c"), toBuy);
	}

	@Test
	public void enoughSlotsKeepsEveryItem()
	{
		List<String> toBuy = new ArrayList<>(Arrays.asList("a", "b", "c"));
		assertFalse(QuestScript.trimToGrandExchangeSlots(toBuy, 8));
		assertEquals(Arrays.asList("a", "b", "c"), toBuy);
	}

	@Test
	public void exactSlotCountKeepsEveryItem()
	{
		List<String> toBuy = new ArrayList<>(Arrays.asList("a", "b", "c"));
		assertFalse(QuestScript.trimToGrandExchangeSlots(toBuy, 3));
		assertEquals(3, toBuy.size());
	}

	@Test
	public void secondPassBuysTheRemainder()
	{
		List<String> remaining = new ArrayList<>(Arrays.asList("d", "e", "f"));
		assertFalse(QuestScript.trimToGrandExchangeSlots(remaining, 3));
		assertEquals(Arrays.asList("d", "e", "f"), remaining);
	}
}
