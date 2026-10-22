package net.runelite.client.plugins.projectx.util.gameobject;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2GameObjectSmallWorldViewTest {

    private static final int BOAT_SIZE = 8;

    private MockedStatic<ProjectX> projectx;
    private Tile[][][] tiles;

    @Before
    public void setUp() {
        tiles = new Tile[4][BOAT_SIZE][BOAT_SIZE];

        Scene scene = mock(Scene.class);
        when(scene.getTiles()).thenReturn(tiles);
        WorldView worldView = mock(WorldView.class);
        when(worldView.getScene()).thenReturn(scene);
        when(worldView.getPlane()).thenReturn(0);
        Player player = mock(Player.class);
        when(player.getWorldView()).thenReturn(worldView);
        Client client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(player);

        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.invoke(any(Supplier.class))).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());

        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClient).thenReturn(client);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
    }

    @After
    public void tearDown() {
        projectx.close();
    }

    @Test
    public void wallObjectQueryOnBoatWorldViewDoesNotOverflowTileArray() {
        int last = BOAT_SIZE - 1;
        LocalPoint location = new LocalPoint(last * 128 + 64, last * 128 + 64);
        WallObject wall = mock(WallObject.class);
        when(wall.getLocalLocation()).thenReturn(location);
        Tile tile = mock(Tile.class);
        when(tile.getWallObject()).thenReturn(wall);
        when(tile.getLocalLocation()).thenReturn(location);
        tiles[0][last][last] = tile;

        List<WallObject> walls = Rs2GameObject.getWallObjects(o -> true, location, 10 * 128);

        assertEquals(1, walls.size());
        assertSame(wall, walls.get(0));
    }
}
