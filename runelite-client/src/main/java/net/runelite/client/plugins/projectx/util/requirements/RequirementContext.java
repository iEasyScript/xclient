package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.util.magic.Runes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The game state a {@link Requirement} check may read. The live implementation reads the
 * client on the client thread; tests pass a fake.
 */
public interface RequirementContext
{
    /** True when the player is logged in, so levels, quests and items can be read. */
    boolean isLoggedIn();

    /** The real (unboosted) level. */
    int realLevel(Skill skill);

    /** Null when it can't be read. */
    QuestState questState(Quest quest);

    boolean isMembersWorld();

    List<HeldItem> inventory();

    List<HeldItem> equipment();

    /**
     * The bank's contents, from the client's cache of it. Empty when the bank has not been
     * opened (this session or a saved snapshot), so the contents are not known.
     */
    Optional<List<HeldItem>> bank();

    /** Runes in the rune pouch, if the player carries one. */
    Map<Runes, Integer> runePouch();

    /**
     * Runes available to cast with: inventory, rune pouch, staves and tomes (as
     * {@link Integer#MAX_VALUE}), combination runes counted towards their base runes,
     * and the bank when {@code includeBank}.
     */
    Map<Runes, Integer> availableRunes(boolean includeBank);

    /** The spellbook varbit (0 standard, 1 ancient, 2 lunar, 3 arceuus), or -1 if unknown. */
    int spellbook();
}
