package net.runelite.client.plugins.projectx.util.grounditem;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LootingParameters {

    private int minValue, maxValue, range, minItems, minQuantity, minInvSlots;
    private boolean delayedLooting, antiLureProtection, eatFoodForSpace;
    private String[] names;
    private String[] ignoredNames;

    /**
     * This constructor is used to create a new LootingParameters object.
     * It sets the minimum value, maximum value, range, minimum items, delayed looting, and anti-lure protection.
     *
     * @param minValue           The minimum value of the items to be looted.
     * @param maxValue           The maximum value of the items to be looted.
     * @param range              The range within which the items to be looted are located.
     * @param minItems           The minimum number of items to be looted.
     * @param minInvSlots        The minimum number of inventory slots to have open.
     * @param delayedLooting     A boolean indicating whether looting should be delayed.
     * @param antiLureProtection A boolean indicating whether anti-lure protection should be enabled.
     */
    public LootingParameters(int minValue, int maxValue, int range, int minItems, int minInvSlots, boolean delayedLooting, boolean antiLureProtection) {
        setValues(minValue, maxValue, range, minItems, 1, minInvSlots, delayedLooting, antiLureProtection,false, null, null);
    }

    /**
     * This constructor is used to create a new LootingParameters object.
     * It sets the range, minimum items, minimum quantity, delayed looting, anti-lure protection, and names of the items to be looted.
     *
     * @param range              The range within which the items to be looted are located.
     * @param minItems           The minimum number of items to be looted.
     * @param minQuantity        The minimum quantity of items to be looted.
     * @param minInvSlots        The minimum number of inventory slots to have open.
     * @param delayedLooting     A boolean indicating whether looting should be delayed.
     * @param antiLureProtection A boolean indicating whether anti-lure protection should be enabled.
     * @param names              The names of the items to be looted.
     */
    public LootingParameters(int range, int minItems, int minQuantity, int minInvSlots, boolean delayedLooting, boolean antiLureProtection, String... names) {
        setValues(0, 0, range, minItems, minQuantity, minInvSlots, delayedLooting, antiLureProtection,false, null, names);
    }

    /**
     * This constructor is used to create a new LootingParameters object.
     * It sets the range, minimum items, minimum quantity, delayed looting, anti-lure protection, and names of the items to be looted.
     *
     * @param minValue           The minimum value of the items to be looted.
     * @param maxValue           The maximum value of the items to be looted.
     * @param range              The range within which the items to be looted are located.
     * @param minItems           The minimum number of items to be looted.
     * @param minInvSlots        The minimum number of inventory slots to have open.
     * @param delayedLooting     A boolean indicating whether looting should be delayed.
     * @param antiLureProtection A boolean indicating whether anti-lure protection should be enabled.
     * @param ignoredNames       The names of the items to be ignored.
     */

    public LootingParameters(int minValue, int maxValue, int range, int minItems, int minInvSlots, boolean delayedLooting, boolean antiLureProtection, String[] ignoredNames) {
        setValues(minValue, maxValue, range, minItems, 1, minInvSlots, delayedLooting, antiLureProtection,false, ignoredNames, null);
    }

    private void setValues(int minValue, int maxValue, int range, int minItems, int minQuantity, int minInvSlots, boolean delayedLooting, boolean antiLureProtection,boolean eatFoodForSpace, String[] ignoredNames, String[] names) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.range = range;
        this.minItems = minItems;
        this.minQuantity = minQuantity;
        this.minInvSlots = minInvSlots;
        this.delayedLooting = delayedLooting;
        this.antiLureProtection = antiLureProtection;
        this.eatFoodForSpace = eatFoodForSpace;
        this.ignoredNames = ignoredNames;
        this.names = names;
    }



}
