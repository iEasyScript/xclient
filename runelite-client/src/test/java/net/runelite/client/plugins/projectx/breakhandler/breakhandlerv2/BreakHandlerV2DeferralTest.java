package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.breakhandler.BreakPreparation;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class BreakHandlerV2DeferralTest {
    private BreakHandlerV2Script script;

    @After
    public void reset() throws Exception {
        BreakPreparation.finishBreak();
        ProjectX.pauseAllScripts.set(false);
        BreakHandlerV2State.setState(BreakHandlerV2State.WAITING_FOR_BREAK);
        if (script != null) {
            Field executor = Script.class.getDeclaredField("scheduledExecutorService");
            executor.setAccessible(true);
            ((ScheduledExecutorService) executor.get(script)).shutdownNow();
        }
    }

    @Test
    public void defersRequestedBreakUntilParticipantIsReady() {
        try (BreakPreparation.Handle handle = BreakPreparation.register("test")) {
            assertTrue(BreakHandlerV2Script.shouldDeferRequestedBreak(null));
            assertTrue(handle.isRequested());
            handle.ready();
            assertFalse(BreakHandlerV2Script.shouldDeferRequestedBreak(null));
        }
    }

    @Test
    public void doesNotDeferWithoutParticipants() {
        assertFalse(BreakHandlerV2Script.shouldDeferRequestedBreak(null));
    }

    @Test
    public void neverDefersAnActiveBreak() {
        try (BreakPreparation.Handle ignored = BreakPreparation.register("test")) {
            assertFalse(BreakHandlerV2Script.shouldDeferRequestedBreak(Instant.now().plusSeconds(60)));
        }
    }

    @Test
    public void activePauseBreakIgnoresLateParticipantTimeoutAndStillEnds() throws Exception {
        script = new BreakHandlerV2Script();
        Instant deadline = Instant.now().plusSeconds(600);
        setField("breakEndTime", deadline);
        BreakHandlerV2State.setState(BreakHandlerV2State.BREAK_REQUESTED);
        ProjectX.pauseAllScripts.set(true);
        BreakPreparation.shouldDeferBreak();
        try (BreakPreparation.Handle late = BreakPreparation.register("late-plugin")) {
            Field requestedAt = BreakPreparation.class.getDeclaredField("requestedAt");
            requestedAt.setAccessible(true);
            requestedAt.setLong(null, System.currentTimeMillis() - 120_001);
            assertTrue(BreakPreparation.isAborted());

            invokeBreakRequested();

            assertEquals(deadline, getField("breakEndTime"));
            assertEquals(BreakHandlerV2State.BREAK_REQUESTED, BreakHandlerV2State.getCurrentState());
            assertTrue(ProjectX.pauseAllScripts.get());

            setField("breakEndTime", Instant.now().minusSeconds(1));
            invokeBreakRequested();
            assertEquals(BreakHandlerV2State.BREAK_ENDING, BreakHandlerV2State.getCurrentState());
            assertFalse(ProjectX.pauseAllScripts.get());
        }
    }

    @Test
    public void failedNewPreparationClearsPauseAndReschedules() throws Exception {
        assertFailedPreparationResumes(false);
    }

    @Test
    public void failedRestoredPausePreparationClearsPauseAndReschedules() throws Exception {
        assertFailedPreparationResumes(true);
    }

    private void assertFailedPreparationResumes(boolean restored) throws Exception {
        script = new BreakHandlerV2Script();
        setField("config", mock(BreakHandlerV2Config.class));
        setField("restoredPauseNeedsPreparation", restored);
        if (restored) {
            setField("breakEndTime", Instant.now().plusSeconds(600));
        }
        BreakHandlerV2State.setState(BreakHandlerV2State.BREAK_REQUESTED);
        ProjectX.pauseAllScripts.set(true);
        BreakPreparation.Handle handle = BreakPreparation.register("failed-plugin");
        BreakPreparation.shouldDeferBreak();
        handle.close();
        assertTrue(BreakPreparation.isAborted());

        invokeBreakRequested();

        assertFalse(ProjectX.pauseAllScripts.get());
        assertFalse(BreakPreparation.isAborted());
        assertEquals(BreakHandlerV2State.WAITING_FOR_BREAK, BreakHandlerV2State.getCurrentState());
        assertNull(getField("breakEndTime"));
        assertEquals(false, getField("restoredPauseNeedsPreparation"));
        assertNotNull(getField("nextBreakTime"));
    }

    private void invokeBreakRequested() throws Exception {
        Method method = BreakHandlerV2Script.class.getDeclaredMethod("handleBreakRequested");
        method.setAccessible(true);
        method.invoke(script);
    }

    private void setField(String name, Object value) throws Exception {
        Field field = BreakHandlerV2Script.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(script, value);
    }

    private Object getField(String name) throws Exception {
        Field field = BreakHandlerV2Script.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(script);
    }
}
