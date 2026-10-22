package net.runelite.client.plugins.projectx.util.gameobject;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.api.playerstate.Rs2PlayerStateCache;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Rs2GameObjectNullAnchorTest {

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<Rs2Player> player;
    private Client client;

    @Before
    public void setUp() {
        client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(mock(Player.class));
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClient).thenReturn(client);
        Rs2PlayerStateCache stateCache = mock(Rs2PlayerStateCache.class);
        when(stateCache.getLocalPlayerWorldView()).thenReturn(mock(WorldView.class));
        projectx.when(ProjectX::getRs2PlayerStateCache).thenReturn(stateCache);
        player = Mockito.mockStatic(Rs2Player.class);
        player.when(Rs2Player::getWorldLocation).thenReturn(null);
    }

    @After
    public void tearDown() {
        player.close();
        projectx.close();
    }

    @Test
    public void findBankReturnsNullWhenPlayerLocationUnknown() {
        assertNull(Rs2GameObject.findBank());
        verify(client, never()).getTopLevelWorldView();
    }

    @Test
    public void worldPointQueriesReturnEmptyForNullAnchor() {
        WorldPoint anchor = null;
        assertTrue(Rs2GameObject.getTileObjects(o -> true, anchor, 10).isEmpty());
        assertTrue(Rs2GameObject.getGameObjects(o -> true, anchor, 10).isEmpty());
        assertTrue(Rs2GameObject.getGroundObjects(o -> true, anchor, 10).isEmpty());
        assertTrue(Rs2GameObject.getWallObjects(o -> true, anchor, 10).isEmpty());
        assertTrue(Rs2GameObject.getDecorativeObjects(o -> true, anchor, 10).isEmpty());
        verify(client, never()).getTopLevelWorldView();
    }

    @Test
    public void singleObjectQueriesReturnNullForNullAnchor() {
        WorldPoint anchor = null;
        assertNull(Rs2GameObject.getTileObject(o -> true, anchor, 10));
        assertNull(Rs2GameObject.getGameObject(o -> true, anchor, 10));
        assertNull(Rs2GameObject.getGroundObject(o -> true, anchor, 10));
        assertNull(Rs2GameObject.getWallObject(o -> true, anchor, 10));
        assertNull(Rs2GameObject.getDecorativeObject(o -> true, anchor, 10));
        assertNull(Rs2GameObject.getGameObject(1234, 10));
    }
}
