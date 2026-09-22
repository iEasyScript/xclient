package net.runelite.client.plugins.projectx.util.walker.shared;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.shortestpath.WorldPointUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Locks the band-aware distance fix that killed the Mor Ul Rek cave entrance/exit infinite loop.
 *
 * <p>OSRS shifts underground coords by +6400 on the Y axis. Plain straight-line distance ignores
 * that, so an inner underground region reads numerically "closer" to a surface goal than the
 * dungeon exit right next to it — which made the walker re-take the cave entrance forever.
 * {@link WorldPointUtil#undergroundAwareDistance} folds the Y band, and
 * {@link Rs2WalkerProgress#madeProgressToward} uses it so re-entering the cave is correctly NOT
 * counted as progress.
 */
public class Rs2WalkerProgressTest {

    // Karamja volcano dungeon (the cave exit landing) — wraps 9572 % 6400 = 3172, ~12 from surface.
    private static final WorldPoint VOLCANO_DUNGEON = new WorldPoint(2862, 9572, 0);
    // Mor Ul Rek interior (the cave entrance destination) — 5175 < 6400, stays ~2015 from surface.
    private static final WorldPoint MOR_UL_REK = new WorldPoint(2480, 5175, 0);
    // A surface goal on Karamja.
    private static final WorldPoint SURFACE_TARGET = new WorldPoint(2855, 3160, 0);

    @Test
    public void undergroundAwareDistance_wrapsDungeonCloseToSurface() {
        assertEquals(12, WorldPointUtil.undergroundAwareDistance(VOLCANO_DUNGEON, SURFACE_TARGET));
    }

    @Test
    public void undergroundAwareDistance_keepsInnerRegionFar() {
        assertEquals(2015, WorldPointUtil.undergroundAwareDistance(MOR_UL_REK, SURFACE_TARGET));
    }

    @Test
    public void reenteringCaveIsNotProgress() {
        // Standing at the cave exit (dungeon) and re-taking the entrance back into Mor Ul Rek must
        // NOT register as progress toward a surface goal — otherwise the walker loops forever.
        assertFalse(Rs2WalkerProgress.madeProgressToward(VOLCANO_DUNGEON, MOR_UL_REK, SURFACE_TARGET));
    }

    @Test
    public void steppingTowardSurfaceGoalIsProgress() {
        WorldPoint nearTarget = new WorldPoint(2856, 3161, 0);
        assertTrue(Rs2WalkerProgress.madeProgressToward(VOLCANO_DUNGEON, nearTarget, SURFACE_TARGET));
    }
}
