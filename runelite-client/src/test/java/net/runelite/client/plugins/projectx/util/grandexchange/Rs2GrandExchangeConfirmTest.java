package net.runelite.client.plugins.projectx.util.grandexchange;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.plugins.projectx.util.Global;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;

public class Rs2GrandExchangeConfirmTest {
	@Test
	public void sellItemReportsFailureWhenOfferContainerClosesBeforeConfirm() {
		try (MockedStatic<Rs2GrandExchange> ge = Mockito.mockStatic(Rs2GrandExchange.class, Answers.CALLS_REAL_METHODS);
			 MockedStatic<Rs2Inventory> inventory = Mockito.mockStatic(Rs2Inventory.class);
			 MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class);
			 MockedStatic<Global> global = Mockito.mockStatic(Global.class, invocation ->
				 invocation.getMethod().getReturnType() == boolean.class ? true : Answers.RETURNS_DEFAULTS.answer(invocation)))
		{
			ge.when(Rs2GrandExchange::isOpen).thenReturn(true);
			ge.when(Rs2GrandExchange::getAvailableSlots).thenReturn(new GrandExchangeSlots[]{GrandExchangeSlots.ONE});
			ge.when(Rs2GrandExchange::getOfferPrice).thenReturn(100L);
			ge.when(Rs2GrandExchange::getOfferQuantity).thenReturn(5);
			ge.when(Rs2GrandExchange::isOfferSetupOpen).thenReturn(false);
			inventory.when(() -> Rs2Inventory.hasItem("Oak plank", false)).thenReturn(true);
			inventory.when(() -> Rs2Inventory.interact("Oak plank", "Offer", false)).thenReturn(true);
			widgets.when(() -> Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 26)).thenReturn(null);

			assertFalse(Rs2GrandExchange.sellItem("Oak plank", 5, 100));
			widgets.verify(() -> Rs2Widget.clickWidget(anyString()), Mockito.never());
		}
	}
}
