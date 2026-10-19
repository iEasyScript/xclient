package net.runelite.client.plugins.projectx.scriptqueue;

import net.runelite.api.Skill;

/**
 * What the queue's conditions read from the game. Every reading is -1 when it isn't
 * known (logged out, or a bank that hasn't been seen), and an unknown reading never
 * meets a condition. An interface so the conditions can be tested without a client.
 */
interface QueueGameState
{
    int UNKNOWN = -1;

    /** Real (unboosted) level. */
    int level(Skill skill);

    int xp(Skill skill);

    int totalLevel();

    int combatLevel();

    /** How many of the item, by exact name, are in the inventory (stacks counted in full). */
    int inventoryCount(String item);

    /** How many of the item, by exact name, the cached bank holds; -1 if the bank hasn't been seen. */
    int bankCount(String item);
}
