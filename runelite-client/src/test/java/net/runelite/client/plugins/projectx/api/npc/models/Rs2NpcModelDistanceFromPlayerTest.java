package net.runelite.client.plugins.projectx.api.npc.models;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2NpcModelDistanceFromPlayerTest {

    private MockedStatic<ProjectX> projectx;
    private Client client;
    private NPC npc;

    @Before
    public void setUp() {
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        when(clientThread.invoke(any(Supplier.class))).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        client = mock(Client.class);
        npc = mock(NPC.class);
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
        projectx.when(ProjectX::getClient).thenReturn(client);
    }

    @After
    public void tearDown() {
        projectx.close();
    }

    private void givenLocalPlayerAt(LocalPoint location) {
        Player player = mock(Player.class);
        when(player.getLocalLocation()).thenReturn(location);
        when(client.getLocalPlayer()).thenReturn(player);
    }

    @Test
    public void nullLocalPlayerIsUnreachable() {
        when(client.getLocalPlayer()).thenReturn(null);
        when(npc.getLocalLocation()).thenReturn(new LocalPoint(128, 128, -1));

        Rs2NpcModel model = new Rs2NpcModel(npc);
        assertEquals(Integer.MAX_VALUE, model.getDistanceFromPlayer());
        assertFalse(model.isWithinDistanceFromPlayer(Integer.MAX_VALUE));
    }

    @Test
    public void nullLocalPlayerLocationIsUnreachable() {
        givenLocalPlayerAt(null);
        when(npc.getLocalLocation()).thenReturn(new LocalPoint(128, 128, -1));

        Rs2NpcModel model = new Rs2NpcModel(npc);
        assertEquals(Integer.MAX_VALUE, model.getDistanceFromPlayer());
        assertFalse(model.isWithinDistanceFromPlayer(Integer.MAX_VALUE));
    }

    @Test
    public void nullNpcLocationIsUnreachable() {
        givenLocalPlayerAt(new LocalPoint(128, 128, -1));
        when(npc.getLocalLocation()).thenReturn(null);

        Rs2NpcModel model = new Rs2NpcModel(npc);
        assertEquals(Integer.MAX_VALUE, model.getDistanceFromPlayer());
        assertFalse(model.isWithinDistanceFromPlayer(Integer.MAX_VALUE));
    }

    @Test
    public void measuresDistanceWhenBothLocationsPresent() {
        givenLocalPlayerAt(new LocalPoint(128, 128, -1));
        when(npc.getLocalLocation()).thenReturn(new LocalPoint(128, 128 + 3 * 128, -1));

        Rs2NpcModel model = new Rs2NpcModel(npc);
        assertEquals(3 * 128, model.getDistanceFromPlayer());
        assertTrue(model.isWithinDistanceFromPlayer(3 * 128));
        assertFalse(model.isWithinDistanceFromPlayer(3 * 128 - 1));
    }

    @Test
    public void legacyModelNullLocalPlayerIsUnreachable() {
        when(client.getLocalPlayer()).thenReturn(null);
        when(npc.getLocalLocation()).thenReturn(new LocalPoint(128, 128, -1));

        net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel model =
                new net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel(npc);
        assertEquals(Integer.MAX_VALUE, model.getDistanceFromPlayer());
        assertFalse(model.isWithinDistanceFromPlayer(Integer.MAX_VALUE));
    }

    @Test
    public void legacyModelNullNpcLocationIsUnreachable() {
        givenLocalPlayerAt(new LocalPoint(128, 128, -1));
        when(npc.getLocalLocation()).thenReturn(null);

        net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel model =
                new net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel(npc);
        assertEquals(Integer.MAX_VALUE, model.getDistanceFromPlayer());
        assertFalse(model.isWithinDistanceFromPlayer(Integer.MAX_VALUE));
    }
}
