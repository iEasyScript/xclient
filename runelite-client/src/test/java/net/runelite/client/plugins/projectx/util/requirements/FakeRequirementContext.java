package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.util.magic.Runes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Game state for tests: set what you need, everything else is empty. */
final class FakeRequirementContext implements RequirementContext
{
    boolean loggedIn = true;
    boolean members = true;
    int spellbook = 0;
    final Map<Skill, Integer> levels = new EnumMap<>(Skill.class);
    final Map<Quest, QuestState> quests = new EnumMap<>(Quest.class);
    final List<HeldItem> inventory = new ArrayList<>();
    final List<HeldItem> equipment = new ArrayList<>();
    /** Null: the bank has not been opened. */
    List<HeldItem> bank = null;
    final Map<Runes, Integer> pouch = new EnumMap<>(Runes.class);
    /** Runes from staves etc., added to what is counted from the containers. */
    final Map<Runes, Integer> extraRunes = new EnumMap<>(Runes.class);

    FakeRequirementContext level(Skill skill, int level)
    {
        levels.put(skill, level);
        return this;
    }

    FakeRequirementContext quest(Quest quest, QuestState state)
    {
        quests.put(quest, state);
        return this;
    }

    FakeRequirementContext inv(int id, String name, int qty)
    {
        inventory.add(new HeldItem(id, name, qty));
        return this;
    }

    FakeRequirementContext worn(int id, String name, int qty)
    {
        equipment.add(new HeldItem(id, name, qty));
        return this;
    }

    FakeRequirementContext banked(int id, String name, int qty)
    {
        if (bank == null)
        {
            bank = new ArrayList<>();
        }
        bank.add(new HeldItem(id, name, qty));
        return this;
    }

    FakeRequirementContext bankOpenedEmpty()
    {
        bank = new ArrayList<>();
        return this;
    }

    @Override
    public boolean isLoggedIn()
    {
        return loggedIn;
    }

    @Override
    public int realLevel(Skill skill)
    {
        return levels.getOrDefault(skill, 1);
    }

    @Override
    public QuestState questState(Quest quest)
    {
        return quests.getOrDefault(quest, QuestState.NOT_STARTED);
    }

    @Override
    public boolean isMembersWorld()
    {
        return members;
    }

    @Override
    public List<HeldItem> inventory()
    {
        return inventory;
    }

    @Override
    public List<HeldItem> equipment()
    {
        return equipment;
    }

    @Override
    public Optional<List<HeldItem>> bank()
    {
        return Optional.ofNullable(bank);
    }

    @Override
    public Map<Runes, Integer> runePouch()
    {
        return pouch;
    }

    @Override
    public Map<Runes, Integer> availableRunes(boolean includeBank)
    {
        Map<Runes, Integer> out = new HashMap<>(pouch);
        addRunes(out, inventory);
        if (includeBank && bank != null)
        {
            addRunes(out, bank);
        }
        extraRunes.forEach((r, q) -> out.merge(r, q, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b)));
        return out;
    }

    private static void addRunes(Map<Runes, Integer> out, List<HeldItem> items)
    {
        for (HeldItem h : items)
        {
            Runes r = Runes.byItemId(h.getId());
            if (r != null)
            {
                out.merge(r, h.getQuantity(), Integer::sum);
            }
        }
    }

    @Override
    public int spellbook()
    {
        return spellbook;
    }
}
