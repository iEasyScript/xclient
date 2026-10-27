package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.misc.Rs2Food;

/**
 * Food items and how much they heal.
 *
 * <p>The constants of {@link Rs2Food} under a shorter name: {@code Foods.Dark_Crab} is the very
 * same value as {@code Rs2Food.Dark_Crab}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2Food}:
 *
 * <pre>{@code
 * Rs2Food choice = Foods.Dark_Crab;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class Foods
{
    private Foods()
    {
    }

    public static final Rs2Food Dark_Crab = Rs2Food.Dark_Crab;
    public static final Rs2Food ROCKTAIL = Rs2Food.ROCKTAIL;
    public static final Rs2Food MANTA = Rs2Food.MANTA;
    public static final Rs2Food SHARK = Rs2Food.SHARK;
    public static final Rs2Food KARAMBWAN = Rs2Food.KARAMBWAN;
    public static final Rs2Food LOBSTER = Rs2Food.LOBSTER;
    public static final Rs2Food TROUT = Rs2Food.TROUT;
    public static final Rs2Food SALMON = Rs2Food.SALMON;
    public static final Rs2Food SWORDFISH = Rs2Food.SWORDFISH;
    public static final Rs2Food TUNA = Rs2Food.TUNA;
    public static final Rs2Food MONKFISH = Rs2Food.MONKFISH;
    public static final Rs2Food SEA_TURTLE = Rs2Food.SEA_TURTLE;
    public static final Rs2Food CAKE = Rs2Food.CAKE;
    public static final Rs2Food BASS = Rs2Food.BASS;
    public static final Rs2Food COD = Rs2Food.COD;
    public static final Rs2Food POTATO = Rs2Food.POTATO;
    public static final Rs2Food BAKED_POTATO = Rs2Food.BAKED_POTATO;
    public static final Rs2Food POTATO_WITH_CHEESE = Rs2Food.POTATO_WITH_CHEESE;
    public static final Rs2Food EGG_POTATO = Rs2Food.EGG_POTATO;
    public static final Rs2Food CHILLI_POTATO = Rs2Food.CHILLI_POTATO;
    public static final Rs2Food MUSHROOM_POTATO = Rs2Food.MUSHROOM_POTATO;
    public static final Rs2Food TUNA_POTATO = Rs2Food.TUNA_POTATO;
    public static final Rs2Food SHRIMPS = Rs2Food.SHRIMPS;
    public static final Rs2Food HERRING = Rs2Food.HERRING;
    public static final Rs2Food SARDINE = Rs2Food.SARDINE;
    public static final Rs2Food CHOCOLATE_CAKE = Rs2Food.CHOCOLATE_CAKE;
    public static final Rs2Food ANCHOVIES = Rs2Food.ANCHOVIES;
    public static final Rs2Food PLAIN_PIZZA = Rs2Food.PLAIN_PIZZA;
    public static final Rs2Food MEAT_PIZZA = Rs2Food.MEAT_PIZZA;
    public static final Rs2Food ANCHOVY_PIZZA = Rs2Food.ANCHOVY_PIZZA;
    public static final Rs2Food PINEAPPLE_PIZZA = Rs2Food.PINEAPPLE_PIZZA;
    public static final Rs2Food BREAD = Rs2Food.BREAD;
    public static final Rs2Food APPLE_PIE = Rs2Food.APPLE_PIE;
    public static final Rs2Food REDBERRY_PIE = Rs2Food.REDBERRY_PIE;
    public static final Rs2Food MEAT_PIE = Rs2Food.MEAT_PIE;
    public static final Rs2Food PIKE = Rs2Food.PIKE;
    public static final Rs2Food POTATO_WITH_BUTTER = Rs2Food.POTATO_WITH_BUTTER;
    public static final Rs2Food BANANA = Rs2Food.BANANA;
    public static final Rs2Food PEACH = Rs2Food.PEACH;
    public static final Rs2Food ORANGE = Rs2Food.ORANGE;
    public static final Rs2Food PINEAPPLE_RINGS = Rs2Food.PINEAPPLE_RINGS;
    public static final Rs2Food PINEAPPLE_CHUNKS = Rs2Food.PINEAPPLE_CHUNKS;
    public static final Rs2Food JUG_OF_WINE = Rs2Food.JUG_OF_WINE;
    public static final Rs2Food COOKED_LARUPIA = Rs2Food.COOKED_LARUPIA;
    public static final Rs2Food COOKED_BARBTAILED_KEBBIT = Rs2Food.COOKED_BARBTAILED_KEBBIT;
    public static final Rs2Food COOKED_GRAAHK = Rs2Food.COOKED_GRAAHK;
    public static final Rs2Food COOKED_KYATT = Rs2Food.COOKED_KYATT;
    public static final Rs2Food COOKED_PYRE_FOX = Rs2Food.COOKED_PYRE_FOX;
    public static final Rs2Food COOKED_SUNLIGHT_ANTELOPE = Rs2Food.COOKED_SUNLIGHT_ANTELOPE;
    public static final Rs2Food COOKED_DASHING_KEBBIT = Rs2Food.COOKED_DASHING_KEBBIT;
    public static final Rs2Food COOKED_MOONLIGHT_ANTELOPE = Rs2Food.COOKED_MOONLIGHT_ANTELOPE;
    public static final Rs2Food PURPLE_SWEETS = Rs2Food.PURPLE_SWEETS;
    public static final Rs2Food CABBAGE = Rs2Food.CABBAGE;
    public static final Rs2Food BLIGHTED_MANTA_RAY = Rs2Food.BLIGHTED_MANTA_RAY;
    public static final Rs2Food BLIGHTED_ANGLERFISH = Rs2Food.BLIGHTED_ANGLERFISH;
    public static final Rs2Food BLIGHTED_KARAMBWAN = Rs2Food.BLIGHTED_KARAMBWAN;

    /** Every value, same as {@code Rs2Food.values()}. */
    public static Rs2Food[] values()
    {
        return Rs2Food.values();
    }

    /** Same as {@code Rs2Food.valueOf(name)}. */
    public static Rs2Food valueOf(String name)
    {
        return Rs2Food.valueOf(name);
    }

    /** Same as {@code Rs2Food.getIds(...)}. */
    public static java.util.Set<Integer> getIds()
    {
        return Rs2Food.getIds();
    }

    /** Same as {@code Rs2Food.getFastFoodIds(...)}. */
    public static java.util.Set<Integer> getFastFoodIds()
    {
        return Rs2Food.getFastFoodIds();
    }
}
