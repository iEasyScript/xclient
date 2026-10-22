package net.runelite.client.plugins.projectx.util.camera;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2CameraAngleToTileTest {

    private static final int WEST_CLIENT_YAW = 512 * 8;

    private MockedStatic<ProjectX> projectx;
    private Client client;

    @Before
    public void setUp() {
        client = mock(Client.class);
        when(client.isClientThread()).thenReturn(true);
        when(client.getCameraYaw()).thenReturn(WEST_CLIENT_YAW);

        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.invoke(any(Supplier.class))).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
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

    @Test
    public void worldPointWithoutLocalPlayerKeepsCurrentFacing() {
        when(client.getLocalPlayer()).thenReturn(null);

        int angle = Rs2Camera.angleToTile(new WorldPoint(3200, 3210, 0));

        assertEquals(180, angle);
        assertEquals(0, Rs2Camera.getAngleTo(angle - 90));
    }

    @Test
    public void localPointWithoutLocalPlayerKeepsCurrentFacing() {
        when(client.getLocalPlayer()).thenReturn(null);

        assertEquals(180, Rs2Camera.angleToTile(new LocalPoint(6400, 6400)));
    }

    @Test
    public void worldPointWithLocalPlayerUsesBearingFromPlayer() {
        WorldView worldView = mock(WorldView.class);
        when(worldView.isTopLevel()).thenReturn(true);
        Player player = mock(Player.class);
        when(player.getWorldView()).thenReturn(worldView);
        when(player.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));
        when(client.getLocalPlayer()).thenReturn(player);

        assertEquals(90, Rs2Camera.angleToTile(new WorldPoint(3200, 3210, 0)));
        assertEquals(0, Rs2Camera.angleToTile(new WorldPoint(3210, 3200, 0)));
        assertEquals(270, Rs2Camera.angleToTile(new WorldPoint(3200, 3190, 0)));
    }
}
