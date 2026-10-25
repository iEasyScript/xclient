package net.runelite.client.plugins.projectx.util.world;

import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigProfile;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.security.LoginManager;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Rs2WorldUtilSeasonalAccessTest {

    private static final int SEASONAL_WORLD = 495;
    private static final int NORMAL_WORLD = 302;

    private MockedStatic<ProjectX> projectx;
    private MockedStatic<LoginManager> loginManager;
    private Client client;
    private ClientThread clientThread;

    @Before
    public void setUp() {
        client = mock(Client.class);
        ConfigProfile profile = mock(ConfigProfile.class);
        when(profile.isMember()).thenReturn(true);

        WorldResult worldResult = mock(WorldResult.class);
        when(worldResult.getWorlds()).thenReturn(Arrays.asList(
                world(SEASONAL_WORLD, EnumSet.of(WorldType.MEMBERS, WorldType.SEASONAL)),
                world(NORMAL_WORLD, EnumSet.of(WorldType.MEMBERS))));
        WorldService worldService = mock(WorldService.class);
        when(worldService.getWorlds()).thenReturn(worldResult);

        clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            Callable<?> callable = inv.getArgument(0);
            return Optional.ofNullable(callable.call());
        });

        projectx = Mockito.mockStatic(ProjectX.class);
        projectx.when(ProjectX::getClient).thenReturn(client);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
        projectx.when(ProjectX::isLoggedIn).thenReturn(false);
        projectx.when(ProjectX::getWorldService).thenReturn(worldService);

        loginManager = Mockito.mockStatic(LoginManager.class);
        loginManager.when(LoginManager::getActiveProfile).thenReturn(profile);
    }

    @After
    public void tearDown() {
        loginManager.close();
        projectx.close();
    }

    @Test
    public void loggedOutOnASeasonalWorldAcceptsSeasonalWorlds() {
        when(client.getWorldType()).thenReturn(EnumSet.of(net.runelite.api.WorldType.MEMBERS, net.runelite.api.WorldType.SEASONAL));

        assertTrue(Rs2WorldUtil.canAccessWorld(SEASONAL_WORLD));
        assertFalse(Rs2WorldUtil.canAccessWorld(NORMAL_WORLD));
    }

    @Test
    public void loggedOutOnANormalWorldRejectsSeasonalWorlds() {
        when(client.getWorldType()).thenReturn(EnumSet.of(net.runelite.api.WorldType.MEMBERS));

        assertTrue(Rs2WorldUtil.canAccessWorld(NORMAL_WORLD));
        assertFalse(Rs2WorldUtil.canAccessWorld(SEASONAL_WORLD));
    }

    @Test
    public void loggedOutWithoutAWorldTypeTreatsTheClientAsNotSeasonal() {
        when(client.getWorldType()).thenReturn(null);

        assertTrue(Rs2WorldUtil.canAccessWorld(NORMAL_WORLD));
        assertFalse(Rs2WorldUtil.canAccessWorld(SEASONAL_WORLD));
    }

    @Test
    public void loggedOutRejectsEveryWorldWhenTheWorldTypeCannotBeRead() {
        when(client.getWorldType()).thenReturn(EnumSet.of(net.runelite.api.WorldType.MEMBERS, net.runelite.api.WorldType.SEASONAL));
        doReturn(Optional.empty()).when(clientThread).runOnClientThreadOptional(any());

        assertFalse(Rs2WorldUtil.canAccessWorld(NORMAL_WORLD));
        assertFalse(Rs2WorldUtil.canAccessWorld(SEASONAL_WORLD));
    }

    private static World world(int id, EnumSet<WorldType> types) {
        return World.builder().id(id).types(types).players(500).address("x").build();
    }
}
