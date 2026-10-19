package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.inventory.Rs2RunePouch;
import net.runelite.client.plugins.projectx.util.magic.RuneFilter;
import net.runelite.client.plugins.projectx.util.magic.Runes;
import net.runelite.client.plugins.projectx.util.magic.Rs2Magic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reads the live game for one evaluation. Must be used on the client thread: it reads skills,
 * runs the quest-state script and resolves item names. Lists are read once and kept for the
 * evaluation, so every requirement sees the same snapshot.
 */
final class LiveRequirementContext implements RequirementContext
{
    private final Client client;
    private List<HeldItem> inventory;
    private List<HeldItem> equipment;
    private Optional<List<HeldItem>> bank;

    LiveRequirementContext(Client client)
    {
        this.client = client;
        assert client.isClientThread();
    }

    @Override
    public boolean isLoggedIn()
    {
        return client.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null;
    }

    @Override
    public int realLevel(Skill skill)
    {
        return client.getRealSkillLevel(skill);
    }

    @Override
    public QuestState questState(Quest quest)
    {
        return quest.getState(client);
    }

    @Override
    public boolean isMembersWorld()
    {
        EnumSet<WorldType> types = client.getWorldType();
        return types != null && types.contains(WorldType.MEMBERS);
    }

    @Override
    public List<HeldItem> inventory()
    {
        if (inventory == null)
        {
            inventory = snapshot(Rs2Inventory.all());
        }
        return inventory;
    }

    @Override
    public List<HeldItem> equipment()
    {
        if (equipment == null)
        {
            equipment = snapshot(Rs2Equipment.items());
        }
        return equipment;
    }

    @Override
    public Optional<List<HeldItem>> bank()
    {
        if (bank == null)
        {
            // Known once the bank has been opened, this session or from the profile's saved snapshot.
            bank = Rs2Bank.hasBankMirrorSnapshot() ? Optional.of(snapshot(Rs2Bank.bankItems())) : Optional.empty();
        }
        return bank;
    }

    @Override
    public Map<Runes, Integer> runePouch()
    {
        return Rs2Inventory.hasRunePouch() ? new HashMap<>(Rs2RunePouch.getRunes()) : Collections.emptyMap();
    }

    @Override
    public Map<Runes, Integer> availableRunes(boolean includeBank)
    {
        return Rs2Magic.getRunes(RuneFilter.builder().includeBank(includeBank).build());
    }

    @Override
    public int spellbook()
    {
        return client.getVarbitValue(VarbitID.SPELLBOOK);
    }

    private static List<HeldItem> snapshot(List<Rs2ItemModel> items)
    {
        if (items == null || items.isEmpty())
        {
            return Collections.emptyList();
        }
        List<HeldItem> out = new ArrayList<>(items.size());
        for (Rs2ItemModel item : items)
        {
            if (item != null && item.getId() > 0)
            {
                out.add(new HeldItem(item.getId(), item.getName(), item.getQuantity()));
            }
        }
        return out;
    }
}
