package net.runelite.client.plugins.projectx.util.bank;

import net.runelite.api.Client;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Rs2BankUnloadedWidgetsTest {

    private MockedStatic<ProjectX> projectx;
    private Client client;
    private ClientThread clientThread;

    @Before
    public void setUp() {
        client = mock(Client.class);
        clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClient).thenReturn(client);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
    }

    @After
    public void tearDown() {
        projectx.close();
    }

    private void bankSizeText(String text) {
        Widget size = mock(Widget.class);
        when(size.getText()).thenReturn(text);
        when(client.getWidget(ComponentID.BANK_ITEM_COUNT_TOP)).thenReturn(size);
    }

    @Test
    public void bankItemCountIsZeroWhenSizeTextIsBlank() {
        bankSizeText("");
        assertEquals(0, Rs2Bank.getBankItemCount());
        bankSizeText(null);
        assertEquals(0, Rs2Bank.getBankItemCount());
        bankSizeText("abc");
        assertEquals(0, Rs2Bank.getBankItemCount());
    }

    @Test
    public void bankItemCountParsesPopulatedText() {
        bankSizeText(" 412 ");
        assertEquals(412, Rs2Bank.getBankItemCount());
    }

    @Test
    public void scrollBankToSlotIsFalseWithoutItemContainer() {
        when(client.getWidget(Rs2Bank.BANK_ITEM_CONTAINER)).thenReturn(null);
        assertFalse(Rs2Bank.scrollBankToSlot(10));
        verify(clientThread, never()).invokeLater(any(Runnable.class));
    }

    @Test
    public void scrollBankToSlotSurvivesContainerDisappearingMidCalculation() {
        Widget container = mock(Widget.class);
        when(client.getWidget(Rs2Bank.BANK_ITEM_CONTAINER)).thenReturn(container, (Widget) null);
        assertTrue(Rs2Bank.scrollBankToSlot(10));
        verify(container).setScrollY(0);
        verify(container, never()).getHeight();
        verify(clientThread).invokeLater(any(Runnable.class));
    }
}
