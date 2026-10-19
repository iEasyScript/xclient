package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsItem;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsStackCompareID;
import net.runelite.client.plugins.projectx.util.magic.Runes;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InventorySetupDiffTest
{
    private static final int SHARK = 385;
    private static final int LOBSTER = 379;
    private static final int DRAGON_SCIMITAR = 4587;
    private static final int PRAYER_POTION_4 = 2434;
    private static final int PRAYER_POTION_2 = 141;
    private static final int LAW_RUNE = 563;
    private static final int AIR_RUNE = 556;
    private static final int RUNE_POUCH = 12791;

    private static final int SPELLBOOK_ANY = 4;
    private static final int LUNAR = 2;

    private final List<InventorySetupsItem> inventory = new ArrayList<>();
    private final List<InventorySetupsItem> equipment = new ArrayList<>();
    private final List<InventorySetupsItem> runePouch = new ArrayList<>();
    private int spellbook = SPELLBOOK_ANY;

    private final FakeRequirementContext ctx = new FakeRequirementContext();

    private static InventorySetupsItem row(int id, String name, int qty)
    {
        return new InventorySetupsItem(id, name, qty, false, InventorySetupsStackCompareID.None, false, -1);
    }

    private static InventorySetupsItem row(int id, String name, int qty, InventorySetupsStackCompareID sc, boolean fuzzy)
    {
        return new InventorySetupsItem(id, name, qty, fuzzy, sc, false, -1);
    }

    private InventorySetup setup()
    {
        List<InventorySetupsItem> eq = new ArrayList<>(equipment);
        // Real setups store every worn slot, empty ones as dummies.
        eq.add(InventorySetupsItem.getDummyItem());
        List<InventorySetupsItem> inv = new ArrayList<>(inventory);
        inv.add(InventorySetupsItem.getDummyItem());
        return new InventorySetup(inv, eq, runePouch.isEmpty() ? null : runePouch, null, null, new HashMap<>(),
            "Main", "", null, false, null, false, false, spellbook, false, -1);
    }

    private InventorySetupDiff diff()
    {
        return InventorySetupDiff.compute(setup(), ctx.inventory, ctx.equipment, ctx.bank, ctx.pouch, ctx.spellbook);
    }

    @Test
    public void completeWhenEverythingIsCarriedOrBanked()
    {
        equipment.add(row(DRAGON_SCIMITAR, "Dragon scimitar", 1));
        inventory.add(row(SHARK, "Shark", 1));
        inventory.add(row(SHARK, "Shark", 1));
        ctx.worn(DRAGON_SCIMITAR, "Dragon scimitar", 1).inv(SHARK, "Shark", 1).banked(SHARK, "Shark", 20);

        InventorySetupDiff d = diff();
        assertEquals(2, d.getLines().size());
        assertFalse(d.isIncomplete());
        RequirementResult r = d.toResult();
        assertEquals(RequirementStatus.MET, r.getStatus());
        assertEquals("All 2 items found", r.getDetail());
    }

    @Test
    public void rowsForTheSameItemAddUp()
    {
        for (int i = 0; i < 10; i++)
        {
            inventory.add(row(SHARK, "Shark", 1));
        }
        ctx.inv(SHARK, "Shark", 1).banked(SHARK, "Shark", 3);

        InventorySetupDiff d = diff();
        InventorySetupDiff.Line line = d.getLines().get(0);
        assertEquals(10, line.getNeeded());
        assertEquals(4, line.getHave());
        assertEquals(InventorySetupDiff.State.MISSING, line.getState());
        assertEquals("Shark: need 10, you have 4", line.describe());
    }

    @Test
    public void missingItemsAreListedWhenTheBankIsKnown()
    {
        equipment.add(row(DRAGON_SCIMITAR, "Dragon scimitar", 1));
        inventory.add(row(SHARK, "Shark", 5));
        ctx.inv(SHARK, "Shark", 5).bankOpenedEmpty();

        RequirementResult r = diff().toResult();
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals("Missing 1 of 2 items", r.getDetail());
        assertEquals(List.of("Dragon scimitar: not found"), r.getNotes());
    }

    @Test
    public void unknownWhenNotCarriedAndTheBankWasNeverOpened()
    {
        inventory.add(row(SHARK, "Shark", 5));
        inventory.add(row(LOBSTER, "Lobster", 1));
        ctx.inv(LOBSTER, "Lobster", 1);

        InventorySetupDiff d = diff();
        assertFalse(d.isBankKnown());
        assertEquals(InventorySetupDiff.State.UNKNOWN, d.getLines().get(0).getState());
        assertEquals(InventorySetupDiff.State.OK, d.getLines().get(1).getState());
        RequirementResult r = d.toResult();
        assertEquals(RequirementStatus.UNKNOWN, r.getStatus());
        assertTrue(r.getDetail(), r.getDetail().contains("open your bank once"));
        assertEquals(List.of("Shark x5: not carried, bank unknown"), r.getNotes());
    }

    @Test
    public void aDifferentDoseOfAPotionCounts()
    {
        inventory.add(row(PRAYER_POTION_4, "Prayer potion(4)", 1));
        ctx.inv(PRAYER_POTION_2, "Prayer potion(2)", 1).bankOpenedEmpty();
        assertEquals(RequirementStatus.MET, diff().toResult().getStatus());
    }

    @Test
    public void fuzzyRowsMatchByPartOfTheName()
    {
        inventory.add(row(PRAYER_POTION_4, "Prayer potion(4)", 1, InventorySetupsStackCompareID.None, true));
        ctx.inv(9999, "Super prayer potion(1)", 1).bankOpenedEmpty();
        assertEquals(RequirementStatus.MET, diff().toResult().getStatus());
    }

    @Test
    public void looseStackRowsNeedOnlyOne()
    {
        // "<" and "!=" rows are withdrawn up to what the bank has, so any amount will do.
        inventory.add(row(LAW_RUNE, "Law rune", 1000, InventorySetupsStackCompareID.Less_Than, false));
        inventory.add(row(SHARK, "Shark", 20, InventorySetupsStackCompareID.Standard, false));
        ctx.banked(LAW_RUNE, "Law rune", 3).banked(SHARK, "Shark", 1);
        assertEquals(RequirementStatus.MET, diff().toResult().getStatus());
    }

    @Test
    public void exactStackRowsNeedTheFullAmount()
    {
        inventory.add(row(LAW_RUNE, "Law rune", 1000));
        ctx.banked(LAW_RUNE, "Law rune", 999);
        RequirementResult r = diff().toResult();
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals(List.of("Law rune: need 1000, you have 999"), r.getNotes());
    }

    @Test
    public void runePouchRowsNeedAFifthAndCountThePouch()
    {
        inventory.add(row(RUNE_POUCH, "Rune pouch", 1));
        runePouch.add(row(LAW_RUNE, "Law rune", 100));
        runePouch.add(row(AIR_RUNE, "Air rune", 1000));
        ctx.inv(RUNE_POUCH, "Rune pouch", 1).bankOpenedEmpty();
        ctx.pouch.put(Runes.LAW, 20);
        ctx.pouch.put(Runes.AIR, 150);

        InventorySetupDiff d = diff();
        InventorySetupDiff.Line air = d.getLines().stream().filter(l -> l.getName().equals("Air rune")).findFirst().orElseThrow();
        assertEquals(200, air.getNeeded());
        assertEquals(InventorySetupDiff.State.MISSING, air.getState());
        InventorySetupDiff.Line law = d.getLines().stream().filter(l -> l.getName().equals("Law rune")).findFirst().orElseThrow();
        assertEquals(InventorySetupDiff.State.OK, law.getState());
    }

    @Test
    public void wrongSpellbookIsReported()
    {
        spellbook = LUNAR;
        ctx.spellbook = 0;
        ctx.bankOpenedEmpty();
        InventorySetupDiff d = diff();
        assertEquals("Spellbook: Lunar needed, you're on Standard", d.getSpellbookProblem());
        RequirementResult r = d.toResult();
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals("Wrong spellbook", r.getDetail());
    }

    @Test
    public void anySpellbookIsFine()
    {
        spellbook = SPELLBOOK_ANY;
        ctx.spellbook = LUNAR;
        assertNull(diff().getSpellbookProblem());
    }

    @Test
    public void dummyRowsAreIgnored()
    {
        RequirementResult r = diff().toResult();
        assertEquals(RequirementStatus.MET, r.getStatus());
        assertEquals("The setup is empty", r.getDetail());
    }

    @Test
    public void missingIsListedBeforeUnknown()
    {
        spellbook = LUNAR;
        inventory.add(row(SHARK, "Shark", 1));
        ctx.spellbook = 0;
        RequirementResult r = diff().toResult();
        // Bank unknown, so the shark is only unknown, but the spellbook is known to be wrong.
        assertEquals(RequirementStatus.UNMET, r.getStatus());
        assertEquals(List.of("Spellbook: Lunar needed, you're on Standard", "Shark: not carried, bank unknown"), r.getNotes());
    }

    @Test
    public void requirementWrapsTheDiff()
    {
        inventory.add(row(SHARK, "Shark", 2));
        ctx.inv(SHARK, "Shark", 2);
        Requirement req = Requirements.inventorySetup(setup());
        assertEquals("Inventory setup \"Main\"", req.getDescription());
        assertEquals(RequirementStatus.MET, req.evaluate(ctx).getStatus());

        ctx.loggedIn = false;
        assertEquals(RequirementStatus.UNKNOWN, req.evaluate(ctx).getStatus());
    }

    @Test
    public void noSetupChosenIsUnmet()
    {
        assertEquals(RequirementStatus.UNMET, Requirements.inventorySetup((InventorySetup) null).evaluate(ctx).getStatus());
    }

    @Test
    public void nullListsAreTreatedAsEmpty()
    {
        inventory.add(row(SHARK, "Shark", 1));
        InventorySetupDiff d = InventorySetupDiff.compute(setup(), null, null, Collections.emptyList(), null, -1);
        assertEquals(InventorySetupDiff.State.MISSING, d.getLines().get(0).getState());
    }
}
