package net.runelite.client.plugins.projectx.util.coords;

import net.runelite.api.Scene;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The two coordinate spaces inside an instance (see {@link SceneSpace}). Obor's lair, 2026-10-20:
 * the climbing rocks beside the player were "unreachable" because their instance point was looked
 * up as a template point and never found.
 */
public class SceneSpaceTest {

    // An instance scene placed at an arbitrary instance base, holding Obor-lair-like template chunks.
    private static final int BASE_X = 7680;
    private static final int BASE_Y = 4096;
    private static final int TEMPLATE_CHUNK_X = 3088 / 8;
    private static final int TEMPLATE_CHUNK_Y = 9800 / 8;

    private static int chunk(int plane, int templateChunkX, int templateChunkY, int rotation) {
        return (plane << 24) | (templateChunkX << 14) | (templateChunkY << 3) | (rotation << 1);
    }

    /** 13x13 chunks, all empty (-1) except template chunk (386,1225) placed at scene chunk (6,6). */
    private static int[][][] instanceWithOneChunk(int rotation) {
        int[][][] chunks = new int[4][13][13];
        for (int[][] plane : chunks) {
            for (int[] column : plane) {
                java.util.Arrays.fill(column, -1);
            }
        }
        chunks[0][6][6] = chunk(0, TEMPLATE_CHUNK_X, TEMPLATE_CHUNK_Y, rotation);
        return chunks;
    }

    @Test
    public void templateAndInstancePointOfTheSameTileResolveToTheSameSceneTile() {
        int[][] plane = instanceWithOneChunk(0)[0];
        // Template tile (3090, 9805) is offset (2,5) inside its chunk, so scene (6*8+2, 6*8+5).
        int expected = SceneSpace.pack(50, 53);

        int fromTemplate = SceneSpace.resolve(3090, 9805, 0, BASE_X, BASE_Y, 0, plane, -1, -1);
        int fromInstance = SceneSpace.resolve(BASE_X + 50, BASE_Y + 53, 0, BASE_X, BASE_Y, 0, plane, -1, -1);

        assertEquals(expected, fromTemplate);
        assertEquals(expected, fromInstance);
    }

    @Test
    public void anInstancePointIsNotATemplatePoint() {
        // The bug: Rs2Tile.isVisited read every point inside an instance as a template point.
        // The object's instance point is not in any template chunk, so it was never "visited".
        int[][] plane = instanceWithOneChunk(0)[0];
        assertEquals(SceneSpace.NONE, SceneSpace.templateToScene(plane, BASE_X + 50, BASE_Y + 53, -1, -1));
    }

    @Test
    public void outsideAnInstanceOnlySceneCoordinatesCount() {
        assertEquals(SceneSpace.pack(10, 20), SceneSpace.resolve(3210, 3220, 0, 3200, 3200, 0, null, -1, -1));
        assertEquals("off the scene", SceneSpace.NONE, SceneSpace.resolve(3400, 3220, 0, 3200, 3200, 0, null, -1, -1));
        assertEquals("other plane", SceneSpace.NONE, SceneSpace.resolve(3210, 3220, 1, 3200, 3200, 0, null, -1, -1));
    }

    @Test
    public void aRepeatedTemplateChunkResolvesToTheCopyNearestThePlayer() {
        int[][][] chunks = instanceWithOneChunk(0);
        chunks[0][1][1] = chunks[0][6][6];
        int[][] plane = chunks[0];

        assertEquals("no player: first copy", SceneSpace.pack(10, 13), SceneSpace.templateToScene(plane, 3090, 9805, -1, -1));
        assertEquals("player beside the second copy", SceneSpace.pack(50, 53), SceneSpace.templateToScene(plane, 3090, 9805, 52, 52));
    }

    @Test
    public void templateToSceneInvertsRuneLitesFromLocalInstanceForEveryRotation() {
        for (int rotation = 0; rotation < 4; rotation++) {
            int[][][] chunks = instanceWithOneChunk(rotation);
            Scene scene = mock(Scene.class);
            when(scene.isInstance()).thenReturn(true);
            when(scene.getInstanceTemplateChunks()).thenReturn(chunks);

            for (int sx = 48; sx < 56; sx++) {
                for (int sy = 48; sy < 56; sy++) {
                    WorldPoint template = WorldPoint.fromLocalInstance(scene, new LocalPoint(sx * 128 + 64, sy * 128 + 64, -1), 0);
                    assertEquals("rotation " + rotation + " scene " + sx + "," + sy,
                            SceneSpace.pack(sx, sy),
                            SceneSpace.templateToScene(chunks[0], template.getX(), template.getY(), -1, -1));
                }
            }
        }
    }

    @Test
    public void distanceIsChebyshevAndUnknownIsFar() {
        assertEquals(3, SceneSpace.distance(SceneSpace.pack(10, 10), SceneSpace.pack(13, 12)));
        assertEquals(Integer.MAX_VALUE, SceneSpace.distance(SceneSpace.NONE, SceneSpace.pack(1, 1)));
    }

    // ---- WorldView-backed helpers (ProjectX has no client here, so the player is unknown) ----

    private static WorldView instanceView() {
        WorldView wv = mock(WorldView.class);
        when(wv.isInstance()).thenReturn(true);
        when(wv.getInstanceTemplateChunks()).thenReturn(instanceWithOneChunk(0));
        when(wv.getBaseX()).thenReturn(BASE_X);
        when(wv.getBaseY()).thenReturn(BASE_Y);
        when(wv.getSizeX()).thenReturn(104);
        when(wv.getSizeY()).thenReturn(104);
        when(wv.getPlane()).thenReturn(0);
        when(wv.getId()).thenReturn(-1);
        return wv;
    }

    @Test
    public void aTemplateAnchorIsMovedOntoTheInstanceScene() {
        // within(Rs2Player.getWorldLocation(), n) inside an instance: the anchor is a template
        // point, the entities are instance points.
        WorldPoint moved = SceneSpace.toSceneSpace(instanceView(), new WorldPoint(3090, 9805, 0));
        assertEquals(new WorldPoint(BASE_X + 50, BASE_Y + 53, 0), moved);
    }

    @Test
    public void anInstanceAnchorIsLeftAlone() {
        WorldPoint anchor = new WorldPoint(BASE_X + 50, BASE_Y + 53, 0);
        assertSame(anchor, SceneSpace.toSceneSpace(instanceView(), anchor));
    }

    @Test
    public void outsideAnInstanceAnchorsAreNeverConverted() {
        WorldView wv = mock(WorldView.class);
        when(wv.isInstance()).thenReturn(false);
        when(wv.getBaseX()).thenReturn(3200);
        when(wv.getBaseY()).thenReturn(3200);
        when(wv.getSizeX()).thenReturn(104);
        when(wv.getSizeY()).thenReturn(104);
        WorldPoint far = new WorldPoint(3090, 9805, 0);
        assertSame(far, SceneSpace.toSceneSpace(wv, far));
    }

    @Test
    public void toLocalTakesEitherSpace() {
        WorldView wv = instanceView();
        LocalPoint fromTemplate = SceneSpace.toLocal(wv, new WorldPoint(3090, 9805, 0));
        LocalPoint fromInstance = SceneSpace.toLocal(wv, new WorldPoint(BASE_X + 50, BASE_Y + 53, 0));
        assertEquals(50, fromTemplate.getSceneX());
        assertEquals(53, fromTemplate.getSceneY());
        assertEquals(fromTemplate, fromInstance);
        assertNull("neither space", SceneSpace.toLocal(wv, new WorldPoint(1000, 1000, 0)));
    }
}
