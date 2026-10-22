package net.runelite.client.plugins.projectx.util.grounditem;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Callable;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.models.RS2Item;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2GroundItemNullLocalPlayerTest {

    private static final WorldPoint CENTER = new WorldPoint(3200, 3200, 0);

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<Rs2Tile> rs2Tile;
    private Client client;

    @Before
    public void setUp() {
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        client = mock(Client.class);
        ItemManager itemManager = mock(ItemManager.class);
        when(itemManager.getItemComposition(anyInt())).thenReturn(mock(ItemComposition.class));
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
        projectx.when(ProjectX::getClient).thenReturn(client);
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        projectx.when(ProjectX::getItemManager).thenReturn(itemManager);
        rs2Tile = Mockito.mockStatic(Rs2Tile.class);
    }

    @After
    public void tearDown() {
        rs2Tile.close();
        projectx.close();
    }

    private Tile tileWithItemAt(int x, int y, LocalPoint location) {
        Tile tile = mock(Tile.class);
        when(tile.getLocalLocation()).thenReturn(location);
        when(tile.getGroundItems()).thenReturn(Collections.singletonList(mock(TileItem.class)));
        rs2Tile.when(() -> Rs2Tile.getTile(x, y)).thenReturn(tile);
        return tile;
    }

    private void givenLocalPlayerAt(LocalPoint location) {
        Player player = mock(Player.class);
        when(player.getLocalLocation()).thenReturn(location);
        when(client.getLocalPlayer()).thenReturn(player);
    }

    @Test
    public void nullLocalPlayerReturnsNoItems() {
        tileWithItemAt(3200, 3201, new LocalPoint(128, 256, -1));
        tileWithItemAt(3200, 3202, new LocalPoint(128, 384, -1));
        when(client.getLocalPlayer()).thenReturn(null);

        assertEquals(0, Rs2GroundItem.getAllFromWorldPoint(5, CENTER).length);
    }

    @Test
    public void nullLocalPlayerLocationReturnsNoItems() {
        tileWithItemAt(3200, 3201, new LocalPoint(128, 256, -1));
        tileWithItemAt(3200, 3202, new LocalPoint(128, 384, -1));
        givenLocalPlayerAt(null);

        assertEquals(0, Rs2GroundItem.getAllFromWorldPoint(5, CENTER).length);
    }

    @Test
    public void sortsItemsClosestToLocalPlayerFirst() {
        Tile far = tileWithItemAt(3200, 3198, new LocalPoint(128, 128 + 4 * 128, -1));
        Tile near = tileWithItemAt(3200, 3203, new LocalPoint(128, 128 + 128, -1));
        givenLocalPlayerAt(new LocalPoint(128, 128, -1));

        RS2Item[] items = Rs2GroundItem.getAllFromWorldPoint(5, CENTER);

        assertEquals(2, items.length);
        assertSame(near, items[0].getTile());
        assertSame(far, items[1].getTile());
    }
}
