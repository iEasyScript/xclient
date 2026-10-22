package net.runelite.client.plugins.projectx.util.coords;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;

import static net.runelite.api.Constants.CHUNK_SIZE;

/**
 * Which coordinate space a {@link WorldPoint} is in, and the scene tile it names.
 *
 * <p>Inside an instance the same tile has two different {@link WorldPoint}s:
 * <ul>
 *     <li><b>scene (instance) coordinates</b>: what the game hands out. {@code Actor.getWorldLocation()},
 *     {@code TileObject.getWorldLocation()}, {@code TileItem}/{@code Tile.getWorldLocation()},
 *     {@code WorldPoint.fromScene}/{@code fromLocal}, and so the {@code getWorldLocation()} of every
 *     queryable model ({@code Rs2PlayerModel}, {@code Rs2NpcModel}, {@code Rs2TileObjectModel},
 *     {@code Rs2TileItemModel}). Here {@code x - baseX} is the scene x.</li>
 *     <li><b>template (real-world) coordinates</b>: {@code WorldPoint.fromLocalInstance}, and so
 *     {@code Rs2Player.getWorldLocation()}, and the tiles scripts hard-code from the wiki.</li>
 * </ul>
 * Outside an instance the two are the same point. Code that compares a point from one list with a
 * point from the other gets nonsense inside an instance: distances of thousands of tiles, or
 * "not reachable" for an object beside the player. Resolve both to a scene tile first.
 *
 * <p>The {@code int} methods are pure maths (testable without a client). A scene tile is packed
 * as {@code sceneX << 16 | sceneY}, or {@link #NONE}. The {@link WorldView} methods read game
 * state and must run on the client thread.
 */
public final class SceneSpace {
    public static final int SCENE_SIZE = 104;
    public static final int NONE = -1;

    private SceneSpace() {
    }

    public static int pack(int sceneX, int sceneY) {
        return (sceneX << 16) | sceneY;
    }

    public static int sceneX(int packed) {
        return packed >>> 16;
    }

    public static int sceneY(int packed) {
        return packed & 0xFFFF;
    }

    /**
     * The scene tile of a point given in scene coordinates, or {@link #NONE} when the point is not
     * on this scene (another plane, or outside the 104x104 area). Outside an instance every in-scene
     * world point passes; inside one, only instance-space points do.
     */
    public static int sceneTile(int x, int y, int plane, int baseX, int baseY, int scenePlane) {
        return sceneTile(x, y, plane, baseX, baseY, scenePlane, SCENE_SIZE, SCENE_SIZE);
    }

    /** {@link #sceneTile(int, int, int, int, int, int)} for a scene of another size (a boat's). */
    public static int sceneTile(int x, int y, int plane, int baseX, int baseY, int scenePlane, int sizeX, int sizeY) {
        if (plane != scenePlane) {
            return NONE;
        }
        int sx = x - baseX;
        int sy = y - baseY;
        if (sx < 0 || sy < 0 || sx >= sizeX || sy >= sizeY) {
            return NONE;
        }
        return pack(sx, sy);
    }

    /**
     * The scene tile of a template (real-world) point in an instance, or {@link #NONE}.
     * One template chunk can be placed several times (a house reuses its room templates); the copy
     * nearest the player wins, the first one when the player's tile is unknown (negative).
     *
     * @param planeChunks {@code WorldView.getInstanceTemplateChunks()[plane]}
     */
    public static int templateToScene(int[][] planeChunks, int x, int y, int playerSceneX, int playerSceneY) {
        if (planeChunks == null) {
            return NONE;
        }
        boolean knowPlayer = playerSceneX >= 0 && playerSceneY >= 0;
        int best = NONE;
        int bestDistance = Integer.MAX_VALUE;

        for (int chunkX = 0; chunkX < planeChunks.length; chunkX++) {
            int[] column = planeChunks[chunkX];
            if (column == null) continue;
            for (int chunkY = 0; chunkY < column.length; chunkY++) {
                int templateChunk = column[chunkY];
                if (templateChunk == -1) continue;

                int rotation = (templateChunk >> 1) & 0x3;
                int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
                int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;

                int[] rotated = rotate(x, y, rotation);
                if (rotated[0] < templateChunkX || rotated[0] >= templateChunkX + CHUNK_SIZE
                        || rotated[1] < templateChunkY || rotated[1] >= templateChunkY + CHUNK_SIZE) {
                    continue;
                }

                int sx = (rotated[0] - templateChunkX) + chunkX * CHUNK_SIZE;
                int sy = (rotated[1] - templateChunkY) + chunkY * CHUNK_SIZE;
                int distance = knowPlayer ? Math.max(Math.abs(sx - playerSceneX), Math.abs(sy - playerSceneY)) : 0;
                if (best == NONE || distance < bestDistance) {
                    best = pack(sx, sy);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    /**
     * The scene tile of a point in either space: scene coordinates first, then (inside an
     * instance, {@code planeChunks != null}) template coordinates. {@link #NONE} if neither.
     */
    public static int resolve(int x, int y, int plane, int baseX, int baseY, int scenePlane,
                              int[][] planeChunks, int playerSceneX, int playerSceneY) {
        int direct = sceneTile(x, y, plane, baseX, baseY, scenePlane);
        if (direct != NONE || planeChunks == null) {
            return direct;
        }
        return templateToScene(planeChunks, x, y, playerSceneX, playerSceneY);
    }

    /** Chebyshev distance between two packed scene tiles, {@link Integer#MAX_VALUE} if either is {@link #NONE}. */
    public static int distance(int a, int b) {
        if (a == NONE || b == NONE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(Math.abs(sceneX(a) - sceneX(b)), Math.abs(sceneY(a) - sceneY(b)));
    }

    /** Same rotation as {@link Rs2LocalPoint#rotate}, on bare ints. */
    static int[] rotate(int x, int y, int rotation) {
        int chunkX = x & ~(CHUNK_SIZE - 1);
        int chunkY = y & ~(CHUNK_SIZE - 1);
        int cx = x & (CHUNK_SIZE - 1);
        int cy = y & (CHUNK_SIZE - 1);
        switch (rotation) {
            case 1:
                return new int[]{chunkX + cy, chunkY + (CHUNK_SIZE - 1 - cx)};
            case 2:
                return new int[]{chunkX + (CHUNK_SIZE - 1 - cx), chunkY + (CHUNK_SIZE - 1 - cy)};
            case 3:
                return new int[]{chunkX + (CHUNK_SIZE - 1 - cy), chunkY + cx};
            default:
                return new int[]{x, y};
        }
    }

    // ---- client-backed: call on the client thread ------------------------------------------

    /**
     * The scene tile of a point in either space on {@code wv}, as a {@link LocalPoint}, or null.
     * Client thread only.
     */
    public static LocalPoint toLocal(WorldView wv, WorldPoint point) {
        if (wv == null || point == null) {
            return null;
        }
        int[][] planeChunks = null;
        if (wv.isInstance()) {
            int[][][] chunks = wv.getInstanceTemplateChunks();
            int plane = wv.getPlane();
            if (chunks != null && plane >= 0 && plane < chunks.length) {
                planeChunks = chunks[plane];
            }
        }
        int playerSceneX = -1;
        int playerSceneY = -1;
        Client client = ProjectX.getClient();
        Player player = client != null ? client.getLocalPlayer() : null;
        LocalPoint playerLocal = player != null ? player.getLocalLocation() : null;
        if (playerLocal != null && playerLocal.getWorldView() == wv.getId()) {
            playerSceneX = playerLocal.getSceneX();
            playerSceneY = playerLocal.getSceneY();
        }
        int tile = sceneTile(point.getX(), point.getY(), point.getPlane(), wv.getBaseX(), wv.getBaseY(), wv.getPlane(),
                wv.getSizeX(), wv.getSizeY());
        if (tile == NONE && planeChunks != null) {
            tile = templateToScene(planeChunks, point.getX(), point.getY(), playerSceneX, playerSceneY);
        }
        return tile == NONE ? null : LocalPoint.fromScene(sceneX(tile), sceneY(tile), wv);
    }

    /**
     * The same tile as {@code point}, in scene coordinates of {@code wv} (the space the queryable
     * models use), or {@code point} unchanged when it is already in that space or names no tile on
     * the scene. Client thread only.
     */
    public static WorldPoint toSceneSpace(WorldView wv, WorldPoint point) {
        if (wv == null || point == null) {
            return point;
        }
        if (sceneTile(point.getX(), point.getY(), point.getPlane(), wv.getBaseX(), wv.getBaseY(), wv.getPlane(),
                wv.getSizeX(), wv.getSizeY()) != NONE) {
            return point;
        }
        if (!wv.isInstance()) {
            return point;
        }
        LocalPoint local = toLocal(wv, point);
        return local == null ? point : WorldPoint.fromScene(wv, local.getSceneX(), local.getSceneY(), wv.getPlane());
    }
}
