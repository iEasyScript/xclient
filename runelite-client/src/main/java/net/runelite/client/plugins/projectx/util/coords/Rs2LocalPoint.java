package net.runelite.client.plugins.projectx.util.coords;

import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;

import static net.runelite.api.Constants.CHUNK_SIZE;

public class Rs2LocalPoint {
    /**
     * Used to convert a worldpoint in an instance to a localpoint
     * @param worldPoint
     * @return
     */
    public static LocalPoint fromWorldInstance(WorldPoint worldPoint)
    {
        return ProjectX.getClientThread().runOnClientThreadOptional(() -> {
            int[][][] instanceTemplateChunks = ProjectX.getClient().getTopLevelWorldView().getInstanceTemplateChunks();
            int worldX = worldPoint.getX();
            int worldY = worldPoint.getY();
            int worldPlane = ProjectX.getClient().getTopLevelWorldView().getPlane();
            net.runelite.api.Player player = ProjectX.getClient().getLocalPlayer();
            LocalPoint playerLocal = player == null ? null : player.getLocalLocation();
            int[] playerScene = playerLocal == null ? null : new int[]{playerLocal.getSceneX(), playerLocal.getSceneY()};
            LocalPoint best = null;
            int bestDistance = Integer.MAX_VALUE;

            for (int chunkX = 0; chunkX < instanceTemplateChunks[worldPlane].length; chunkX++)
            {
                for (int chunkY = 0; chunkY < instanceTemplateChunks[worldPlane][chunkX].length; chunkY++)
                {
                    int templateChunk = instanceTemplateChunks[worldPlane][chunkX][chunkY];

                    int rotation = (templateChunk >> 1) & 0x3;
                    int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
                    int templateChunkPlane = (templateChunk >> 24) & 0x3;

                    WorldPoint rotatedWorldPoint = rotate(new WorldPoint(worldX, worldY, templateChunkPlane), rotation);

                    if (rotatedWorldPoint.getX() >= templateChunkX && rotatedWorldPoint.getX() < templateChunkX + CHUNK_SIZE
                            && rotatedWorldPoint.getY() >= templateChunkY && rotatedWorldPoint.getY() < templateChunkY + CHUNK_SIZE)
                    {
                        int localX = (rotatedWorldPoint.getX() - templateChunkX) + (chunkX * CHUNK_SIZE);
                        int localY = (rotatedWorldPoint.getY() - templateChunkY) + (chunkY * CHUNK_SIZE);

                        // One template chunk can be placed several times in an instance (a house
                        // reuses its room templates), so the same template tile can be in more than
                        // one place. The first match could be a room away and off the minimap; the
                        // walker means the copy beside the player.
                        int distance = playerScene == null ? 0
                                : Math.max(Math.abs(localX - playerScene[0]), Math.abs(localY - playerScene[1]));
                        if (best == null || distance < bestDistance) {
                            best = LocalPoint.fromScene(localX, localY, ProjectX.getClient().getTopLevelWorldView());
                            bestDistance = distance;
                        }
                    }
                }
            }

            return best;
        }).orElse(null);
    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point point
     * @param rotation rotation
     * @return world point
     */
    public static WorldPoint rotate(WorldPoint point, int rotation)
    {
        int chunkX = point.getX() & ~(CHUNK_SIZE - 1);
        int chunkY = point.getY() & ~(CHUNK_SIZE - 1);
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }

    public static Integer worldToLocalDistance(int distance) {
        return distance * Perspective.LOCAL_TILE_SIZE;
    }

    public static Integer localToWorldDistance(int distance) {
        return distance / Perspective.LOCAL_TILE_SIZE;
    }
}
