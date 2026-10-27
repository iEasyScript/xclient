package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.item.Rs2EnsouledHead;

/**
 * Ensouled heads and the spells that reanimate them.
 *
 * <p>The constants of {@link Rs2EnsouledHead} under a shorter name: {@code EnsouledHeads.GOBLIN} is the very
 * same value as {@code Rs2EnsouledHead.GOBLIN}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2EnsouledHead}:
 *
 * <pre>{@code
 * Rs2EnsouledHead choice = EnsouledHeads.GOBLIN;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class EnsouledHeads
{
    private EnsouledHeads()
    {
    }

    public static final Rs2EnsouledHead GOBLIN = Rs2EnsouledHead.GOBLIN;
    public static final Rs2EnsouledHead MONKEY = Rs2EnsouledHead.MONKEY;
    public static final Rs2EnsouledHead IMP = Rs2EnsouledHead.IMP;
    public static final Rs2EnsouledHead MINOTAUR = Rs2EnsouledHead.MINOTAUR;
    public static final Rs2EnsouledHead SCORPION = Rs2EnsouledHead.SCORPION;
    public static final Rs2EnsouledHead BEAR = Rs2EnsouledHead.BEAR;
    public static final Rs2EnsouledHead UNICORN = Rs2EnsouledHead.UNICORN;
    public static final Rs2EnsouledHead DOG = Rs2EnsouledHead.DOG;
    public static final Rs2EnsouledHead CHAOS_DRUID = Rs2EnsouledHead.CHAOS_DRUID;
    public static final Rs2EnsouledHead GIANT = Rs2EnsouledHead.GIANT;
    public static final Rs2EnsouledHead OGRE = Rs2EnsouledHead.OGRE;
    public static final Rs2EnsouledHead ELF = Rs2EnsouledHead.ELF;
    public static final Rs2EnsouledHead TROLL = Rs2EnsouledHead.TROLL;
    public static final Rs2EnsouledHead HORROR = Rs2EnsouledHead.HORROR;
    public static final Rs2EnsouledHead KALPHITE = Rs2EnsouledHead.KALPHITE;
    public static final Rs2EnsouledHead DAGANNOTH = Rs2EnsouledHead.DAGANNOTH;
    public static final Rs2EnsouledHead BLOODVELD = Rs2EnsouledHead.BLOODVELD;
    public static final Rs2EnsouledHead TZHAAR = Rs2EnsouledHead.TZHAAR;
    public static final Rs2EnsouledHead DEMON = Rs2EnsouledHead.DEMON;
    public static final Rs2EnsouledHead HELLHOUND = Rs2EnsouledHead.HELLHOUND;
    public static final Rs2EnsouledHead AVIANSIE = Rs2EnsouledHead.AVIANSIE;
    public static final Rs2EnsouledHead ABYSSAL = Rs2EnsouledHead.ABYSSAL;
    public static final Rs2EnsouledHead DRAGON = Rs2EnsouledHead.DRAGON;
    public static final Rs2EnsouledHead ALL_IN_BANK = Rs2EnsouledHead.ALL_IN_BANK;

    /** Every value, same as {@code Rs2EnsouledHead.values()}. */
    public static Rs2EnsouledHead[] values()
    {
        return Rs2EnsouledHead.values();
    }

    /** Same as {@code Rs2EnsouledHead.valueOf(name)}. */
    public static Rs2EnsouledHead valueOf(String name)
    {
        return Rs2EnsouledHead.valueOf(name);
    }

    /** Same as {@code Rs2EnsouledHead.getReanimatableHead(...)}. */
    public static Rs2EnsouledHead getReanimatableHead()
    {
        return Rs2EnsouledHead.getReanimatableHead();
    }

    /** Same as {@code Rs2EnsouledHead.forItem(...)}. */
    public static Rs2EnsouledHead forItem(net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel item)
    {
        return Rs2EnsouledHead.forItem(item);
    }

    /** Same as {@code Rs2EnsouledHead.isNpcReanimated(...)}. */
    public static boolean isNpcReanimated(net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel npc)
    {
        return Rs2EnsouledHead.isNpcReanimated(npc);
    }
}
