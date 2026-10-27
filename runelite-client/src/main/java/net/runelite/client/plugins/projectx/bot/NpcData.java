package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.npc.Rs2NpcManager;

/**
 * NPC data loaded from the wiki dumps: stats, attack styles, locations. Named {@code NpcData} so it does not clash with RuneLite's {@code NpcManager}.
 *
 * <p>Every static method and field of {@link Rs2NpcManager} is available here under the shorter name,
 * so {@code Rs2NpcManager.anything(...)} can be written {@code NpcData.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class NpcData extends Rs2NpcManager
{
    private NpcData()
    {
    }
}
