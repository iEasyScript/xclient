package net.runelite.client.plugins.projectx.util.inventory;

import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.projectx.util.shop.Rs2Shop;
import net.runelite.client.plugins.projectx.util.tabs.Rs2Tab;
import net.runelite.client.plugins.projectx.util.widget.Rs2Widget;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2InventoryInvokeMenuTest {

    private static final int SHOP_INVENTORY_ITEM_CONTAINER = 19726336;

    @Test
    public void sellItemFallsBackToItemActionsWhenNoContainerChildMatchesSlot() {
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
                Optional.ofNullable(((Callable<?>) invocation.getArgument(0)).call()));

        Widget otherSlot = mock(Widget.class);
        when(otherSlot.getIndex()).thenReturn(0);
        Widget container = mock(Widget.class);
        when(container.getChildren()).thenReturn(new Widget[]{otherSlot, null});

        Rs2ItemModel item = mock(Rs2ItemModel.class);
        when(item.getName()).thenReturn("Pot");
        when(item.getId()).thenReturn(1931);
        when(item.getSlot()).thenReturn(5);
        when(item.getInventoryActions()).thenReturn(new String[]{"Value", "Sell 1", "Sell 5", "Sell 10", "Sell 50"});

        try (MockedStatic<Rs2Inventory> inventory = Mockito.mockStatic(Rs2Inventory.class, Answers.CALLS_REAL_METHODS);
             MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class);
             MockedStatic<Rs2Widget> widgets = Mockito.mockStatic(Rs2Widget.class);
             MockedStatic<Rs2Tab> tabs = Mockito.mockStatic(Rs2Tab.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class);
             MockedStatic<Rs2GrandExchange> ge = Mockito.mockStatic(Rs2GrandExchange.class);
             MockedStatic<Rs2Shop> shop = Mockito.mockStatic(Rs2Shop.class))
        {
            projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
            shop.when(Rs2Shop::isOpen).thenReturn(true);
            widgets.when(() -> Rs2Widget.getWidget(SHOP_INVENTORY_ITEM_CONTAINER)).thenReturn(container);
            inventory.when(() -> Rs2Inventory.get("Pot", true)).thenReturn(item);
            inventory.when(Rs2Inventory::isItemSelected).thenReturn(false);
            inventory.when(() -> Rs2Inventory.itemBounds(item)).thenReturn(null);

            assertTrue(Rs2Inventory.sellItem("Pot", "1"));

            projectx.verify(() -> ProjectX.doInvoke(argThat(entry ->
                    entry.getIdentifier() == 2
                            && entry.getParam0() == 5
                            && entry.getParam1() == SHOP_INVENTORY_ITEM_CONTAINER), any()));
        }
    }
}
