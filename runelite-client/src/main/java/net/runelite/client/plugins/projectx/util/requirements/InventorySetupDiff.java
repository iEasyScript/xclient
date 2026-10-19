package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsItem;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsStackCompareID;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetupsVariationMapping;
import net.runelite.client.plugins.projectx.util.magic.Runes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compares a saved {@link InventorySetup} with what the player owns: inventory, worn items,
 * rune pouch and the bank's cached contents. It answers "can the script load this setup",
 * not "is it loaded already": an item in the bank counts.
 *
 * <p>Follows {@code Rs2InventorySetup}'s matching rules, so the check agrees with what the
 * script will do when it loads the setup:
 * <ul>
 *   <li>a fuzzy row matches any item whose name contains the row's name;</li>
 *   <li>other rows match the same item id, a variation of it (charges, degradation), or the
 *       same name ignoring the dose, e.g. "Prayer potion(4)" and "Prayer potion(2)";</li>
 *   <li>a row with no stack indicator needs its full quantity; a row marked {@code !=},
 *       {@code <} or {@code >} is withdrawn "up to what the bank has", so one is enough;</li>
 *   <li>rune pouch rows need a fifth of their quantity, as {@code doesInventoryMatch} accepts;</li>
 *   <li>a setup on a specific spellbook needs that spellbook.</li>
 * </ul>
 * Rows for the same item (in the inventory, worn, or the pouch) are added together.
 * The bolt pouch, quiver and "additional filtered items" are not checked.
 */
public final class InventorySetupDiff
{
    /** One item the setup needs, and whether the player has it. */
    public enum State
    {
        OK,
        MISSING,
        /** Not carried, and the bank has not been opened, so it may be there. */
        UNKNOWN
    }

    public static final class Line
    {
        private final String name;
        private final int needed;
        private final long have;
        private final State state;

        Line(String name, int needed, long have, State state)
        {
            this.name = name;
            this.needed = needed;
            this.have = have;
            this.state = state;
        }

        public String getName()
        {
            return name;
        }

        public int getNeeded()
        {
            return needed;
        }

        public long getHave()
        {
            return have;
        }

        public State getState()
        {
            return state;
        }

        /** "Shark: need 10, you have 4", "Dragon scimitar: not found", "Rune pouch: not carried, bank unknown". */
        public String describe()
        {
            if (state == State.UNKNOWN)
            {
                return name + (needed > 1 ? " x" + needed : "") + ": not carried, bank unknown";
            }
            if (have <= 0)
            {
                return name + (needed > 1 ? " x" + needed : "") + ": not found";
            }
            return name + ": need " + needed + ", you have " + have;
        }

        @Override
        public String toString()
        {
            return state + " " + describe();
        }
    }

    private static final int SPELLBOOK_ANY = 4;

    private final String setupName;
    private final List<Line> lines;
    private final String spellbookProblem;
    private final boolean bankKnown;

    private InventorySetupDiff(String setupName, List<Line> lines, String spellbookProblem, boolean bankKnown)
    {
        this.setupName = setupName;
        this.lines = Collections.unmodifiableList(lines);
        this.spellbookProblem = spellbookProblem;
        this.bankKnown = bankKnown;
    }

    /**
     * @param bank             the bank's cached contents, or {@code null} when not known
     * @param runePouch        the rune pouch's contents; may be empty
     * @param currentSpellbook the spellbook varbit, or -1 if unknown
     */
    public static InventorySetupDiff compute(InventorySetup setup, List<HeldItem> inventory, List<HeldItem> equipment,
                                             List<HeldItem> bank, Map<Runes, Integer> runePouch, int currentSpellbook)
    {
        Map<String, Group> groups = new LinkedHashMap<>();
        if (setup != null)
        {
            addRows(groups, setup.getEquipment(), false);
            addRows(groups, setup.getInventory(), false);
            addRows(groups, setup.getRune_pouch(), true);
        }

        List<Line> lines = new ArrayList<>();
        for (Group g : groups.values())
        {
            long have = Requirements.count(inventory, h -> matches(g.row, h))
                + Requirements.count(equipment, h -> matches(g.row, h))
                + (bank != null ? Requirements.count(bank, h -> matches(g.row, h)) : 0);
            Runes rune = g.row.isFuzzy() ? null : Runes.byItemId(g.row.getId());
            if (rune != null && runePouch != null)
            {
                have += Math.max(0, runePouch.getOrDefault(rune, 0));
            }
            int need = g.need();
            State state = have >= need ? State.OK : bank == null ? State.UNKNOWN : State.MISSING;
            lines.add(new Line(g.row.getName(), need, have, state));
        }

        String spellbookProblem = null;
        if (setup != null)
        {
            int wanted = setup.getSpellBook();
            if (wanted >= 0 && wanted < SPELLBOOK_ANY && currentSpellbook >= 0 && wanted != currentSpellbook)
            {
                spellbookProblem = "Spellbook: " + Requirements.spellbookName(wanted) + " needed, you're on "
                    + Requirements.spellbookName(currentSpellbook);
            }
        }
        return new InventorySetupDiff(setup == null ? null : setup.getName(), lines, spellbookProblem, bank != null);
    }

    private static void addRows(Map<String, Group> groups, List<InventorySetupsItem> rows, boolean runePouch)
    {
        if (rows == null)
        {
            return;
        }
        for (InventorySetupsItem row : rows)
        {
            if (row == null || row.getId() == -1 || InventorySetupsItem.itemIsDummy(row))
            {
                continue;
            }
            if (runePouch && row.getQuantity() <= 0)
            {
                continue;
            }
            String key = row.isFuzzy()
                ? "f:" + row.getName().toLowerCase(Locale.ROOT)
                : "i:" + canonicalId(row.getId());
            Group g = groups.computeIfAbsent(key, k -> new Group(row));
            if (runePouch)
            {
                g.strictNeed += Math.max(1, (int) Math.ceil(row.getQuantity() * 0.2));
            }
            else if (isStrict(row))
            {
                g.strictNeed += Math.max(1, row.getQuantity());
            }
            else
            {
                g.loose = true;
            }
        }
    }

    /** No stack indicator: the script wants the exact quantity. */
    private static boolean isStrict(InventorySetupsItem row)
    {
        InventorySetupsStackCompareID sc = row.getStackCompare();
        return sc == null || sc == InventorySetupsStackCompareID.None;
    }

    private static final class Group
    {
        final InventorySetupsItem row;
        int strictNeed;
        boolean loose;

        Group(InventorySetupsItem row)
        {
            this.row = row;
        }

        int need()
        {
            return strictNeed > 0 ? strictNeed : 1;
        }
    }

    static boolean matches(InventorySetupsItem row, HeldItem held)
    {
        if (held == null || held.getId() <= 0)
        {
            return false;
        }
        if (row.isFuzzy())
        {
            String part = row.getName().toLowerCase(Locale.ROOT).trim();
            return !part.isEmpty() && held.getName().toLowerCase(Locale.ROOT).contains(part);
        }
        if (held.getId() == row.getId() || canonicalId(held.getId()) == canonicalId(row.getId()))
        {
            return true;
        }
        String base = baseName(row.getName());
        return !base.isEmpty() && base.equals(baseName(held.getName()));
    }

    private static int canonicalId(int id)
    {
        try
        {
            return InventorySetupsVariationMapping.map(id);
        }
        catch (RuntimeException | ExceptionInInitializerError e)
        {
            return id;
        }
    }

    /** Lower case, without a dose suffix like "(4)", and without a Barrows degradation number. */
    static String baseName(String name)
    {
        if (name == null)
        {
            return "";
        }
        String lower = name.toLowerCase(Locale.ROOT).trim();
        lower = lower.replaceAll("\\s*\\(\\d+\\)\\s*$", "");
        if (InventorySetupsItem.isBarrowsItem(lower))
        {
            lower = lower.replaceAll("\\s+\\d+$", "");
        }
        return lower.trim();
    }

    public String getSetupName()
    {
        return setupName;
    }

    public List<Line> getLines()
    {
        return lines;
    }

    /** Null when the spellbook is right, or the setup accepts any. */
    public String getSpellbookProblem()
    {
        return spellbookProblem;
    }

    public boolean isBankKnown()
    {
        return bankKnown;
    }

    public List<Line> linesIn(State state)
    {
        return lines.stream().filter(l -> l.getState() == state).collect(Collectors.toList());
    }

    /** Missing anything that is known to be missing. */
    public boolean isIncomplete()
    {
        return spellbookProblem != null || lines.stream().anyMatch(l -> l.getState() == State.MISSING);
    }

    public RequirementResult toResult()
    {
        List<Line> missing = linesIn(State.MISSING);
        List<Line> unknown = linesIn(State.UNKNOWN);

        List<String> notes = new ArrayList<>();
        missing.forEach(l -> notes.add(l.describe()));
        if (spellbookProblem != null)
        {
            notes.add(spellbookProblem);
        }
        unknown.forEach(l -> notes.add(l.describe()));

        if (isIncomplete())
        {
            String detail;
            if (missing.isEmpty())
            {
                detail = "Wrong spellbook";
            }
            else
            {
                detail = "Missing " + missing.size() + " of " + lines.size() + (lines.size() == 1 ? " item" : " items");
            }
            return RequirementResult.unmet(detail).withNotes(notes);
        }
        if (!unknown.isEmpty())
        {
            return RequirementResult.unknown(unknown.size() + (unknown.size() == 1 ? " item isn't" : " items aren't")
                + " carried. " + Requirements.OPEN_BANK).withNotes(notes);
        }
        if (lines.isEmpty())
        {
            return RequirementResult.met("The setup is empty");
        }
        return RequirementResult.met(lines.size() == 1 ? "Item found" : "All " + lines.size() + " items found");
    }
}
