package net.runelite.client.plugins.projectx.util.requirements;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Where an item requirement may be satisfied from.
 */
public enum ItemSource
{
    INVENTORY,
    EQUIPMENT,
    BANK;

    /** Inventory, worn, or in the bank. */
    public static final Set<ItemSource> ANYWHERE = Collections.unmodifiableSet(EnumSet.allOf(ItemSource.class));

    /** Inventory or worn: what the player has on them. */
    public static final Set<ItemSource> CARRIED = Collections.unmodifiableSet(EnumSet.of(INVENTORY, EQUIPMENT));
}
