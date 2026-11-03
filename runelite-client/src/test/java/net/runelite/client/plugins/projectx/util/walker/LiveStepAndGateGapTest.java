package net.runelite.client.plugins.projectx.util.walker;

import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Pure parts of the 2026-11-04 door-choice fixes: the live collision step check that keeps the
 * "recently opened" timer from hiding a closed gate, and the origin gap used to pick a transport's object.
 */
public class LiveStepAndGateGapTest {

    private static int[][] open(int size) {
        return new int[size][size];
    }

    @Test
    public void cardinalStepBlockedByWallFlagOnEitherSide() {
        int[][] flags = open(4);
        assertTrue(Rs2Walker.stepOpenInFlags(flags, 1, 1, 1, 0));

        // A closed gate on the west edge of (2,1) blocks stepping east from (1,1).
        flags[2][1] |= CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        assertFalse(Rs2Walker.stepOpenInFlags(flags, 1, 1, 1, 0));
        // The step the other way is blocked by the same edge, seen from the source side.
        flags = open(4);
        flags[1][1] |= CollisionDataFlag.BLOCK_MOVEMENT_EAST;
        assertFalse(Rs2Walker.stepOpenInFlags(flags, 1, 1, 1, 0));
    }

    @Test
    public void solidTargetBlocksTheStep() {
        int[][] flags = open(4);
        flags[1][2] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        assertFalse(Rs2Walker.stepOpenInFlags(flags, 1, 1, 0, 1));
    }

    @Test
    public void diagonalOpenWhenEitherCornerPathIsOpen() {
        int[][] flags = open(4);
        // Block the east-then-north corner; north-then-east is still open.
        flags[2][1] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        assertTrue(Rs2Walker.stepOpenInFlags(flags, 1, 1, 1, 1));
        // Block the other corner too.
        flags[1][2] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        assertFalse(Rs2Walker.stepOpenInFlags(flags, 1, 1, 1, 1));
    }

    @Test
    public void outsideTheFlagsGivesNoVerdict() {
        assertNull(Rs2Walker.stepOpenInFlags(open(2), 1, 1, 1, 0));
        assertNull(Rs2Walker.stepOpenInFlags(null, 0, 0, 1, 0));
    }

    @Test
    public void footprintGapMeasuresToTheNearestFootprintTile() {
        // One-tile object: plain Chebyshev distance.
        assertEquals(0, Rs2Walker.footprintGap(10, 10, 0, 0, 10, 10));
        assertEquals(10, Rs2Walker.footprintGap(3728, 9775, 0, 0, 3728, 9765));
        // A 3x3 object spans +-2 around its location in the superset; an origin two tiles
        // beyond that edge is gap 2.
        assertEquals(2, Rs2Walker.footprintGap(10, 10, 2, 2, 14, 10));
        assertEquals(0, Rs2Walker.footprintGap(10, 10, 2, 2, 12, 8));
    }
}
