package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.magic.Rs2Spells;

/**
 * Every standard, ancient, lunar and arceuus spell.
 *
 * <p>The constants of {@link Rs2Spells} under a shorter name: {@code Spells.CONFUSE} is the very
 * same value as {@code Rs2Spells.CONFUSE}. Java enums cannot be renamed without breaking compiled
 * scripts, so a variable or parameter still has the type {@code Rs2Spells}:
 *
 * <pre>{@code
 * Rs2Spells choice = Spells.CONFUSE;
 * }</pre>
 */
@SuppressWarnings("deprecation")
public final class Spells
{
    private Spells()
    {
    }

    public static final Rs2Spells CONFUSE = Rs2Spells.CONFUSE;
    public static final Rs2Spells WEAKEN = Rs2Spells.WEAKEN;
    public static final Rs2Spells CURSE = Rs2Spells.CURSE;
    public static final Rs2Spells BIND = Rs2Spells.BIND;
    public static final Rs2Spells SNARE = Rs2Spells.SNARE;
    public static final Rs2Spells VULNERABILITY = Rs2Spells.VULNERABILITY;
    public static final Rs2Spells ENFEEBLE = Rs2Spells.ENFEEBLE;
    public static final Rs2Spells ENTANGLE = Rs2Spells.ENTANGLE;
    public static final Rs2Spells STUN = Rs2Spells.STUN;
    public static final Rs2Spells TELE_BLOCK = Rs2Spells.TELE_BLOCK;
    public static final Rs2Spells LUMBRIDGE_HOME_TELEPORT = Rs2Spells.LUMBRIDGE_HOME_TELEPORT;
    public static final Rs2Spells VARROCK_TELEPORT = Rs2Spells.VARROCK_TELEPORT;
    public static final Rs2Spells LUMBRIDGE_TELEPORT = Rs2Spells.LUMBRIDGE_TELEPORT;
    public static final Rs2Spells FALADOR_TELEPORT = Rs2Spells.FALADOR_TELEPORT;
    public static final Rs2Spells TELEPORT_TO_HOUSE = Rs2Spells.TELEPORT_TO_HOUSE;
    public static final Rs2Spells CAMELOT_TELEPORT = Rs2Spells.CAMELOT_TELEPORT;
    public static final Rs2Spells ARDOUGNE_TELEPORT = Rs2Spells.ARDOUGNE_TELEPORT;
    public static final Rs2Spells WATCHTOWER_TELEPORT = Rs2Spells.WATCHTOWER_TELEPORT;
    public static final Rs2Spells TROLLHEIM_TELEPORT = Rs2Spells.TROLLHEIM_TELEPORT;
    public static final Rs2Spells CIVITAS_ILLA_FORTIS_TELEPORT = Rs2Spells.CIVITAS_ILLA_FORTIS_TELEPORT;
    public static final Rs2Spells KOUREND_CASTLE_TELEPORT = Rs2Spells.KOUREND_CASTLE_TELEPORT;
    public static final Rs2Spells TELEKINETIC_GRAB = Rs2Spells.TELEKINETIC_GRAB;
    public static final Rs2Spells BONES_TO_BANANAS = Rs2Spells.BONES_TO_BANANAS;
    public static final Rs2Spells BONES_TO_PEACHES = Rs2Spells.BONES_TO_PEACHES;
    public static final Rs2Spells LOW_LEVEL_ALCHEMY = Rs2Spells.LOW_LEVEL_ALCHEMY;
    public static final Rs2Spells SUPERHEAT_ITEM = Rs2Spells.SUPERHEAT_ITEM;
    public static final Rs2Spells HIGH_LEVEL_ALCHEMY = Rs2Spells.HIGH_LEVEL_ALCHEMY;
    public static final Rs2Spells ENCHANT_OPAL_BOLTS = Rs2Spells.ENCHANT_OPAL_BOLTS;
    public static final Rs2Spells ENCHANT_SAPPHIRE_BOLT = Rs2Spells.ENCHANT_SAPPHIRE_BOLT;
    public static final Rs2Spells ENCHANT_JADE_BOLT = Rs2Spells.ENCHANT_JADE_BOLT;
    public static final Rs2Spells ENCHANT_PEARL_BOLT = Rs2Spells.ENCHANT_PEARL_BOLT;
    public static final Rs2Spells ENCHANT_EMERALD_BOLT = Rs2Spells.ENCHANT_EMERALD_BOLT;
    public static final Rs2Spells ENCHANT_TOPAZ_BOLT = Rs2Spells.ENCHANT_TOPAZ_BOLT;
    public static final Rs2Spells ENCHANT_RUBY_BOLT = Rs2Spells.ENCHANT_RUBY_BOLT;
    public static final Rs2Spells ENCHANT_DIAMOND_BOLT = Rs2Spells.ENCHANT_DIAMOND_BOLT;
    public static final Rs2Spells ENCHANT_DRAGONSTONE_BOLT = Rs2Spells.ENCHANT_DRAGONSTONE_BOLT;
    public static final Rs2Spells ENCHANT_ONYX_BOLT = Rs2Spells.ENCHANT_ONYX_BOLT;
    public static final Rs2Spells ENCHANT_SAPPHIRE_JEWELLERY = Rs2Spells.ENCHANT_SAPPHIRE_JEWELLERY;
    public static final Rs2Spells ENCHANT_EMERALD_JEWELLERY = Rs2Spells.ENCHANT_EMERALD_JEWELLERY;
    public static final Rs2Spells ENCHANT_RUBY_JEWELLERY = Rs2Spells.ENCHANT_RUBY_JEWELLERY;
    public static final Rs2Spells ENCHANT_DIAMOND_JEWELLERY = Rs2Spells.ENCHANT_DIAMOND_JEWELLERY;
    public static final Rs2Spells ENCHANT_DRAGONSTONE_JEWELLERY = Rs2Spells.ENCHANT_DRAGONSTONE_JEWELLERY;
    public static final Rs2Spells ENCHANT_ONYX_JEWELLERY = Rs2Spells.ENCHANT_ONYX_JEWELLERY;
    public static final Rs2Spells ENCHANT_ZENYTE_JEWELLERY = Rs2Spells.ENCHANT_ZENYTE_JEWELLERY;
    public static final Rs2Spells CHARGE_WATER_ORB = Rs2Spells.CHARGE_WATER_ORB;
    public static final Rs2Spells CHARGE_EARTH_ORB = Rs2Spells.CHARGE_EARTH_ORB;
    public static final Rs2Spells CHARGE_FIRE_ORB = Rs2Spells.CHARGE_FIRE_ORB;
    public static final Rs2Spells CHARGE_AIR_ORB = Rs2Spells.CHARGE_AIR_ORB;
    public static final Rs2Spells PADDEWWA_TELEPORT = Rs2Spells.PADDEWWA_TELEPORT;
    public static final Rs2Spells SENNTISTEN_TELEPORT = Rs2Spells.SENNTISTEN_TELEPORT;
    public static final Rs2Spells KHARYRLL_TELEPORT = Rs2Spells.KHARYRLL_TELEPORT;
    public static final Rs2Spells LASSAR_TELEPORT = Rs2Spells.LASSAR_TELEPORT;
    public static final Rs2Spells DAREEYAK_TELEPORT = Rs2Spells.DAREEYAK_TELEPORT;
    public static final Rs2Spells CARRALLANGER_TELEPORT = Rs2Spells.CARRALLANGER_TELEPORT;
    public static final Rs2Spells ANNAKARL_TELEPORT = Rs2Spells.ANNAKARL_TELEPORT;
    public static final Rs2Spells GHORROCK_TELEPORT = Rs2Spells.GHORROCK_TELEPORT;
    public static final Rs2Spells MONSTER_EXAMINE = Rs2Spells.MONSTER_EXAMINE;
    public static final Rs2Spells CURE_OTHER = Rs2Spells.CURE_OTHER;
    public static final Rs2Spells CURE_ME = Rs2Spells.CURE_ME;
    public static final Rs2Spells CURE_GROUP = Rs2Spells.CURE_GROUP;
    public static final Rs2Spells STAT_SPY = Rs2Spells.STAT_SPY;
    public static final Rs2Spells DREAM = Rs2Spells.DREAM;
    public static final Rs2Spells STAT_RESTORE_POT_SHARE = Rs2Spells.STAT_RESTORE_POT_SHARE;
    public static final Rs2Spells BOOST_POTION_SHARE = Rs2Spells.BOOST_POTION_SHARE;
    public static final Rs2Spells ENERGY_TRANSFER = Rs2Spells.ENERGY_TRANSFER;
    public static final Rs2Spells HEAL_OTHER = Rs2Spells.HEAL_OTHER;
    public static final Rs2Spells VENGEANCE_OTHER = Rs2Spells.VENGEANCE_OTHER;
    public static final Rs2Spells VENGEANCE = Rs2Spells.VENGEANCE;
    public static final Rs2Spells HEAL_GROUP = Rs2Spells.HEAL_GROUP;
    public static final Rs2Spells MOONCLAN_TELEPORT = Rs2Spells.MOONCLAN_TELEPORT;
    public static final Rs2Spells TELE_GROUP_MOONCLAN = Rs2Spells.TELE_GROUP_MOONCLAN;
    public static final Rs2Spells OURANIA_TELEPORT = Rs2Spells.OURANIA_TELEPORT;
    public static final Rs2Spells WATERBIRTH_TELEPORT = Rs2Spells.WATERBIRTH_TELEPORT;
    public static final Rs2Spells TELE_GROUP_WATERBIRTH = Rs2Spells.TELE_GROUP_WATERBIRTH;
    public static final Rs2Spells BARBARIAN_TELEPORT = Rs2Spells.BARBARIAN_TELEPORT;
    public static final Rs2Spells TELE_GROUP_BARBARIAN = Rs2Spells.TELE_GROUP_BARBARIAN;
    public static final Rs2Spells KHAZARD_TELEPORT = Rs2Spells.KHAZARD_TELEPORT;
    public static final Rs2Spells TELE_GROUP_KHAZARD = Rs2Spells.TELE_GROUP_KHAZARD;
    public static final Rs2Spells FISHING_GUILD_TELEPORT = Rs2Spells.FISHING_GUILD_TELEPORT;
    public static final Rs2Spells TELE_GROUP_FISHING_GUILD = Rs2Spells.TELE_GROUP_FISHING_GUILD;
    public static final Rs2Spells CATHERBY_TELEPORT = Rs2Spells.CATHERBY_TELEPORT;
    public static final Rs2Spells TELE_GROUP_CATHERBY = Rs2Spells.TELE_GROUP_CATHERBY;
    public static final Rs2Spells ICE_PLATEAU_TELEPORT = Rs2Spells.ICE_PLATEAU_TELEPORT;
    public static final Rs2Spells TELE_GROUP_ICE_PLATEAU = Rs2Spells.TELE_GROUP_ICE_PLATEAU;
    public static final Rs2Spells BAKE_PIE = Rs2Spells.BAKE_PIE;
    public static final Rs2Spells GEOMANCY = Rs2Spells.GEOMANCY;
    public static final Rs2Spells CURE_PLANT = Rs2Spells.CURE_PLANT;
    public static final Rs2Spells NPC_CONTACT = Rs2Spells.NPC_CONTACT;
    public static final Rs2Spells HUMIDIFY = Rs2Spells.HUMIDIFY;
    public static final Rs2Spells HUNTER_KIT = Rs2Spells.HUNTER_KIT;
    public static final Rs2Spells SPIN_FLAX = Rs2Spells.SPIN_FLAX;
    public static final Rs2Spells SUPERGLASS_MAKE = Rs2Spells.SUPERGLASS_MAKE;
    public static final Rs2Spells TAN_LEATHER = Rs2Spells.TAN_LEATHER;
    public static final Rs2Spells STRING_JEWELLERY = Rs2Spells.STRING_JEWELLERY;
    public static final Rs2Spells MAGIC_IMBUE = Rs2Spells.MAGIC_IMBUE;
    public static final Rs2Spells FERTILE_SOIL = Rs2Spells.FERTILE_SOIL;
    public static final Rs2Spells PLANK_MAKE = Rs2Spells.PLANK_MAKE;
    public static final Rs2Spells RECHARGE_DRAGONSTONE = Rs2Spells.RECHARGE_DRAGONSTONE;
    public static final Rs2Spells SPELLBOOK_SWAP = Rs2Spells.SPELLBOOK_SWAP;
    public static final Rs2Spells ARCEUUS_LIBRARY_TELEPORT = Rs2Spells.ARCEUUS_LIBRARY_TELEPORT;
    public static final Rs2Spells DRAYNOR_MANOR_TELEPORT = Rs2Spells.DRAYNOR_MANOR_TELEPORT;
    public static final Rs2Spells BATTLEFRONT_TELEPORT = Rs2Spells.BATTLEFRONT_TELEPORT;
    public static final Rs2Spells MIND_ALTAR_TELEPORT = Rs2Spells.MIND_ALTAR_TELEPORT;
    public static final Rs2Spells RESPAWN_TELEPORT = Rs2Spells.RESPAWN_TELEPORT;
    public static final Rs2Spells SALVE_GRAVEYARD_TELEPORT = Rs2Spells.SALVE_GRAVEYARD_TELEPORT;
    public static final Rs2Spells FENKENSTRAINS_CASTLE_TELEPORT = Rs2Spells.FENKENSTRAINS_CASTLE_TELEPORT;
    public static final Rs2Spells HARMONY_ISLAND_TELEPORT = Rs2Spells.HARMONY_ISLAND_TELEPORT;
    public static final Rs2Spells CEMETERY_TELEPORT = Rs2Spells.CEMETERY_TELEPORT;
    public static final Rs2Spells BARROWS_TELEPORT = Rs2Spells.BARROWS_TELEPORT;
    public static final Rs2Spells APE_ATOLL_TELEPORT = Rs2Spells.APE_ATOLL_TELEPORT;
    public static final Rs2Spells GHOSTLY_GRASP = Rs2Spells.GHOSTLY_GRASP;
    public static final Rs2Spells SKELETAL_GRASP = Rs2Spells.SKELETAL_GRASP;
    public static final Rs2Spells UNDEAD_GRASP = Rs2Spells.UNDEAD_GRASP;
    public static final Rs2Spells INFERIOR_DEMONBANE = Rs2Spells.INFERIOR_DEMONBANE;
    public static final Rs2Spells SUPERIOR_DEMONBANE = Rs2Spells.SUPERIOR_DEMONBANE;
    public static final Rs2Spells DARK_DEMONBANE = Rs2Spells.DARK_DEMONBANE;
    public static final Rs2Spells LESSER_CORRUPTION = Rs2Spells.LESSER_CORRUPTION;
    public static final Rs2Spells GREATER_CORRUPTION = Rs2Spells.GREATER_CORRUPTION;
    public static final Rs2Spells DARK_LURE = Rs2Spells.DARK_LURE;
    public static final Rs2Spells MARK_OF_DARKNESS = Rs2Spells.MARK_OF_DARKNESS;
    public static final Rs2Spells WARD_OF_ARCEUUS = Rs2Spells.WARD_OF_ARCEUUS;
    public static final Rs2Spells BASIC_REANIMATION = Rs2Spells.BASIC_REANIMATION;
    public static final Rs2Spells ADEPT_REANIMATION = Rs2Spells.ADEPT_REANIMATION;
    public static final Rs2Spells EXPERT_REANIMATION = Rs2Spells.EXPERT_REANIMATION;
    public static final Rs2Spells MASTER_REANIMATION = Rs2Spells.MASTER_REANIMATION;
    public static final Rs2Spells DEMONIC_OFFERING = Rs2Spells.DEMONIC_OFFERING;
    public static final Rs2Spells SINISTER_OFFERING = Rs2Spells.SINISTER_OFFERING;
    public static final Rs2Spells SHADOW_VEIL = Rs2Spells.SHADOW_VEIL;
    public static final Rs2Spells VILE_VIGOUR = Rs2Spells.VILE_VIGOUR;
    public static final Rs2Spells DEGRIME = Rs2Spells.DEGRIME;
    public static final Rs2Spells RESURRECT_CROPS = Rs2Spells.RESURRECT_CROPS;
    public static final Rs2Spells DEATH_CHARGE = Rs2Spells.DEATH_CHARGE;

    /** Every value, same as {@code Rs2Spells.values()}. */
    public static Rs2Spells[] values()
    {
        return Rs2Spells.values();
    }

    /** Same as {@code Rs2Spells.valueOf(name)}. */
    public static Rs2Spells valueOf(String name)
    {
        return Rs2Spells.valueOf(name);
    }
}
