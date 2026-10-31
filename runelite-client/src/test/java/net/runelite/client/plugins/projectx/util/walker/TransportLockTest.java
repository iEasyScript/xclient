package net.runelite.client.plugins.projectx.util.walker;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A staircase that never climbed (Wizards' Guild, Yanille: report 2026-10-31) was clicked for minutes
 * because a failed transport was simply retried. It is now locked after three failures in a row.
 */
public class TransportLockTest {

    @Test
    public void locksOnTheThirdFailureInARow() {
        Map<String, Integer> failures = new HashMap<>();
        String stairs = "WorldPoint(x=2590, y=3083, plane=1)|WorldPoint(x=2590, y=3087, plane=2)|15645";
        assertFalse(Rs2Walker.recordTransportFailure(failures, stairs));
        assertFalse(Rs2Walker.recordTransportFailure(failures, stairs));
        assertTrue(Rs2Walker.recordTransportFailure(failures, stairs));
        assertTrue("the count resets once the transport is locked", failures.isEmpty());
    }

    @Test
    public void countsEachTransportSeparately() {
        Map<String, Integer> failures = new HashMap<>();
        assertFalse(Rs2Walker.recordTransportFailure(failures, "a"));
        assertFalse(Rs2Walker.recordTransportFailure(failures, "b"));
        assertFalse(Rs2Walker.recordTransportFailure(failures, "a"));
        assertFalse(Rs2Walker.recordTransportFailure(failures, "b"));
    }

    @Test
    public void ignoresATransportWithoutAKey() {
        assertFalse(Rs2Walker.recordTransportFailure(new HashMap<>(), null));
    }
}
