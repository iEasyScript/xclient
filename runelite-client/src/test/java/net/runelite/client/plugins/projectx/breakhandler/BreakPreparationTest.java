package net.runelite.client.plugins.projectx.breakhandler;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class BreakPreparationTest
{
    @After
    public void reset()
    {
        BreakPreparation.finishBreak();
    }

    @Test
    public void allOwnersMustPrepareAndNextBreakRequiresPreparationAgain()
    {
        try (BreakPreparation.Handle a = BreakPreparation.register("a");
             BreakPreparation.Handle b = BreakPreparation.register("b"))
        {
            assertFalse(a.isRequested());
            assertTrue(BreakPreparation.shouldDeferBreak());
            assertTrue(a.isRequested());
            a.ready();
            assertTrue(BreakPreparation.shouldDeferBreak());
            b.ready();
            assertFalse(BreakPreparation.shouldDeferBreak());
            BreakPreparation.finishBreak();
            assertFalse(a.isRequested());
            assertTrue(BreakPreparation.shouldDeferBreak());
        }
    }

    @Test
    public void oldHandleCannotReleaseItsReplacement()
    {
        BreakPreparation.Handle old = BreakPreparation.register("a");
        try (BreakPreparation.Handle replacement = BreakPreparation.register("a"))
        {
            BreakPreparation.shouldDeferBreak();
            old.close();
            old.ready();
            assertFalse(BreakPreparation.isAborted());
            assertTrue(BreakPreparation.shouldDeferBreak());
            replacement.ready();
            assertFalse(BreakPreparation.shouldDeferBreak());
        }
        assertFalse(BreakPreparation.hasParticipants());
    }

    @Test
    public void failedPreparationCancelsInsteadOfClaimingSafety()
    {
        BreakPreparation.Handle handle = BreakPreparation.register("a");
        BreakPreparation.shouldDeferBreak();
        handle.close();
        assertTrue(BreakPreparation.isAborted());
        BreakPreparation.finishBreak();
        assertFalse(BreakPreparation.isAborted());
    }

    @Test
    public void cancelledRequestCanBeRequestedAgainWithoutReplacingOwner()
    {
        try (BreakPreparation.Handle handle = BreakPreparation.register("blackjack"))
        {
            assertTrue(BreakPreparation.shouldDeferBreak());
            BreakPreparation.finishBreak();
            assertFalse(handle.isRequested());
            assertTrue(BreakPreparation.shouldDeferBreak());
            handle.ready();
            assertFalse(BreakPreparation.shouldDeferBreak());
        }
    }

    @Test
    public void noParticipantsDoesNotDeferExistingBreakBehavior()
    {
        assertFalse(BreakPreparation.hasParticipants());
        assertFalse(BreakPreparation.shouldDeferBreak());
        assertFalse(BreakPreparation.isAborted());
    }

    @Test
    public void unreadyParticipantTimesOutRatherThanAllowingUnsafeBreak() throws Exception
    {
        try (BreakPreparation.Handle handle = BreakPreparation.register("blackjack"))
        {
            assertTrue(BreakPreparation.shouldDeferBreak());
            java.lang.reflect.Field requestedAt = BreakPreparation.class.getDeclaredField("requestedAt");
            requestedAt.setAccessible(true);
            requestedAt.setLong(null, System.currentTimeMillis() - 120_001);
            assertTrue(BreakPreparation.isAborted());
            assertTrue(BreakPreparation.shouldDeferBreak());
        }
    }
}
