package net.runelite.client.plugins.projectx.util.shop;

import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2ShopItemBoundsTest {

    private static final int SHOP_ITEMS_WIDGET = 19660816;
    private static final int ITEM_ID = 1931;

    private MockedStatic<Rs2Widget> widgets;
    private MockedStatic<ProjectX> projectx;
    private List<Rs2ItemModel> previousShopItems;

    @Before
    public void setUp() {
        widgets = Mockito.mockStatic(Rs2Widget.class);
        projectx = Mockito.mockStatic(ProjectX.class);
        previousShopItems = Rs2Shop.shopItems;
        Rs2ItemModel item = mock(Rs2ItemModel.class);
        when(item.getId()).thenReturn(ITEM_ID);
        when(item.getName()).thenReturn("Pot");
        when(item.getQuantity()).thenReturn(5);
        when(item.getSlot()).thenReturn(11);
        Rs2Shop.shopItems = new ArrayList<>(Collections.singletonList(item));
    }

    @After
    public void tearDown() {
        Rs2Shop.shopItems = previousShopItems;
        projectx.close();
        widgets.close();
    }

    private void assertInvokedWith(Rectangle expected) {
        assertTrue(Rs2Shop.buyItem(ITEM_ID, "1"));
        projectx.verify(() -> ProjectX.doInvoke(any(), eq(expected)));
        projectx.verify(() -> ProjectX.logStackTrace(anyString(), any(Exception.class)), Mockito.never());
    }

    @Test
    public void buyFallsBackToDefaultBoundsWhenShopWidgetMissing() {
        widgets.when(() -> Rs2Widget.getWidget(SHOP_ITEMS_WIDGET)).thenReturn(null);
        assertInvokedWith(new Rectangle(1, 1));
    }

    @Test
    public void buyFallsBackToDefaultBoundsWhenShopChildrenNotPopulated() {
        Widget container = mock(Widget.class);
        when(container.getDynamicChildren()).thenReturn(new Widget[0]);
        widgets.when(() -> Rs2Widget.getWidget(SHOP_ITEMS_WIDGET)).thenReturn(container);
        assertInvokedWith(new Rectangle(1, 1));
    }

    @Test
    public void buyFallsBackToDefaultBoundsWhenShopChildrenNull() {
        Widget container = mock(Widget.class);
        when(container.getDynamicChildren()).thenReturn(null);
        widgets.when(() -> Rs2Widget.getWidget(SHOP_ITEMS_WIDGET)).thenReturn(container);
        assertInvokedWith(new Rectangle(1, 1));
    }

    @Test
    public void buyUsesSlotBoundsWhenShopChildrenPopulated() {
        Rectangle slotBounds = new Rectangle(40, 60, 32, 32);
        Widget slot = mock(Widget.class);
        when(slot.getBounds()).thenReturn(slotBounds);
        Widget[] children = new Widget[13];
        children[12] = slot;
        Widget container = mock(Widget.class);
        when(container.getDynamicChildren()).thenReturn(children);
        widgets.when(() -> Rs2Widget.getWidget(SHOP_ITEMS_WIDGET)).thenReturn(container);
        assertInvokedWith(slotBounds);
    }
}
