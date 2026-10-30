package net.runelite.client.plugins.projectx.util.walker;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A POH teleport listed in the Web Walker's POH panel but missing from the house made the walker
 * reopen the nexus for it on every pass (POH report, 2026-10). It is now left out of planning after
 * two failures in a row; one failure alone may be a slow menu.
 */
public class PohTeleportLockTest {

    @Test
    public void locksOnTheSecondFailureInARow() {
        Map<String, Integer> failures = new HashMap<>();
        assertFalse(Rs2Walker.recordPohTeleportFailure(failures, "Nexus: Kourend Castle"));
        assertTrue(Rs2Walker.recordPohTeleportFailure(failures, "Nexus: Kourend Castle"));
        assertTrue("the count resets once the teleport is locked", failures.isEmpty());
    }

    @Test
    public void countsEachTeleportSeparately() {
        Map<String, Integer> failures = new HashMap<>();
        assertFalse(Rs2Walker.recordPohTeleportFailure(failures, "Nexus: Kourend Castle"));
        assertFalse(Rs2Walker.recordPohTeleportFailure(failures, "Nexus: Annakarl"));
    }

    @Test
    public void ignoresATeleportWithoutAName() {
        assertFalse(Rs2Walker.recordPohTeleportFailure(new HashMap<>(), null));
    }
}
