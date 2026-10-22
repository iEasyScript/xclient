package net.runelite.client.plugins.projectx.util.coords;

import net.runelite.api.Perspective;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;

import static net.runelite.api.Constants.CHUNK_SIZE;

public class Rs2LocalPoint {
    /**
     * Converts a template (real-world) point inside an instance to its scene tile. One template
     * chunk can be placed several times in an instance (a house reuses its room templates), so the
     * same template tile can be in more than one place; the copy nearest the player wins, because
     * the first match could be a room away and off the minimap.
     *
     * <p>Only for template points. A point already in instance (scene) coordinates, such as any
     * queryable model's {@code getWorldLocation()}, is not found here; use
     * {@link SceneSpace#toLocal} when the point may be in either space.
     *
     * @param worldPoint template point
     * @return the scene tile, or null when no instance chunk holds that template tile
     */
    public static LocalPoint fromWorldInstance(WorldPoint worldPoint)
    {
        return ProjectX.getClientThread().runOnClientThreadOptional(() -> {
            WorldView wv = ProjectX.getClient().getTopLevelWorldView();
            int[][][] instanceTemplateChunks = wv.getInstanceTemplateChunks();
            int worldPlane = wv.getPlane();
            if (instanceTemplateChunks == null || worldPlane < 0 || worldPlane >= instanceTemplateChunks.length) {
                return null;
            }
            net.runelite.api.Player player = ProjectX.getClient().getLocalPlayer();
            LocalPoint playerLocal = player == null ? null : player.getLocalLocation();

            int tile = SceneSpace.templateToScene(instanceTemplateChunks[worldPlane],
                    worldPoint.getX(), worldPoint.getY(),
                    playerLocal == null ? -1 : playerLocal.getSceneX(),
                    playerLocal == null ? -1 : playerLocal.getSceneY());
            if (tile == SceneSpace.NONE) {
                return null;
            }
            return LocalPoint.fromScene(SceneSpace.sceneX(tile), SceneSpace.sceneY(tile), wv);
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
