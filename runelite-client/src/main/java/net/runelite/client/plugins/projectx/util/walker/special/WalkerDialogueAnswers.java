package net.runelite.client.plugins.projectx.util.walker.special;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.projectx.util.dialogues.Rs2Dialogue;
import net.runelite.client.plugins.projectx.util.misc.Rs2UiHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One table of the dialogue options the walker answers on its own while walking: the "are you
 * sure?" prompts that sit between a click and the far side of an obstacle. Most of the strings come
 * from the DAX Web Walker (Apache-2.0, see shortestpath/THIRD_PARTY_DAX.txt): its PathObjectHandler,
 * NavigationSpecialCase and DoomsToggle answered them, and ours left the menu open and clicked the
 * obstacle again in a loop.
 *
 * <p>Only answers that cannot mean anything else are in the shared table; a bare "Yes." belongs to
 * the one obstacle that asks it and is passed as an extra answer by that obstacle's handler.
 * Matching ignores case, colour tags, repeated spaces and trailing punctuation, nothing more.
 */
@Slf4j
public final class WalkerDialogueAnswers {

    /** Answers that are safe to give whenever they are on screen while the walker runs. */
    static final List<String> SHARED_ANSWERS = Collections.unmodifiableList(Arrays.asList(
            // Revenant cave entrances, low, high and mid (DAX PathObjectHandler WILDERNESS_CAVERN*)
            "Yes, and don't ask again.",
            "Yes, don't ask again.",
            // Killerwatt plane rift (DAX NavigationSpecialCase KILLERWAT_PLANE_*)
            "Yes I want to go in and don't show me this message again.",
            "Thanks for the warning, but I'm not scared of any monster.",
            // The Doomsayer's warnings when they come up as a dialogue (DAX DoomsToggle)
            "Enter the swamp.",
            "I'll be fine without a tinderbox.",
            "Proceed regardless",
            "Let me jump, and don't warn me again!",
            // Brimhaven Dungeon entrance fee (DAX NavigationSpecialCase BRIMHAVEN_DUNGEON)
            "Pay 875 coins to enter once",
            // Shilo Village gate (DAX NavigationSpecialCase SHILO_INSIDE)
            "Yes, Ok, I'll go into the village!",
            // Karamja ship before Pirate's Treasure (DAX NavigationSpecialCase handleKaramjaShip)
            "Can I journey on this ship?",
            "Search away, I have nothing to hide."));

    private WalkerDialogueAnswers() {
    }

    /**
     * The option to press: the first shared answer on screen, else the first extra answer on
     * screen. Pure, for tests.
     *
     * @return the option text exactly as offered, or null when none of the answers is offered
     */
    static String pick(List<String> options, List<String> extraAnswers) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        String hit = firstOffered(options, SHARED_ANSWERS);
        if (hit == null && extraAnswers != null) {
            hit = firstOffered(options, extraAnswers);
        }
        return hit;
    }

    private static String firstOffered(List<String> options, List<String> answers) {
        for (String answer : answers) {
            String wanted = normalise(answer);
            for (String option : options) {
                if (option != null && normalise(option).equals(wanted)) {
                    return option;
                }
            }
        }
        return null;
    }

    static String normalise(String text) {
        if (text == null) {
            return "";
        }
        String s = Rs2UiHelper.stripColTags(text).replace(' ', ' ').trim().replaceAll("\\s+", " ");
        while (!s.isEmpty() && ".!?".indexOf(s.charAt(s.length() - 1)) >= 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s.toLowerCase(Locale.ROOT);
    }

    /**
     * Presses a known answer when an option menu is open. Script thread only (it presses a key).
     *
     * @param extraAnswers answers that only the calling obstacle may give (e.g. "Yes."); may be empty
     * @return true when an option was pressed
     */
    public static boolean answerIfOpen(String... extraAnswers) {
        if (!Rs2Dialogue.hasSelectAnOption()) {
            return false;
        }
        List<String> options = Rs2Dialogue.getDialogueOptionTexts();
        String option = pick(options, extraAnswers == null ? null : Arrays.asList(extraAnswers));
        if (option == null) {
            return false;
        }
        log.info("[Walker] answering walker dialogue: \"{}\"", option);
        final String wanted = option;
        return Rs2Dialogue.clickOptionMatching(text -> wanted.equals(text));
    }
}
