package net.runelite.client.plugins.projectx;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.combat.Rs2Combat;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;
import net.runelite.client.plugins.projectx.util.shop.Rs2Shop;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectXWorldHopTest {

    private MockedStatic<ProjectX> projectx;
    private Client client;
    private World target;
    private final AtomicInteger clientThreadCalls = new AtomicInteger();
    private final AtomicBoolean switcherLoaded = new AtomicBoolean();
    private final AtomicInteger openCall = new AtomicInteger(-1);
    private final AtomicInteger hopCall = new AtomicInteger(-1);

    @Before
    public void setUp() {
        client = mock(Client.class);
        target = mock(World.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(clientThread.runOnClientThreadOptional(any())).thenAnswer(inv -> {
            clientThreadCalls.incrementAndGet();
            Callable<?> callable = inv.getArgument(0);
            return Optional.ofNullable(callable.call());
        });
        Widget buttons = mock(Widget.class);
        when(client.getWidget(InterfaceID.Worldswitcher.BUTTONS)).thenAnswer(inv -> switcherLoaded.get() ? buttons : null);
        doAnswer(inv -> {
            openCall.set(clientThreadCalls.get());
            return null;
        }).when(client).openWorldHopper();
        doAnswer(inv -> {
            hopCall.set(clientThreadCalls.get());
            return null;
        }).when(client).hopToWorld(any());

        projectx = Mockito.mockStatic(ProjectX.class, Mockito.CALLS_REAL_METHODS);
        projectx.when(ProjectX::getClient).thenReturn(client);
        projectx.when(ProjectX::getClientThread).thenReturn(clientThread);
    }

    @After
    public void tearDown() {
        ProjectX.cantHopWorld = false;
        projectx.close();
    }

    private static final WorldPoint REFUSED_AT = new WorldPoint(3221, 3218, 0);

    private void stubWorlds() {
        net.runelite.http.api.worlds.World f2p = mock(net.runelite.http.api.worlds.World.class);
        when(f2p.getTypes()).thenReturn(EnumSet.noneOf(WorldType.class));
        net.runelite.http.api.worlds.World members = mock(net.runelite.http.api.worlds.World.class);
        when(members.getTypes()).thenReturn(EnumSet.of(WorldType.MEMBERS));
        WorldResult result = mock(WorldResult.class);
        when(result.findWorld(308)).thenReturn(f2p);
        when(result.findWorld(302)).thenReturn(members);
        WorldService worldService = mock(WorldService.class);
        when(worldService.getWorlds()).thenReturn(result);
        projectx.when(ProjectX::getWorldService).thenReturn(worldService);
    }

    @Test
    public void membersAreaRefusalBlocksNonMembersHopFromTheSameTile() {
        stubWorlds();
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        when(client.getWorld()).thenReturn(301);
        ProjectX.markNonMembersHopRefused(REFUSED_AT);
        try (MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class);
             MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class)) {
            player.when(Rs2Player::getWorldLocation).thenReturn(REFUSED_AT);

            assertFalse(ProjectX.hopToWorld(308));

            combat.verify(Rs2Combat::inCombat, never());
        }
        assertTrue(ProjectX.cantHopWorld);
        verify(client, never()).hopToWorld(any());
    }

    @Test
    public void membersAreaRefusalDoesNotBlockMembersWorlds() {
        stubWorlds();
        ProjectX.markNonMembersHopRefused(REFUSED_AT);
        try (MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class)) {
            player.when(Rs2Player::getWorldLocation).thenReturn(REFUSED_AT);

            assertFalse(ProjectX.isNonMembersHopRefused(302));
            assertTrue(ProjectX.isNonMembersHopRefused(308));
        }
    }

    @Test
    public void membersAreaRefusalClearsOnceThePlayerHasMoved() {
        stubWorlds();
        ProjectX.markNonMembersHopRefused(REFUSED_AT);
        try (MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class)) {
            player.when(Rs2Player::getWorldLocation).thenReturn(REFUSED_AT.dx(1));

            assertFalse(ProjectX.isNonMembersHopRefused(308));
            assertFalse(ProjectX.cantHopWorld);

            player.when(Rs2Player::getWorldLocation).thenReturn(REFUSED_AT);
            assertFalse(ProjectX.isNonMembersHopRefused(308));
        }
    }

    @Test
    public void flagWithoutARefusalLocationDoesNotBlockHops() {
        stubWorlds();
        ProjectX.markNonMembersHopRefused(null);
        try (MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class)) {
            player.when(Rs2Player::getWorldLocation).thenReturn(REFUSED_AT);

            assertFalse(ProjectX.isNonMembersHopRefused(308));
            assertFalse(ProjectX.cantHopWorld);
        }
    }

    @Test
    public void staleRefusalDoesNotStopLaterHopAttempts() {
        stubWorlds();
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        when(client.getWorld()).thenReturn(301);
        ProjectX.markNonMembersHopRefused(REFUSED_AT);
        try (MockedStatic<Rs2Player> player = Mockito.mockStatic(Rs2Player.class);
             MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class)) {
            player.when(Rs2Player::getWorldLocation).thenReturn(new WorldPoint(3165, 3487, 0));
            combat.when(Rs2Combat::inCombat).thenReturn(false);
            bank.when(Rs2Bank::isOpen).thenReturn(true);

            assertFalse(ProjectX.hopToWorld(308));

            combat.verify(Rs2Combat::inCombat, atLeastOnce());
        }
        assertFalse(ProjectX.cantHopWorld);
    }

    @Test
    public void alreadyOnTheTargetWorldIsSuccessEvenWhenAHopWouldBeBlocked() {
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        when(client.getWorld()).thenReturn(302);
        try (MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class)) {
            combat.when(Rs2Combat::inCombat).thenReturn(true);
            bank.when(Rs2Bank::isOpen).thenReturn(true);

            assertTrue(ProjectX.hopToWorld(302));

            combat.verify(Rs2Combat::inCombat, never());
            bank.verify(Rs2Bank::isOpen, never());
        }
        verify(client, never()).hopToWorld(any());
    }

    @Test
    public void quickHopIsReleasedWhenIssuingTheHopThrows() {
        stubWorlds();
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        AtomicInteger world = new AtomicInteger(301);
        when(client.getWorld()).thenAnswer(inv -> world.get());
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.createWorld()).thenThrow(new IllegalStateException("createWorld failed")).thenReturn(target);
        switcherLoaded.set(true);
        doAnswer(inv -> {
            world.set(308);
            return null;
        }).when(client).hopToWorld(any());
        try (MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class);
             MockedStatic<Rs2Shop> shop = Mockito.mockStatic(Rs2Shop.class);
             MockedStatic<Rs2Dialogue> dialogue = Mockito.mockStatic(Rs2Dialogue.class)) {
            try {
                ProjectX.hopToWorld(308);
                fail("expected the createWorld failure to propagate");
            } catch (IllegalStateException expected) {
                // the client-thread stub runs the callable directly, so the failure escapes hopToWorld
            }
            assertFalse(ProjectX.isQuickHopInProgress());

            assertTrue(ProjectX.hopToWorld(308));
        }
        assertFalse(ProjectX.isQuickHopInProgress());
        verify(client).hopToWorld(target);
    }

    @Test
    public void missingWorldListRefusesTheHopWithoutCallingItInvalid() {
        Notifier notifier = mock(Notifier.class);
        projectx.when(ProjectX::getNotifier).thenReturn(notifier);
        WorldService worldService = mock(WorldService.class);
        projectx.when(ProjectX::getWorldService).thenReturn(worldService);
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        when(client.getWorld()).thenReturn(301);
        try (MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class);
             MockedStatic<Rs2Shop> shop = Mockito.mockStatic(Rs2Shop.class);
             MockedStatic<Rs2Dialogue> dialogue = Mockito.mockStatic(Rs2Dialogue.class)) {
            assertFalse(ProjectX.hopToWorld(308));
            projectx.when(ProjectX::getWorldService).thenReturn(null);
            assertFalse(ProjectX.hopToWorld(308));
        }
        verify(notifier, never()).notify(Mockito.anyString());
        verify(client, never()).createWorld();
    }

    @Test
    public void invalidWorldIsNotifiedOnce() {
        stubWorlds();
        Notifier notifier = mock(Notifier.class);
        projectx.when(ProjectX::getNotifier).thenReturn(notifier);
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        when(client.getWorld()).thenReturn(301);
        try (MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class);
             MockedStatic<Rs2Shop> shop = Mockito.mockStatic(Rs2Shop.class);
             MockedStatic<Rs2Dialogue> dialogue = Mockito.mockStatic(Rs2Dialogue.class)) {
            assertFalse(ProjectX.hopToWorld(9999));
            assertFalse(ProjectX.hopToWorld(9999));
            assertFalse(ProjectX.hopToWorld(9999));
        }
        verify(notifier, times(1)).notify(Mockito.anyString());
        verify(client, never()).createWorld();
    }

    @Test
    public void opensSwitcherThenHopsOnceListHasLoaded() {
        doAnswer(inv -> {
            openCall.set(clientThreadCalls.get());
            new Thread(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
                switcherLoaded.set(true);
            }).start();
            return null;
        }).when(client).openWorldHopper();

        assertTrue(ProjectX.hopViaWorldSwitcher(target, 3000));

        InOrder order = inOrder(client);
        order.verify(client, atLeastOnce()).openWorldHopper();
        order.verify(client).hopToWorld(target);
        assertNotEquals(openCall.get(), hopCall.get());
    }

    @Test
    public void hopsImmediatelyWhenSwitcherAlreadyOpen() {
        switcherLoaded.set(true);

        assertTrue(ProjectX.hopViaWorldSwitcher(target, 3000));

        verify(client, never()).openWorldHopper();
        verify(client).hopToWorld(target);
    }

    @Test
    public void blockedHopIsRefusedBeforePreparingTheHop() {
        projectx.when(ProjectX::isLoggedIn).thenReturn(true);
        when(client.getWorld()).thenReturn(301);
        try (MockedStatic<Rs2Combat> combat = Mockito.mockStatic(Rs2Combat.class);
             MockedStatic<Rs2Bank> bank = Mockito.mockStatic(Rs2Bank.class)) {
            combat.when(Rs2Combat::inCombat).thenReturn(false);
            bank.when(Rs2Bank::isOpen).thenReturn(true);

            assertFalse(ProjectX.hopToWorld(302));
        }

        projectx.verify(ProjectX::getWorldService, never());
        verify(client, never()).hopToWorld(any());
    }

    @Test
    public void doesNotHopWhenSwitcherNeverLoads() {
        assertFalse(ProjectX.hopViaWorldSwitcher(target, 300));

        verify(client, atLeastOnce()).openWorldHopper();
        verify(client, never()).hopToWorld(any());
        assertEquals(-1, hopCall.get());
    }
}
