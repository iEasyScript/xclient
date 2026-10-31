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
    public void aPlayerLeftOnTheOriginCountsAsARefusal() {
        net.runelite.api.coords.WorldPoint origin = new net.runelite.api.coords.WorldPoint(3200, 3200, 0);
        net.runelite.api.coords.WorldPoint dest = new net.runelite.api.coords.WorldPoint(3210, 3200, 0);
        assertTrue(Rs2Walker.isStillAtTransportOrigin(origin, origin, dest));
        assertTrue(Rs2Walker.isStillAtTransportOrigin(new net.runelite.api.coords.WorldPoint(3199, 3200, 0), origin, dest));
        assertFalse("two tiles away is not on the origin",
                Rs2Walker.isStillAtTransportOrigin(new net.runelite.api.coords.WorldPoint(3202, 3200, 0), origin, dest));
        assertFalse("landed near the destination",
                Rs2Walker.isStillAtTransportOrigin(dest, origin, dest));
        assertFalse(Rs2Walker.isStillAtTransportOrigin(null, origin, dest));
        assertFalse("another plane is not the origin",
                Rs2Walker.isStillAtTransportOrigin(new net.runelite.api.coords.WorldPoint(3200, 3200, 1), origin, dest));
    }

    @Test
    public void aDoorPlayerOnTheFarSideIsNotRefused() {
        net.runelite.api.coords.WorldPoint origin = new net.runelite.api.coords.WorldPoint(3123, 3360, 0);
        net.runelite.api.coords.WorldPoint dest = new net.runelite.api.coords.WorldPoint(3123, 3361, 0);
        assertTrue(Rs2Walker.isStillAtTransportOrigin(origin, origin, dest));
        assertFalse(Rs2Walker.isStillAtTransportOrigin(dest, origin, dest));
    }

    @Test
    public void ignoresATransportWithoutAKey() {
        assertFalse(Rs2Walker.recordTransportFailure(new HashMap<>(), null));
    }
}
