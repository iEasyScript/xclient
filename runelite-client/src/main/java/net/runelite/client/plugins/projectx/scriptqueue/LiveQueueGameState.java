package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.bank.Rs2Bank;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The real game, for the queue's conditions. Called from the queue's own thread, once a
 * second: client reads hop onto the client thread, and nothing is read while logged out.
 */
final class LiveQueueGameState implements QueueGameState
{
    /**
     * Item names by id. A bank item's name can cost a hop onto the client thread, and
     * the bank holds hundreds: each id is looked up once, not once a second.
     */
    private final Map<Integer, String> names = new ConcurrentHashMap<>();

    @Override
    public int level(Skill skill)
    {
        if (!ProjectX.isLoggedIn())
        {
            return UNKNOWN;
        }
        int level = Rs2Player.getRealSkillLevel(skill);
        return level > 0 ? level : UNKNOWN;
    }

    @Override
    public int xp(Skill skill)
    {
        if (!ProjectX.isLoggedIn())
        {
            return UNKNOWN;
        }
        Client client = ProjectX.getClient();
        return ProjectX.getClientThread().runOnClientThreadOptional(() -> client.getSkillExperience(skill)).orElse(UNKNOWN);
    }

    @Override
    public int totalLevel()
    {
        if (!ProjectX.isLoggedIn())
        {
            return UNKNOWN;
        }
        Client client = ProjectX.getClient();
        int total = ProjectX.getClientThread().runOnClientThreadOptional(client::getTotalLevel).orElse(UNKNOWN);
        return total > 0 ? total : UNKNOWN;
    }

    @Override
    public int combatLevel()
    {
        if (!ProjectX.isLoggedIn())
        {
            return UNKNOWN;
        }
        Client client = ProjectX.getClient();
        int combat = ProjectX.getClientThread().runOnClientThreadOptional(() ->
        {
            Player me = client.getLocalPlayer();
            return me == null ? null : me.getCombatLevel();
        }).orElse(UNKNOWN);
        return combat > 0 ? combat : UNKNOWN;
    }

    @Override
    public int inventoryCount(String item)
    {
        if (item == null || !ProjectX.isLoggedIn())
        {
            return UNKNOWN;
        }
        return count(Rs2Inventory.items(), item);
    }

    @Override
    public int bankCount(String item)
    {
        if (item == null || !Rs2Bank.hasBankMirrorSnapshot())
        {
            return UNKNOWN;
        }
        return count(Rs2Bank.getAll(), item);
    }

    private int count(Stream<Rs2ItemModel> items, String item)
    {
        String wanted = item.trim();
        long total = items
            .filter(i -> i.getId() > 0 && wanted.equalsIgnoreCase(nameOf(i)))
            .mapToLong(Rs2ItemModel::getQuantity)
            .sum();
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private String nameOf(Rs2ItemModel item)
    {
        String name = names.get(item.getId());
        if (name == null)
        {
            name = item.getName();
            // Not kept when the lookup failed, so it is tried again next time.
            if (name != null && !name.equals("Unknown Item"))
            {
                names.put(item.getId(), name);
            }
        }
        return name;
    }
}
