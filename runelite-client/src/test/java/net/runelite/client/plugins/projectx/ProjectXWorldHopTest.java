package net.runelite.client.plugins.projectx;

import net.runelite.api.Client;
import net.runelite.api.World;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectXWorldHopTest {

    private MockedStatic<ProjectX> projectx;
    private Client client;
    private World target;
    private final AtomicInteger clientThreadCalls = new AtomicInteger();
    private final AtomicBoolean switcherLoaded = new AtomicBoolean();
    private final AtomicInteger openCall = new AtomicInteger(-1);
    private final AtomicInteger hopCall = new AtomicInteger(-1);

    @Before
    public void setUp() {
        client = mock(Client.class);
        target = mock(World.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            clientThreadCalls.incrementAndGet();
            Callable<?> callable = inv.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        Widget buttons = mock(Widget.class);
        when(client.getWidget(InterfaceID.Worldswitcher.BUTTONS)).thenAnswer(inv -> switcherLoaded.get() ? buttons : null);
        doAnswer(inv -> {
            openCall.set(clientThreadCalls.get());
            return null;
        }).when(client).openWorldHopper();
        doAnswer(inv -> {
            hopCall.set(clientThreadCalls.get());
            return null;
        }).when(client).hopToWorld(any());

        projectx = Mockito.mockStatic(ProjectX.class, Mockito.CALLS_REAL_METHODS);
        projectx.when(ProjectX::getClient).thenReturn(client);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
    }

    @After
    public void tearDown() {
        projectx.close();
    }

    @Test
    public void opensSwitcherThenHopsOnceListHasLoaded() {
        doAnswer(inv -> {
            openCall.set(clientThreadCalls.get());
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                switcherLoaded.set(true);
            }).start();
            return null;
        }).when(client).openWorldHopper();

        assertTrue(ProjectX.hopViaWorldSwitcher(target, 3000));

        InOrder order = inOrder(client);
        order.verify(client, atLeastOnce()).openWorldHopper();
        order.verify(client).hopToWorld(target);
        assertNotEquals(openCall.get(), hopCall.get());
    }

    @Test
    public void hopsImmediatelyWhenSwitcherAlreadyOpen() {
        switcherLoaded.set(true);

        assertTrue(ProjectX.hopViaWorldSwitcher(target, 3000));

        verify(client, never()).openWorldHopper();
        verify(client).hopToWorld(target);
    }

    @Test
    public void doesNotHopWhenSwitcherNeverLoads() {
        assertFalse(ProjectX.hopViaWorldSwitcher(target, 300));

        verify(client, atLeastOnce()).openWorldHopper();
        verify(client, never()).hopToWorld(any());
        assertEquals(-1, hopCall.get());
    }
}
