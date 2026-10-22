package net.runelite.client.plugins.projectx.util.player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class Rs2PlayerInstanceWorldLocationTest {
    private final Map<String, Object> original = new HashMap<>();
    private Client client;
    private WorldView worldView;
    private Player player;

    @Before
    public void setUp() throws Exception {
        client = mock(Client.class);
        ClientThread clientThread = mock(ClientThread.class);
        worldView = mock(WorldView.class);
        Scene scene = mock(Scene.class);
        player = mock(Player.class);
        replace("client", client);
        replace("clientThread", clientThread);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(invocation ->
                Optional.ofNullable(((Callable<?>) invocation.getArgument(0)).call()));
        when(client.getLocalPlayer()).thenReturn(player);
        when(client.getTopLevelWorldView()).thenReturn(worldView);
        when(client.getWorldView(anyInt())).thenReturn(worldView);
        when(worldView.getScene()).thenReturn(scene);
        when(scene.isInstance()).thenReturn(true);
        when(worldView.getPlane()).thenReturn(1);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
    }

    private void replace(String name, Object value) throws Exception {
        Field field = ProjectX.class.getDeclaredField(name);
        field.setAccessible(true);
        original.put(name, field.get(null));
        field.set(null, value);
    }

    @After
    public void tearDown() throws Exception {
        for (Map.Entry<String, Object> entry : original.entrySet()) {
            Field field = ProjectX.class.getDeclaredField(entry.getKey());
            field.setAccessible(true);
            field.set(null, entry.getValue());
        }
    }

    @Test
    public void fallsBackToWorldLocationWhenNoLocalPointResolves() {
        when(player.getLocalLocation()).thenReturn(null);
        assertEquals(new WorldPoint(3200, 3200, 0), Rs2Player.getWorldLocation_Internal());
    }

    @Test
    public void fallsBackToPlayerLocalLocationWhenWorldPointIsNotInTopLevelScene() {
        when(worldView.isInstance()).thenReturn(false);
        when(worldView.getBaseX()).thenReturn(3000);
        when(worldView.getBaseY()).thenReturn(3100);
        when(player.getLocalLocation()).thenReturn(LocalPoint.fromScene(10, 20));
        assertEquals(new WorldPoint(3010, 3120, 1), Rs2Player.getWorldLocation_Internal());
    }
}
