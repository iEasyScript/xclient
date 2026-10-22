package net.runelite.client.plugins.projectx.util.tile;

import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Scene-space rule behind {@link Rs2Tile#isGameObjectReachable}: a tile ringing the object's
 * footprint must be walkable and reachable. Being scene maths, it gives the same answer whatever
 * the world coordinates of the scene are, which is the point: inside an instance the old
 * world-point version answered "no" for an object beside the player.
 */
public class Rs2GameObjectFootprintReachableTest {

    private static final int SIZE = 104;

    private static int[][] openField() {
        return new int[SIZE][SIZE];
    }

    private static void solid(int[][] flags, int minX, int minY, int maxX, int maxY) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                flags[x][y] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            }
        }
    }

    private static boolean reach(int[][] flags, int playerX, int playerY, int minX, int minY, int maxX, int maxY) {
        boolean[][] reachable = Rs2Tile.reachableFrom(flags, playerX, playerY);
        return Rs2Tile.canReachFootprint(flags, reachable, minX, minY, maxX, maxY, null);
    }

    @Test
    public void solidObjectBesideThePlayerIsReachable() {
        // Obor's climbing rocks: a solid 1x1 object on the tile next to the player.
        int[][] flags = openField();
        solid(flags, 51, 50, 51, 50);
        assertTrue(reach(flags, 50, 50, 51, 50, 51, 50));
    }

    @Test
    public void largeObjectIsReachableFromAnySideTile() {
        int[][] flags = openField();
        solid(flags, 60, 60, 62, 62);
        assertTrue(reach(flags, 40, 40, 60, 60, 62, 62));
    }

    @Test
    public void objectInsideAWalledPenIsNotReachable() {
        int[][] flags = openField();
        solid(flags, 70, 70, 70, 70);
        // A solid ring two tiles out seals off the object's own ring.
        for (int i = 67; i <= 73; i++) {
            flags[i][67] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            flags[i][73] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            flags[67][i] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            flags[73][i] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        }
        assertFalse(reach(flags, 40, 40, 70, 70, 70, 70));
        assertTrue("from inside the pen", reach(flags, 69, 69, 70, 70, 70, 70));
    }

    @Test
    public void ringTilesThatAreSolidDoNotCount() {
        int[][] flags = openField();
        // Object surrounded by solid tiles on every side: nothing to stand on.
        solid(flags, 29, 29, 31, 31);
        assertFalse(reach(flags, 10, 10, 30, 30, 30, 30));
    }

    @Test
    public void collisionIgnoredRingTileCountsEvenWhenNotReachable() {
        int[][] flags = openField();
        solid(flags, 70, 70, 70, 70);
        for (int i = 67; i <= 73; i++) {
            flags[i][67] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            flags[i][73] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            flags[67][i] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            flags[73][i] |= CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        }
        boolean[][] reachable = Rs2Tile.reachableFrom(flags, 40, 40);
        assertTrue(Rs2Tile.canReachFootprint(flags, reachable, 70, 70, 70, 70, (x, y) -> x == 69 && y == 70));
    }

    @Test
    public void objectOnTheSceneEdgeDoesNotIndexOutOfBounds() {
        int[][] flags = openField();
        solid(flags, 0, 0, 0, 0);
        assertTrue(reach(flags, 2, 2, 0, 0, 0, 0));
        solid(flags, 103, 103, 103, 103);
        assertTrue(reach(flags, 100, 100, 103, 103, 103, 103));
    }
}
