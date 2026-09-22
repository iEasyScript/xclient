package net.runelite.client.plugins.projectx.configs;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.combat.Rs2Combat;
import net.runelite.client.plugins.projectx.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.misc.SpecialAttackWeaponEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.runelite.client.plugins.projectx.util.Global.sleep;

public class SpecialAttackConfigs {

    @Setter
    private static List<Rs2ItemModel> currentEquipment = null;

    @Getter
    private boolean useSpecialAttack = false;

    @Getter
    private SpecialAttackWeaponEnum specialAttackWeapon = null;

    @Getter
    private int minimumSpecEnergy = -1;

    public SpecialAttackConfigs setSpecialAttack(boolean useSpecialAttack) {
        this.useSpecialAttack = useSpecialAttack;
        return this;
    }

    public SpecialAttackConfigs setSpecialAttackWeapon(SpecialAttackWeaponEnum specialAttackWeapon) {
        this.specialAttackWeapon = specialAttackWeapon;
        return this;
    }

    public SpecialAttackConfigs setMinimumSpecEnergy(int minimumSpecEnergy) {
        this.minimumSpecEnergy = minimumSpecEnergy;
        return this;
    }

    public void reset() {
        setCurrentEquipment(null);
        this.setMinimumSpecEnergy(-1);
        this.setSpecialAttackWeapon(null);
        this.setSpecialAttack(false);
    }

    public boolean useSpecWeapon() {
        if (!ProjectX.isLoggedIn()) return false;
        if (!Rs2Combat.inCombat()) return false;
        if (!ProjectX.getSpecialAttackConfigs().isUseSpecialAttack()) return false;
        if (Rs2Combat.getSpecEnergy() < ProjectX.getSpecialAttackConfigs().getMinimumSpecEnergy() && currentEquipment == null) return false;

        //check spec weapon in inventory
        for (SpecialAttackWeaponEnum specialAttackWeapon :SpecialAttackWeaponEnum.values()) {
            boolean hasPresetSpecialAttackWeapon = Rs2Inventory.hasItem(specialAttackWeapon.getName())
                    && ProjectX.getSpecialAttackConfigs().getSpecialAttackWeapon() != null
                    && ProjectX.getSpecialAttackConfigs().getSpecialAttackWeapon().getName().toLowerCase().contains(specialAttackWeapon.getName());
            boolean hasRandomSpecialAttackWeapon = Rs2Inventory.hasItem(specialAttackWeapon.getName()) && ProjectX.getSpecialAttackConfigs().getSpecialAttackWeapon() == null;
            if (hasPresetSpecialAttackWeapon || hasRandomSpecialAttackWeapon) {
                if (useSpecWeapon(specialAttackWeapon.getName(), specialAttackWeapon.getEnergyRequired(), specialAttackWeapon.is2H()))
                    return true;
            }
        }
        //check spec weapon in equipment slot
        for (SpecialAttackWeaponEnum specialAttackWeapon :SpecialAttackWeaponEnum.values()) {
            boolean hasPresetSpecialAttackWeapon = Rs2Equipment.isWearing(specialAttackWeapon.getName())
                    && ProjectX.getSpecialAttackConfigs().getSpecialAttackWeapon() != null
                    && ProjectX.getSpecialAttackConfigs().getSpecialAttackWeapon().getName().toLowerCase().contains(specialAttackWeapon.getName());
            boolean hasRandomSpecialAttackWeapon = Rs2Equipment.isWearing(specialAttackWeapon.getName()) && ProjectX.getSpecialAttackConfigs().getSpecialAttackWeapon() == null;
            if (hasPresetSpecialAttackWeapon || hasRandomSpecialAttackWeapon) {
                if (useSpecWeapon(specialAttackWeapon.getName(), specialAttackWeapon.getEnergyRequired(), specialAttackWeapon.is2H()))
                    return true;
            }
        }
        return false;
    }

    public boolean useSpecWeapon(String name, int specEnergy, boolean is2H) {
        if (name.isEmpty()) return false;
        if (Rs2Equipment.isWearing(EquipmentInventorySlot.SHIELD) && is2H && Rs2Inventory.isFull()) return false;

        if (currentEquipment == null) {
            currentEquipment = new ArrayList<>();
            currentEquipment.addAll(Rs2Equipment.items());
        }

        if (Rs2Combat.getSpecEnergy() < specEnergy) {
            Rs2ItemModel rs2Item = currentEquipment.stream().filter(x -> x.getSlot() == EquipmentInventorySlot.WEAPON.getSlotIdx()).findFirst().orElse(null);
            if (rs2Item != null && rs2Item.getId() != Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getId()) {
                Rs2ItemModel weapon = currentEquipment
                        .stream()
                        .filter(x -> x.getSlot() == EquipmentInventorySlot.WEAPON.getSlotIdx())
                        .findFirst()
                        .orElse(null);
                Rs2ItemModel shield = currentEquipment
                        .stream()
                        .filter(x -> x.getSlot() == EquipmentInventorySlot.SHIELD.getSlotIdx())
                        .findFirst()
                        .orElse(null);

                if (shield != null) {
                    Rs2Inventory.wear(shield.getName());
                }
                if (weapon != null) {
                    Rs2Inventory.wear(Objects.requireNonNull(weapon).getName());
                }
            }
            return false;
        }

        boolean didInteract = Rs2Inventory.wear(name);
        if (didInteract) sleep(600);
        return Rs2Combat.setSpecState(true, specEnergy);
    }
}
