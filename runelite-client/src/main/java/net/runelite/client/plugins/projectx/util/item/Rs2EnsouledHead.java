package net.runelite.client.plugins.projectx.util.item;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.globval.enums.InterfaceTab;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.projectx.util.magic.Rs2Magic;
import net.runelite.client.plugins.projectx.util.magic.Rs2Spells;
import net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel;
import net.runelite.client.plugins.projectx.util.tabs.Rs2Tab;

import java.util.Arrays;
import java.util.Objects;

import static net.runelite.client.plugins.projectx.util.Global.sleepUntil;

@Getter
@RequiredArgsConstructor
public enum Rs2EnsouledHead {
    // Level 16 - Basic Reanimation
    GOBLIN("Ensouled goblin head", 13448, 16, Rs2Spells.BASIC_REANIMATION),
    MONKEY("Ensouled monkey head", 13451, 16, Rs2Spells.BASIC_REANIMATION),
    IMP("Ensouled imp head", 13454, 16, Rs2Spells.BASIC_REANIMATION),
    MINOTAUR("Ensouled minotaur head", 13457, 16, Rs2Spells.BASIC_REANIMATION),
    SCORPION("Ensouled scorpion head", 13460, 16, Rs2Spells.BASIC_REANIMATION),
    BEAR("Ensouled bear head", 13463, 16, Rs2Spells.BASIC_REANIMATION),
    UNICORN("Ensouled unicorn head", 13466, 16, Rs2Spells.BASIC_REANIMATION),

    // Level 41 - Adept Reanimation
    DOG("Ensouled dog head", 13469, 41, Rs2Spells.ADEPT_REANIMATION),
    CHAOS_DRUID("Ensouled chaos druid head", 13472, 41, Rs2Spells.ADEPT_REANIMATION),
    GIANT("Ensouled giant head", 13475, 41, Rs2Spells.ADEPT_REANIMATION),
    OGRE("Ensouled ogre head", 13478, 41, Rs2Spells.ADEPT_REANIMATION),
    ELF("Ensouled elf head", 13481, 41, Rs2Spells.ADEPT_REANIMATION),
    TROLL("Ensouled troll head", 13484, 41, Rs2Spells.ADEPT_REANIMATION),
    HORROR("Ensouled horror head", 13487, 41, Rs2Spells.ADEPT_REANIMATION),

    // Level 72 - Expert Reanimation
    KALPHITE("Ensouled kalphite head", 13490, 72, Rs2Spells.EXPERT_REANIMATION),
    DAGANNOTH("Ensouled dagannoth head", 13493, 72, Rs2Spells.EXPERT_REANIMATION),
    BLOODVELD("Ensouled bloodveld head", 13496, 72, Rs2Spells.EXPERT_REANIMATION),
    TZHAAR("Ensouled tzhaar head", 13499, 72, Rs2Spells.EXPERT_REANIMATION),
    DEMON("Ensouled demon head", 13502, 72, Rs2Spells.EXPERT_REANIMATION),
    HELLHOUND("Ensouled hellhound head", 26997, 72, Rs2Spells.EXPERT_REANIMATION),

    // Level 90 - Master Reanimation
    AVIANSIE("Ensouled aviansie head", 13505, 90, Rs2Spells.MASTER_REANIMATION),
    ABYSSAL("Ensouled abyssal head", 13508, 90, Rs2Spells.MASTER_REANIMATION),
    DRAGON("Ensouled dragon head", 13511, 90, Rs2Spells.MASTER_REANIMATION),
    ALL_IN_BANK("All", 1337, 0, null);

    private final String name;
    private final int ItemId;
    private final int magicLevelRequirement;
    private final Rs2Spells magicSpell;

    public boolean hasRequirements() {
        return Rs2Magic.canCast(magicSpell);
    }

    public Rs2ItemModel getInventoryHead() {
        return Rs2Inventory.get(getItemId());
    }

    /**
     * Reanimates the ensouled head found in inventory
     *
     * @return
     */
    public boolean reanimate() {
        Rs2ItemModel head = getInventoryHead();
        if (head == null || !Rs2Magic.cast(magicSpell)) {
            return false;
        }
        sleepUntil(() -> ProjectX.getClientThread().runOnClientThreadOptional(() -> Rs2Tab.getCurrentTab() == InterfaceTab.INVENTORY).orElse(false), 5000);
        return Rs2Inventory.interact(head, "Reanimate");
    }

    public static Rs2EnsouledHead getReanimatableHead() {
        return Rs2Inventory.items().map(Rs2EnsouledHead::forItem).filter(Objects::nonNull).filter(Rs2EnsouledHead::hasRequirements).findFirst().orElse(null);
    }

    public static Rs2EnsouledHead forItem(Rs2ItemModel item) {
        if (item == null) return null;
        int itemId = item.getId();
        return Arrays.stream(values()).filter(e -> e.getItemId() == itemId).findFirst().orElse(null);
    }

    public static boolean isNpcReanimated(Rs2NpcModel npc) {
        if (npc == null) return false;
        String name = npc.getName();
        if (name == null) return false;
        return name.contains("Reanimated");
    }

}
