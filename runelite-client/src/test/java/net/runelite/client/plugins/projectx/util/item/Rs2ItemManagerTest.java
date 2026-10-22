package net.runelite.client.plugins.projectx.util.item;

import java.util.Collections;
import java.util.List;
import net.runelite.http.api.item.ItemPrice;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Rs2ItemManagerTest
{
	private static Rs2ItemManager withResults(List<ItemPrice> results)
	{
		return new Rs2ItemManager()
		{
			@Override
			public List<ItemPrice> searchItem(String query)
			{
				return results;
			}
		};
	}

	@Test
	public void getItemIdReturnsMinusOneForBlankName()
	{
		Rs2ItemManager manager = new Rs2ItemManager();
		assertEquals(-1, manager.getItemId(""));
		assertEquals(-1, manager.getItemId("   "));
		assertEquals(-1, manager.getItemId(null));
	}

	@Test
	public void getItemIdReturnsMinusOneWhenNothingMatches()
	{
		assertEquals(-1, withResults(Collections.emptyList()).getItemId("Not an item"));
	}

	@Test
	public void getGEPriceSkipsLookupWhenNothingMatches()
	{
		assertEquals(-1, withResults(Collections.emptyList()).getGEPrice("Not an item"));
	}

	@Test
	public void getItemIdReturnsFirstMatch()
	{
		ItemPrice first = new ItemPrice();
		first.setId(4151);
		ItemPrice second = new ItemPrice();
		second.setId(12773);
		assertEquals(4151, withResults(List.of(first, second)).getItemId("Abyssal whip"));
	}
}
