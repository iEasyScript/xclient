package net.runelite.client.plugins.projectx.util.walker.special;

import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WalkerSpecialTest {

    @Test
    public void picksSharedAnswerWhateverThePunctuationAndCase() {
        assertEquals("Yes, and don't ask again",
                WalkerDialogueAnswers.pick(Arrays.asList("No.", "Yes, and don't ask again", "Yes."), null));
        assertEquals("<col=ff0000>Can I journey on this ship?</col>",
                WalkerDialogueAnswers.pick(Arrays.asList("Never mind.", "<col=ff0000>Can I journey on this ship?</col>"), null));
    }

    @Test
    public void bareYesIsOnlyGivenWhenTheObstacleAsksForIt() {
        assertNull(WalkerDialogueAnswers.pick(Arrays.asList("Yes.", "No."), null));
        assertEquals("Yes.", WalkerDialogueAnswers.pick(Arrays.asList("Yes.", "No."), Collections.singletonList("Yes.")));
    }

    @Test
    public void unknownDialogueIsLeftAlone() {
        assertNull(WalkerDialogueAnswers.pick(Arrays.asList("Buy a pass", "Leave"), Collections.singletonList("Yes.")));
        assertNull(WalkerDialogueAnswers.pick(Collections.emptyList(), null));
    }

    @Test
    public void slashCategoriesMatchDax() {
        for (int c : new int[]{1, 4, 9, 10, 12, 17, 20, 21}) {
            assertTrue("category " + c, WebCutting.isSlashCategory(c));
        }
        for (int c : new int[]{0, 2, 3, 5, 11, 13, 15, 18}) {
            assertFalse("category " + c, WebCutting.isSlashCategory(c));
        }
    }

    @Test
    public void bladesByName() {
        assertTrue(WebCutting.isBladeName("Knife"));
        assertTrue(WebCutting.isBladeName("Rune scimitar"));
        assertTrue(WebCutting.isBladeName("Abyssal whip"));
        assertTrue(WebCutting.isBladeName("Dragon dagger(p++)"));
        assertTrue(WebCutting.isBladeName("Bronze axe"));
        assertFalse(WebCutting.isBladeName("Swordfish"));
        assertFalse(WebCutting.isBladeName("Raw swordfish"));
        assertFalse(WebCutting.isBladeName("Pickaxe"));
        assertFalse(WebCutting.isBladeName(null));
    }

    @Test
    public void specialObjectsAreTheDaxOnes() {
        assertTrue(WalkerSpecialObstacles.isSpecial(ObjectID.BIGWEB_SLASHABLE));
        assertTrue(WalkerSpecialObstacles.isSpecial(ObjectID.WILD_CAVE_ENTRANCE_LOW));
        assertTrue(WalkerSpecialObstacles.isSpecial(ObjectID.WILD_CAVE_ENTRANCE_HIGH));
        assertTrue(WalkerSpecialObstacles.isSpecial(ObjectID.WILD_CAVE_ENTRANCE_MID));
        assertTrue(WalkerSpecialObstacles.isSpecial(ObjectID.FREMENNIK_DIARY_ELITE_TELEPORT));
        assertFalse(WalkerSpecialObstacles.isSpecial(ObjectID.CRUMBLED_WALL));
    }
}
