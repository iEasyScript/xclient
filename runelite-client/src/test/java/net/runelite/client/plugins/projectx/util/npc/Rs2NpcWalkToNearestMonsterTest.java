package net.runelite.client.plugins.projectx.util.npc;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class Rs2NpcWalkToNearestMonsterTest {

    private static final String UNKNOWN_MONSTER = "Unknown monster without location data";

    @Test
    public void walkToNearestMonsterReturnsFalseWhenNoLocationIsFound() {
        assertFalse(Rs2Npc.walkToNearestMonster(UNKNOWN_MONSTER));
    }

    @Test
    public void walkToNearestMonsterWithClusteringReturnsFalseWhenNoLocationIsFound() {
        assertFalse(Rs2Npc.walkToNearestMonster(UNKNOWN_MONSTER, 3));
    }

    @Test
    public void walkToNearestMonsterWithWildernessFilterReturnsFalseWhenNoLocationIsFound() {
        assertFalse(Rs2Npc.walkToNearestMonster(UNKNOWN_MONSTER, 3, true));
    }
}
