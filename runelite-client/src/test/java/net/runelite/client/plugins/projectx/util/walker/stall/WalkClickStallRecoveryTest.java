package net.runelite.client.plugins.projectx.util.walker.stall;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WalkClickStallRecoveryTest {

    @Test
    public void watchesGracePlusStillnessAfterTheClick() {
        assertEquals(3_000L, WalkClickStallRecovery.remainingWatchMs(0L));
        assertEquals(600L, WalkClickStallRecovery.remainingWatchMs(2_400L));
        assertEquals(0L, WalkClickStallRecovery.remainingWatchMs(5_000L));
    }

    @Test
    public void backoffGrowsWithTheAttemptAndIsCapped() {
        assertEquals(600L, WalkClickStallRecovery.backoffMs(1, 600));
        assertEquals(1_200L, WalkClickStallRecovery.backoffMs(2, 600));
        assertEquals(2_400L, WalkClickStallRecovery.backoffMs(4, 600));
        assertEquals(2_400L, WalkClickStallRecovery.backoffMs(9, 600));
        assertEquals(600L, WalkClickStallRecovery.backoffMs(0, 600));
    }
}
