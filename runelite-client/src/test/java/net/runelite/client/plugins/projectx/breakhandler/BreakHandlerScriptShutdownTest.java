package net.runelite.client.plugins.projectx.breakhandler;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.accountselector.AutoLoginScript;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.ContainableFrame;
import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BreakHandlerScriptShutdownTest
{
    private BreakHandlerScript script;

    @After
    public void reset() throws Exception
    {
        setState(BreakHandlerState.WAITING_FOR_BREAK);
        BreakHandlerScript.isBanned = false;
        BreakHandlerScript.breakDuration = -1;
        ProjectX.pauseAllScripts.set(false);
        if (script != null)
        {
            Field executor = Script.class.getDeclaredField("scheduledExecutorService");
            executor.setAccessible(true);
            ((ScheduledExecutorService) executor.get(script)).shutdownNow();
        }
    }

    @Test
    public void shutdownDuringLoggedOutBreakReleasesLoginOwnership() throws Exception
    {
        assertShutdownClearsBreak(BreakHandlerState.LOGGED_OUT);
    }

    @Test
    public void shutdownDuringInGameBreakReleasesLoginOwnership() throws Exception
    {
        assertShutdownClearsBreak(BreakHandlerState.INGAME_BREAK_ACTIVE);
    }

    @Test
    public void restartClearsAnEarlierBanDetection() throws Exception
    {
        BreakHandlerScript.isBanned = true;
        BreakHandlerConfig config = mock(BreakHandlerConfig.class);
        ContainableFrame frame = mock(ContainableFrame.class);
        when(frame.getTitle()).thenReturn("ProjectX");
        try (MockedStatic<ClientUI> clientUi = Mockito.mockStatic(ClientUI.class))
        {
            clientUi.when(ClientUI::getFrame).thenReturn(frame);
            script = new BreakHandlerScript();
            script.run(config);
        }

        assertFalse(BreakHandlerScript.isBanned);
    }

    private void assertShutdownClearsBreak(BreakHandlerState state) throws Exception
    {
        setState(state);
        BreakHandlerScript.breakDuration = 300;
        ProjectX.pauseAllScripts.set(true);

        new BreakHandlerScript().shutdown();

        assertEquals(BreakHandlerState.WAITING_FOR_BREAK, BreakHandlerScript.getCurrentState());
        assertFalse(BreakHandlerScript.isBreakActive());
        assertFalse(isBreakHandlerOwningLogin());
        assertEquals(-1, BreakHandlerScript.breakDuration);
        assertFalse(ProjectX.pauseAllScripts.get());
    }

    private static boolean isBreakHandlerOwningLogin() throws Exception
    {
        Method method = AutoLoginScript.class.getDeclaredMethod("isBreakHandlerOwningLogin");
        method.setAccessible(true);
        return (boolean) method.invoke(null);
    }

    @SuppressWarnings("unchecked")
    private static void setState(BreakHandlerState state) throws Exception
    {
        Field field = BreakHandlerScript.class.getDeclaredField("currentState");
        field.setAccessible(true);
        ((AtomicReference<BreakHandlerState>) field.get(null)).set(state);
    }
}
