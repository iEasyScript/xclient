package net.runelite.client.plugins.projectx.util.gameobject;

import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class Rs2CannonOwnershipTest {

    private static final WorldPoint CANNON_CENTER = new WorldPoint(3200, 3200, 0);

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<Rs2Inventory> inventory;
    private MockedStatic<Rs2GameObject> gameObject;
    private GameObject cannon;

    @Before
    public void setUp() {
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any(Callable.class))).thenReturn(Optional.of(0));
        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);

        inventory = Mockito.mockStatic(Rs2Inventory.class);
        inventory.when(() -> Rs2Inventory.hasItemAmount(eq("cannonball"), anyInt(), anyBoolean())).thenReturn(true);

        cannon = mock(GameObject.class);
        when(cannon.getWorldLocation()).thenReturn(CANNON_CENTER);
        gameObject = Mockito.mockStatic(Rs2GameObject.class);
        gameObject.when(() -> Rs2GameObject.findObject(any(Integer[].class))).thenReturn(cannon);
    }

    @After
    public void tearDown() {
        gameObject.close();
        inventory.close();
        projectx.close();
        ProjectX.pauseAllScripts.set(false);
    }

    private void ownCannonAt(int coord) {
        projectx.when(() -> ProjectX.getVarbitPlayerValue(VarPlayerID.OWNEDMCANNON)).thenReturn(coord);
    }

    private static int coordOf(int x, int y, int plane) {
        return (plane << 28) | (x << 14) | y;
    }

    @Test
    public void refillReturnsFalseWhenNoCannonOwned() {
        ownCannonAt(0);
        assertFalse(Rs2Cannon.refill(15));
        assertFalse(ProjectX.pauseAllScripts.get());
        gameObject.verify(() -> Rs2GameObject.interact(any(TileObject.class), anyString()), never());
    }

    @Test
    public void refillReturnsFalseWhenOwnedCannonIsElsewhere() {
        ownCannonAt(coordOf(3300, 3300, 0));
        assertFalse(Rs2Cannon.refill(15));
        assertFalse(ProjectX.pauseAllScripts.get());
        gameObject.verify(() -> Rs2GameObject.interact(any(TileObject.class), anyString()), never());
    }

    @Test
    public void repairReturnsFalseWhenNoCannonOwned() {
        ownCannonAt(-1);
        assertFalse(Rs2Cannon.repair());
        gameObject.verify(() -> Rs2GameObject.interact(any(TileObject.class), anyString()), never());
    }

    @Test
    public void repairInteractsWithOwnedCannonWithoutCannonPlugin() {
        ownCannonAt(coordOf(CANNON_CENTER.getX() - 1, CANNON_CENTER.getY() - 1, CANNON_CENTER.getPlane()));
        assertTrue(Rs2Cannon.repair());
        gameObject.verify(() -> Rs2GameObject.interact((TileObject) cannon, "Repair"));
    }
}
