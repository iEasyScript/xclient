package net.runelite.client.plugins.projectx.util.grandexchange;

import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

	@Test
	public void getConfirmReturnsNullWhenOfferContainerMissing() {
		try (MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class)) {
			widgets.when(() -> Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 26)).thenReturn(null);
			assertNull(GrandExchangeWidget.getConfirm());
		}
	}

	@Test
	public void getConfirmReturnsNullWhenOfferContainerHasNoDynamicChildren() {
		Widget container = mock(Widget.class);
		when(container.getDynamicChildren()).thenReturn(null);
		try (MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class)) {
			widgets.when(() -> Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 26)).thenReturn(container);
			assertNull(GrandExchangeWidget.getConfirm());
		}
	}

	@Test
	public void getConfirmFindsButtonInOfferContainer() {
		Widget container = mock(Widget.class);
		Widget button = mock(Widget.class);
		when(container.getDynamicChildren()).thenReturn(new Widget[]{button});
		try (MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class)) {
			widgets.when(() -> Rs2Widget.getWidget(InterfaceID.GE_OFFERS, 26)).thenReturn(container);
			widgets.when(() -> Rs2Widget.findWidget(eq("Confirm"), anyList(), anyBoolean())).thenReturn(button);
			assertSame(button, GrandExchangeWidget.getConfirm());
		}
	}
}
