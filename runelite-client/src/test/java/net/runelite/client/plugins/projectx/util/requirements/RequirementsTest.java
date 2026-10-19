package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.util.magic.Runes;
import org.junit.Test;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RequirementsTest
{
    private static final int COINS = 995;
    private static final int RUNE_POUCH = 12791;
    private static final int AIR_RUNE = 556;
    private static final int LAW_RUNE = 563;
    private static final int NATURE_RUNE = 561;

    private final FakeRequirementContext ctx = new FakeRequirementContext();

    // ---- skills

    @Test
    public void skillMetWhenLevelHighEnough()
    {
        ctx.level(Skill.MAGIC, 60);
        RequirementResult r = Requirements.skill(Skill.MAGIC, 55).evaluate(ctx);
        assertEquals(RequirementStatus.MET, r.getStatus());
    }

    @Test
    public void skillUnmetSaysNeededAndHave()
    {
        ctx.level(Skill.MAGIC, 43);
        Requirement req = Requirements.skill(Skill.MAGIC, 55);
        RequirementResult r = req.evaluate(ctx);
        assertEquals("Magic 55", req.getDescription());
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals("Magic 55 needed, you have 43", r.getDetail());
    }

    @Test
    public void everythingUnknownWhenLoggedOut()
    {
        ctx.loggedIn = false;
        assertEquals(RequirementStatus.UNKNOWN, Requirements.skill(Skill.MAGIC, 1).evaluate(ctx).getStatus());
        assertEquals(RequirementStatus.UNKNOWN, Requirements.questCompleted(Quest.COOKS_ASSISTANT).evaluate(ctx).getStatus());
        assertEquals(RequirementStatus.UNKNOWN, Requirements.membersWorld().evaluate(ctx).getStatus());
        assertEquals(RequirementStatus.UNKNOWN, Requirements.item("Coins", 1, COINS).evaluate(ctx).getStatus());
    }

    // ---- quests

    @Test
    public void questStates()
    {
        Requirement done = Requirements.questCompleted(Quest.COOKS_ASSISTANT);
        assertEquals(RequirementStatus.UNMET, done.evaluate(ctx).getStatus());
        assertEquals("Not started", done.evaluate(ctx).getDetail());

        ctx.quest(Quest.COOKS_ASSISTANT, QuestState.IN_PROGRESS);
        assertEquals(RequirementStatus.UNMET, done.evaluate(ctx).getStatus());
        assertEquals(RequirementStatus.MET, Requirements.questStarted(Quest.COOKS_ASSISTANT).evaluate(ctx).getStatus());

        ctx.quest(Quest.COOKS_ASSISTANT, QuestState.FINISHED);
        assertEquals(RequirementStatus.MET, done.evaluate(ctx).getStatus());
    }

    // ---- world

    @Test
    public void membersWorld()
    {
        assertEquals(RequirementStatus.MET, Requirements.membersWorld().evaluate(ctx).getStatus());
        ctx.members = false;
        assertEquals(RequirementStatus.UNMET, Requirements.membersWorld().evaluate(ctx).getStatus());
    }

    // ---- items

    @Test
    public void itemCountsAcrossInventoryEquipmentAndBank()
    {
        ctx.inv(COINS, "Coins", 400).banked(COINS, "Coins", 700);
        RequirementResult r = Requirements.item("Coins", 1000, COINS).evaluate(ctx);
        assertEquals(RequirementStatus.MET, r.getStatus());
        assertEquals("You have 1100", r.getDetail());
    }

    @Test
    public void itemUnknownWhenBankNotOpenedAndNotCarried()
    {
        RequirementResult r = Requirements.item("Rune pouch", 1, RUNE_POUCH).evaluate(ctx);
        assertEquals(RequirementStatus.UNKNOWN, r.getStatus());
        assertTrue(r.getDetail(), r.getDetail().contains("open your bank once"));
    }

    @Test
    public void itemMetWithoutBankWhenCarried()
    {
        ctx.inv(RUNE_POUCH, "Rune pouch", 1);
        assertEquals(RequirementStatus.MET, Requirements.item("Rune pouch", 1, RUNE_POUCH).evaluate(ctx).getStatus());
    }

    @Test
    public void itemUnmetWhenBankKnownAndShort()
    {
        ctx.inv(COINS, "Coins", 10).bankOpenedEmpty();
        RequirementResult r = Requirements.item("Coins", 1000, COINS).evaluate(ctx);
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals("Need 1000, you have 10", r.getDetail());
    }

    @Test
    public void itemOnlyCountsTheSourcesAsked()
    {
        ctx.banked(COINS, "Coins", 5000);
        Requirement inInventory = Requirements.item("Coins", 100, EnumSet.of(ItemSource.INVENTORY), COINS);
        assertEquals("Coins x100 in inventory", inInventory.getDescription());
        // The bank isn't a source, so its being known or not doesn't matter: this is plainly unmet.
        assertEquals(RequirementStatus.UNMET, inInventory.evaluate(ctx).getStatus());
    }

    @Test
    public void itemNamedIgnoresCase()
    {
        ctx.worn(4151, "Abyssal whip", 1);
        assertEquals(RequirementStatus.MET, Requirements.itemNamed("abyssal WHIP", 1).evaluate(ctx).getStatus());
        assertEquals(RequirementStatus.MET, Requirements.itemNamed("Abyssal whip", 1, ItemSource.CARRIED).evaluate(ctx).getStatus());
    }

    // ---- runes

    @Test
    public void runesCountPouchInventoryAndBank()
    {
        ctx.pouch.put(Runes.LAW, 60);
        ctx.inv(LAW_RUNE, "Law rune", 50);
        Map<Runes, Integer> want = new LinkedHashMap<>();
        want.put(Runes.LAW, 100);
        assertEquals(RequirementStatus.MET, Requirements.runes(want, false).evaluate(ctx).getStatus());
    }

    @Test
    public void runesStaffCountsAsUnlimited()
    {
        ctx.extraRunes.put(Runes.AIR, Integer.MAX_VALUE);
        Map<Runes, Integer> want = Map.of(Runes.AIR, 5000);
        assertEquals(RequirementStatus.MET, Requirements.runes(want, false).evaluate(ctx).getStatus());
    }

    @Test
    public void runesMissingListsWhatIsShort()
    {
        ctx.inv(AIR_RUNE, "Air rune", 100).inv(NATURE_RUNE, "Nature rune", 3);
        Map<Runes, Integer> want = new LinkedHashMap<>();
        want.put(Runes.AIR, 100);
        want.put(Runes.NATURE, 5);
        RequirementResult r = Requirements.runes(want, false).evaluate(ctx);
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals("Missing 2 Nature", r.getDetail());
    }

    @Test
    public void runesUnknownWhenBankCountsButNotOpened()
    {
        Map<Runes, Integer> want = Map.of(Runes.LAW, 10);
        assertEquals(RequirementStatus.UNKNOWN, Requirements.runes(want, true).evaluate(ctx).getStatus());

        ctx.banked(LAW_RUNE, "Law rune", 10);
        assertEquals(RequirementStatus.MET, Requirements.runes(want, true).evaluate(ctx).getStatus());
    }

    // ---- custom checks

    @Test
    public void aCheckThatThrowsIsUnknown()
    {
        Requirement broken = Requirement.of("Broken", c ->
        {
            throw new IllegalStateException("boom");
        });
        RequirementResult r = broken.evaluate(ctx);
        assertEquals(RequirementStatus.UNKNOWN, r.getStatus());
        assertEquals("Couldn't check: boom", r.getDetail());
    }

    @Test
    public void aCheckThatAnswersNothingIsUnknown()
    {
        assertEquals(RequirementStatus.UNKNOWN, Requirement.of("Null", c -> null).evaluate(ctx).getStatus());
    }

    @Test
    public void severityCanBeChanged()
    {
        Requirement r = Requirements.membersWorld();
        assertEquals(RequirementSeverity.REQUIRED, r.getSeverity());
        assertEquals(RequirementSeverity.RECOMMENDED, r.recommended().getSeverity());
        assertEquals(RequirementSeverity.REQUIRED, r.recommended().required().getSeverity());
        assertEquals(r.getKey(), r.recommended().getKey());
    }
}
