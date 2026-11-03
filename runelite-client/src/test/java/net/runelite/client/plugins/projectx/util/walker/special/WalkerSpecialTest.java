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
    public void whistleChargeMessages() {
        assertEquals(17, QuetzalWhistle.chargesFromMessage("Your basic quetzal whistle has 17 charges remaining."));
        assertEquals(1, QuetzalWhistle.chargesFromMessage("<col=ef1020>Your perfected quetzal whistle has 1 charge remaining.</col>"));
        assertEquals(0, QuetzalWhistle.chargesFromMessage("Your quetzal whistle has no charges left."));
        assertEquals(-1, QuetzalWhistle.chargesFromMessage("You have 17 coins remaining."));
        assertEquals(-1, QuetzalWhistle.chargesFromMessage(null));
    }

    @Test
    public void storedEmptyWhistleExpires() {
        long now = 10_000_000L;
        assertEquals(5, QuetzalWhistle.parseStoredCharges("5", now));
        assertEquals(0, QuetzalWhistle.parseStoredCharges("0:" + (now - 1000), now));
        assertEquals(-1, QuetzalWhistle.parseStoredCharges("0:" + (now - QuetzalWhistle.EMPTY_VERDICT_TTL_MS - 1), now));
        assertEquals(-1, QuetzalWhistle.parseStoredCharges("junk", now));
        assertEquals(-1, QuetzalWhistle.parseStoredCharges(null, now));
    }

    @Test
    public void whistleLastDestinationValues() {
        assertEquals(1, QuetzalWhistle.lastDestinationValue(new net.runelite.api.coords.WorldPoint(1697, 3140, 0)));
        assertEquals(9, QuetzalWhistle.lastDestinationValue(new net.runelite.api.coords.WorldPoint(1389, 2901, 0)));
        assertEquals(-1, QuetzalWhistle.lastDestinationValue(new net.runelite.api.coords.WorldPoint(3200, 3200, 0)));
        assertEquals(-1, QuetzalWhistle.lastDestinationValue(null));
    }

    @Test
    public void gliderButtonsByActionWithTheOldIdsAsFallback() {
        assertTrue(GnomeGliderMenu.actionsName(new String[]{null, "Travel to <col=ff9040>Kar-Hewo</col>"}, "Kar-Hewo"));
        assertFalse(GnomeGliderMenu.actionsName(new String[]{"Travel to Gandius"}, "Kar-Hewo"));
        assertFalse(GnomeGliderMenu.actionsName(null, "Kar-Hewo"));
        // The component ids the walker used to hardcode.
        assertEquals(9043972, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Ta Quir Priw"));
        assertEquals(9043975, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Sindarpos"));
        assertEquals(9043978, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Lemanto Andra"));
        assertEquals(9043981, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Kar-Hewo"));
        assertEquals(9043984, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Gandius"));
        assertEquals(9043993, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Ookookolly Undri"));
        assertEquals(9043989, (int) GnomeGliderMenu.FALLBACK_BUTTONS.get("Lemantolly Undri"));
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
