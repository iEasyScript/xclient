package net.runelite.client.plugins.projectx.util.world;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.shop.Rs2Shop;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class Rs2WorldUtilHopWorldTest {

    @Test
    public void failedHopIsAttemptedOncePerAttempt() {
        Client client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        WorldHoppingConfig config = WorldHoppingConfig.builder()
                .maxAttemptsPerWorld(2)
                .baseHopDelay(10)
                .maxHopDelay(10)
                .build();

        try (MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<Rs2Shop> shop = Mockito.mockStatic(Rs2Shop.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class);
             MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::isLoggedIn).thenReturn(true);
            projectx.when(() -> ProjectX.hopToWorld(anyInt())).thenReturn(false);
            projectx.when(ProjectX::isHopping).thenReturn(false);
            player.when(Rs2Player::getWorld).thenReturn(301);

            assertFalse(Rs2WorldUtil.hopWorld(null, 302, 0, config));

            projectx.verify(() -> ProjectX.hopToWorld(302), times(2));
        }
    }

    @Test
    public void waitsForLoginBeforeHopping() {
        Client client = mock(Client.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        WorldHoppingConfig config = WorldHoppingConfig.builder()
                .maxAttemptsPerWorld(1)
                .baseHopDelay(10)
                .maxHopDelay(10)
                .build();
        AtomicInteger world = new AtomicInteger(301);

        try (MockedStatic<ProjectX> projectx = Mockito.mockStatic(ProjectX.class, Mockito.CALLS_REAL_METHODS);
             MockedStatic<Rs2Shop> shop = Mockito.mockStatic(Rs2Shop.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class);
             MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::isLoggedIn).thenReturn(false, false, true);
            projectx.when(() -> ProjectX.hopToWorld(anyInt())).thenAnswer(invocation -> {
                if (!ProjectX.isLoggedIn()) {
                    return false;
                }
                world.set(invocation.getArgument(0));
                return true;
            });
            projectx.when(ProjectX::isHopping).thenReturn(false);
            player.when(Rs2Player::getWorld).thenAnswer(invocation -> world.get());

            assertTrue(Rs2WorldUtil.hopWorld(null, 302, 0, config));

            projectx.verify(() -> ProjectX.hopToWorld(302), times(1));
        }
    }
}
