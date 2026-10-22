package net.runelite.client.plugins.projectx.util.camera;

import net.runelite.api.Client;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
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

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class Rs2CameraTileOnScreenTest {

    private static final int WORLD_VIEW_ID = WorldView.TOPLEVEL;
    private static final int SCENE_SIZE = 104;

    private MockedStatic<ProjectX> projectx;
    private Client client;
    private WorldView worldView;

    @Before
    public void setUp() {
        client = mock(Client.class);
        worldView = mock(WorldView.class);
        when(worldView.getId()).thenReturn(WORLD_VIEW_ID);
        when(worldView.isTopLevel()).thenReturn(true);
        when(worldView.getSizeX()).thenReturn(SCENE_SIZE);
        when(worldView.getSizeY()).thenReturn(SCENE_SIZE);
        when(worldView.getTileHeight(anyInt(), anyInt(), anyInt())).thenThrow(new ArrayIndexOutOfBoundsException("Index 111 out of bounds for length 105"));
        when(client.getWorldView(WORLD_VIEW_ID)).thenReturn(worldView);
        when(client.getViewportWidth()).thenReturn(765);
        when(client.getViewportHeight()).thenReturn(503);

        ClientThread clientThread = mock(ClientThread.class);
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
    public void localPointPastSceneEdgeIsNotOnScreen() {
        assertFalse(Rs2Camera.isTileOnScreen(sceneTile(111, 50)));
        assertFalse(Rs2Camera.isTileOnScreen(sceneTile(50, 126)));
        verify(worldView, never()).getTileHeight(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void localPointBeforeSceneOriginIsNotOnScreen() {
        assertFalse(Rs2Camera.isTileOnScreen(sceneTile(-5, 50)));
        assertFalse(Rs2Camera.isTileOnScreen(sceneTile(50, -5)));
        verify(worldView, never()).getTileHeight(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void localPointInUnloadedWorldViewIsNotOnScreen() {
        LocalPoint localPoint = new LocalPoint(50 * 128 + 64, 50 * 128 + 64, 7);
        assertFalse(Rs2Camera.isTileOnScreen(localPoint));
        verify(worldView, never()).getTileHeight(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void nullLocalPointIsNotOnScreen() {
        assertFalse(Rs2Camera.isTileOnScreen((LocalPoint) null));
    }

    @Test
    public void tileObjectPastSceneEdgeIsNotOnScreen() {
        TileObject tileObject = mock(TileObject.class);
        when(tileObject.getLocalLocation()).thenReturn(sceneTile(109, 50));
        assertFalse(Rs2Camera.isTileOnScreen(tileObject));
        verify(worldView, never()).getTileHeight(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void localPointInsideSceneStillReachesPerspective() {
        Scene scene = mock(Scene.class);
        when(scene.getExtendedTileSettings()).thenReturn(new byte[4][184][184]);
        when(worldView.getScene()).thenReturn(scene);
        doReturn(0).when(worldView).getTileHeight(anyInt(), anyInt(), anyInt());
        Rs2Camera.isTileOnScreen(sceneTile(103, 103));
        verify(worldView, atLeastOnce()).getTileHeight(anyInt(), anyInt(), anyInt());
    }

    private static LocalPoint sceneTile(int sceneX, int sceneY) {
        return new LocalPoint(sceneX * 128 + 64, sceneY * 128 + 64, WORLD_VIEW_ID);
    }
}
