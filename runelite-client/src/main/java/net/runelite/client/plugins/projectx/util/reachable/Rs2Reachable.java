package net.runelite.client.plugins.projectx.util.reachable;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.shortestpath.WorldPointUtil;

public class Rs2Reachable {
    private static int lastUpdateTick = 0;
    @Getter
    private static IntSet reachableTiles;

    private static final int REGION_SIZE = 104;

    public static boolean isReachable(WorldPoint target) {
        IntSet reachableTiles = getReachableTiles(target);
        int packedPoint = WorldPointUtil.packWorldPoint(
                target.getX(),
                target.getY(),
                target.getPlane()
        );
        return reachableTiles.contains(packedPoint);
    }

    public static IntSet getReachableTiles(WorldPoint start) {
        Client client = ProjectX.getClient();
        int tick = client.getTickCount();

        // Reuse cache for this tick
        if (reachableTiles != null && lastUpdateTick == tick) {
            return reachableTiles;
        }

        return ProjectX.getClientThread().invoke(() -> {
            Client c = ProjectX.getClient();
            WorldView worldView = c.getTopLevelWorldView();
            if (worldView == null) {
                reachableTiles = new IntOpenHashSet();
                lastUpdateTick = tick;
                return reachableTiles;
            }

            CollisionData[] collisionMaps = worldView.getCollisionMaps();
            if (collisionMaps == null) {
                reachableTiles = new IntOpenHashSet();
                lastUpdateTick = tick;
                return reachableTiles;
            }

            int plane = worldView.getPlane();
            int[][] collisionFlags = collisionMaps[plane].getFlags();

            boolean[][] visited = new boolean[REGION_SIZE][REGION_SIZE];
            IntArrayFIFOQueue openQueue = new IntArrayFIFOQueue();

            int worldBaseX = worldView.getBaseX();
            int worldBaseY = worldView.getBaseY();

            if (start == null || start.getPlane() != plane) {
                reachableTiles = new IntOpenHashSet();
                lastUpdateTick = tick;
                return reachableTiles;
            }

            int localStartX = start.getX() - worldBaseX;
            int localStartY = start.getY() - worldBaseY;

            if (localStartX < 0 || localStartY < 0
                    || localStartX >= REGION_SIZE || localStartY >= REGION_SIZE) {
                reachableTiles = new IntOpenHashSet();
                lastUpdateTick = tick;
                return reachableTiles;
            }

            int startKey = (localStartX << 16) | localStartY;
            openQueue.enqueue(startKey);
            visited[localStartX][localStartY] = true;

            while (!openQueue.isEmpty()) {
                int tileKey = openQueue.dequeueInt();
                int localX = tileKey >> 16;
                int localY = tileKey & 0xFFFF;

                int tileFlags = collisionFlags[localX][localY];

                // South
                int southY = localY - 1;
                if (southY >= 0
                        && (tileFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0
                        && (collisionFlags[localX][southY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                        && !visited[localX][southY]) {
                    openQueue.enqueue((localX << 16) | southY);
                    visited[localX][southY] = true;
                }

                // North
                int northY = localY + 1;
                if (northY < REGION_SIZE
                        && (tileFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0
                        && (collisionFlags[localX][northY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                        && !visited[localX][northY]) {
                    openQueue.enqueue((localX << 16) | northY);
                    visited[localX][northY] = true;
                }

                // West
                int westX = localX - 1;
                if (westX >= 0
                        && (tileFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0
                        && (collisionFlags[westX][localY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                        && !visited[westX][localY]) {
                    openQueue.enqueue((westX << 16) | localY);
                    visited[westX][localY] = true;
                }

                // East
                int eastX = localX + 1;
                if (eastX < REGION_SIZE
                        && (tileFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0
                        && (collisionFlags[eastX][localY] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0
                        && !visited[eastX][localY]) {
                    openQueue.enqueue((eastX << 16) | localY);
                    visited[eastX][localY] = true;
                }
            }

            IntSet reachablePackedPoints = new IntOpenHashSet();

            for (int x = 0; x < REGION_SIZE; x++) {
                for (int y = 0; y < REGION_SIZE; y++) {
                    if (visited[x][y]) {
                        int worldX = worldBaseX + x;
                        int worldY = worldBaseY + y;
                        reachablePackedPoints.add(
                                WorldPointUtil.packWorldPoint(worldX, worldY, plane)
                        );
                    }
                }
            }

            reachableTiles = reachablePackedPoints;
            lastUpdateTick = tick;
            return reachableTiles;
        });
    }
}
