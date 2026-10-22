package net.runelite.client.plugins.projectx.util.tile;

import net.runelite.api.CollisionDataFlag;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Rs2TileGameObjectInteractableTest {

    private static final int SIZE = 104;

    private static int[][] openField() {
        return new int[SIZE][SIZE];
    }

    private static void wallBetweenColumns(int[][] flags, int westX) {
        for (int y = 0; y < SIZE; y++) {
            flags[westX][y] |= CollisionDataFlag.BLOCK_MOVEMENT_EAST;
            flags[westX + 1][y] |= CollisionDataFlag.BLOCK_MOVEMENT_WEST;
        }
    }

    private static void solid(int[][] flags, int minX, int minY, int maxX, int maxY, int flag) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                flags[x][y] |= flag;
            }
        }
    }

    private static boolean interactable(int[][] flags, int playerX, int playerY, int minX, int minY, int maxX, int maxY) {
        boolean[][] reachable = Rs2Tile.reachableFrom(flags, playerX, playerY);
        return Rs2Tile.canInteractWithFootprintFromReachableTile(flags, reachable, minX, minY, maxX, maxY);
    }

    @Test
    public void objectAgainstAWallIsNotInteractableFromOutside() {
        int[][] flags = openField();
        wallBetweenColumns(flags, 48);
        solid(flags, 48, 50, 48, 50, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);

        assertTrue("the tile outside the wall is reachable and in the 1-tile ring",
                Rs2Tile.reachableFrom(flags, 60, 50)[49][50]);
        assertFalse(interactable(flags, 60, 50, 48, 50, 48, 50));
    }

    @Test
    public void objectAgainstAWallIsInteractableFromInside() {
        int[][] flags = openField();
        wallBetweenColumns(flags, 48);
        solid(flags, 48, 50, 48, 50, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);

        assertTrue(interactable(flags, 40, 50, 48, 50, 48, 50));
    }

    @Test
    public void multiTileObjectAgainstAWallIsNotInteractableFromOutside() {
        int[][] flags = openField();
        wallBetweenColumns(flags, 48);
        solid(flags, 47, 50, 48, 51, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);

        assertFalse(interactable(flags, 60, 50, 47, 50, 48, 51));
        assertTrue(interactable(flags, 40, 50, 47, 50, 48, 51));
    }

    @Test
    public void solidObjectInTheOpenIsInteractable() {
        int[][] flags = openField();
        solid(flags, 50, 50, 50, 50, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);

        assertTrue(interactable(flags, 45, 45, 50, 50, 50, 50));
    }

    @Test
    public void diagonalNeighbourAloneDoesNotMakeAnObjectInteractable() {
        int[][] flags = openField();
        solid(flags, 50, 50, 50, 50, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);
        solid(flags, 49, 50, 49, 50, CollisionDataFlag.BLOCK_MOVEMENT_FULL);
        solid(flags, 51, 50, 51, 50, CollisionDataFlag.BLOCK_MOVEMENT_FULL);
        solid(flags, 50, 49, 50, 49, CollisionDataFlag.BLOCK_MOVEMENT_FULL);
        solid(flags, 50, 51, 50, 51, CollisionDataFlag.BLOCK_MOVEMENT_FULL);

        assertTrue("the diagonal tile is reachable", Rs2Tile.reachableFrom(flags, 45, 45)[49][49]);
        assertFalse(interactable(flags, 45, 45, 50, 50, 50, 50));
    }

    @Test
    public void boatOnWaterIsInteractableFromItsOnlyLandNeighbour() {
        int[][] flags = openField();
        solid(flags, 40, 40, 70, 53, CollisionDataFlag.BLOCK_MOVEMENT_FLOOR);
        solid(flags, 54, 51, 58, 53, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);

        assertFalse("the boat's own tile is never reachable", Rs2Tile.reachableFrom(flags, 56, 55)[56][52]);
        assertTrue(interactable(flags, 56, 55, 54, 51, 58, 53));
    }

    @Test
    public void boatIsNotInteractableWhenItsLandNeighbourIsFencedOff() {
        int[][] flags = openField();
        solid(flags, 40, 40, 70, 53, CollisionDataFlag.BLOCK_MOVEMENT_FLOOR);
        solid(flags, 54, 51, 58, 53, CollisionDataFlag.BLOCK_MOVEMENT_OBJECT);
        for (int x = 0; x < SIZE; x++) {
            flags[x][54] |= CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
            flags[x][55] |= CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;
        }

        assertFalse(interactable(flags, 56, 56, 54, 51, 58, 53));
    }

    @Test
    public void footprintOutsideTheSceneIsNotInteractable() {
        assertFalse(interactable(openField(), 10, 10, SIZE, 50, SIZE + 1, 51));
    }
}
