package net.runelite.client.plugins.projectx.api.npc.models;

import net.runelite.api.NPC;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2NpcModelGetIdThreadTest {

    private MockedStatic<ProjectX> projectx;
    private final AtomicBoolean onClientThread = new AtomicBoolean(false);
    private ClientThread clientThread;

    @Before
    public void setUp() {
        clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            onClientThread.set(true);
            try {
                return Optional.ofNullable(callable.call());
            } finally {
                onClientThread.set(false);
            }
        });
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
    }

    @After
    public void tearDown() {
        projectx.close();
    }

    @Test
    public void readsNpcIdOnClientThread() {
        NPC npc = mock(NPC.class);
        when(npc.getId()).thenAnswer(inv -> {
            if (!onClientThread.get()) {
                throw new NullPointerException("NPC.getId called off the client thread");
            }
            return 12345;
        });

        assertEquals(12345, new Rs2NpcModel(npc).getId());
    }

    @Test
    public void returnsMinusOneWhenClientThreadReturnsNothing() {
        doReturn(Optional.empty()).when(clientThread).runOnClientThreadOptional(any());
        NPC npc = mock(NPC.class);

        assertEquals(-1, new Rs2NpcModel(npc).getId());
    }
}
