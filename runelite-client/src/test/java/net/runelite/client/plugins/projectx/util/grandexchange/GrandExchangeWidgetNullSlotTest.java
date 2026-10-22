package net.runelite.client.plugins.projectx.util.grandexchange;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class GrandExchangeWidgetNullSlotTest {
	@Test
	public void getSlotReturnsNullForNullSlot() {
		assertNull(GrandExchangeWidget.getSlot(null));
	}

	@Test
	public void getOfferBuyButtonReturnsNullForNullSlot() {
		assertNull(GrandExchangeWidget.getOfferBuyButton(null));
	}

	@Test
	public void getOfferSellButtonReturnsNullForNullSlot() {
		assertNull(GrandExchangeWidget.getOfferSellButton(null));
	}
}
