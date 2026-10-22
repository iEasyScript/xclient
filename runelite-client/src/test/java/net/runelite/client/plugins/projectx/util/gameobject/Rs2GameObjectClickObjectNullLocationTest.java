package net.runelite.client.plugins.projectx.util.gameobject;

import net.runelite.api.Client;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2GameObjectClickObjectNullLocationTest {

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<Rs2Player> player;
    private MockedStatic<Rs2Walker> walker;
    private boolean savedCantReachTarget;

    @Before
    public void setUp() {
        savedCantReachTarget = ProjectX.cantReachTarget;
        ProjectX.cantReachTarget = false;
        Client client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(null);
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClient).thenReturn(client);
        player = Mockito.mockStatic(Rs2Player.class);
        walker = Mockito.mockStatic(Rs2Walker.class);
    }

    @After
    public void tearDown() {
        walker.close();
        player.close();
        projectx.close();
        ProjectX.cantReachTarget = savedCantReachTarget;
    }

    @Test
    public void returnsFalseWhenPlayerLocationUnknown() {
        player.when(Rs2Player::getWorldLocation).thenReturn(null);
        TileObject object = mock(TileObject.class);
        when(object.getWorldLocation()).thenReturn(new WorldPoint(3200, 3200, 0));

        assertFalse(Rs2GameObject.clickObject(object, "Bank"));
        walker.verify(() -> Rs2Walker.walkTo(any(WorldPoint.class)), Mockito.never());
    }

    @Test
    public void returnsFalseWhenObjectLocationUnknown() {
        player.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3200, 3200, 0));
        TileObject object = mock(TileObject.class);
        when(object.getWorldLocation()).thenReturn(null);

        assertFalse(Rs2GameObject.clickObject(object, "Bank"));
        walker.verify(() -> Rs2Walker.walkTo(any(WorldPoint.class)), Mockito.never());
    }
}
