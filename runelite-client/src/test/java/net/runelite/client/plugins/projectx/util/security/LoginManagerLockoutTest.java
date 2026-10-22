package net.runelite.client.plugins.projectx.util.security;

import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.projectx.util.mouse.Mouse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class LoginManagerLockoutTest {
    private final AtomicInteger loginIndex = new AtomicInteger(LoginLockout.LOGIN_INDEX_TOO_MANY_ATTEMPTS);
    private Client client;
    private Mouse mouse;

    @Before public void setUp() {
        LoginLockout.getInstance().recordSuccessfulLogin();
        client = mock(Client.class);
        mouse = mock(Mouse.class);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        when(client.getLoginIndex()).thenAnswer(call -> loginIndex.get());
        when(client.getCanvasWidth()).thenReturn(804);
    }

    @After public void tearDown() {
        LoginLockout.getInstance().recordSuccessfulLogin();
    }

    @Test public void lockoutScreenPressesBackAndNeverSubmits() {
        try (var projectx = mockStatic(ProjectX.class);
             var keyboard = mockStatic(Rs2Keyboard.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::getMouse).thenReturn(mouse);

            assertFalse(LoginManager.login("user", "", 383));

            verify(mouse, times(1)).click(365, 326);
            verify(client, never()).setUsername(anyString());
            verify(client, never()).setPassword(anyString());
            verify(client, never()).changeWorld(any());
            keyboard.verifyNoInteractions();
            assertTrue(LoginLockout.getInstance().isActive());
        }
    }

    @Test public void cooldownKeepsRefusingAfterTheScreenIsDismissed() {
        try (var projectx = mockStatic(ProjectX.class);
             var keyboard = mockStatic(Rs2Keyboard.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::getMouse).thenReturn(mouse);

            assertFalse(LoginManager.login("user", "", 383));
            loginIndex.set(2);
            assertFalse(LoginManager.isLoginLockoutScreen());
            assertTrue(LoginManager.checkLoginLockout());
            assertFalse(LoginManager.login("user", "", 383));

            verify(mouse, times(1)).click(anyInt(), anyInt());
            verify(client, never()).setUsername(anyString());
            keyboard.verifyNoInteractions();
        }
    }

    @Test public void repeatedPollingOnLockoutScreenDoesNotSpamClicks() {
        try (var projectx = mockStatic(ProjectX.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::getMouse).thenReturn(mouse);

            for (int i = 0; i < 5; i++) {
                assertTrue(LoginManager.checkLoginLockout());
            }

            verify(mouse, times(1)).click(anyInt(), anyInt());
            assertEquals(1, LoginLockout.getInstance().getConsecutiveLockouts());
        }
    }

    @Test public void normalLoginScreenIsNotTreatedAsLockout() {
        loginIndex.set(2);
        try (var projectx = mockStatic(ProjectX.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::getMouse).thenReturn(mouse);

            assertFalse(LoginManager.isLoginLockoutScreen());
            assertFalse(LoginManager.checkLoginLockout());
            verifyNoInteractions(mouse);
        }
    }

    @Test public void lockoutIndexOutsideLoginScreenIsIgnored() {
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        try (var projectx = mockStatic(ProjectX.class)) {
            projectx.when(ProjectX::getClient).thenReturn(client);
            projectx.when(ProjectX::getMouse).thenReturn(mouse);

            assertFalse(LoginManager.checkLoginLockout());
            verifyNoInteractions(mouse);
        }
    }
}
