package net.runelite.client.plugins.projectx.util.antiban;

import lombok.Getter;
import net.runelite.client.plugins.projectx.util.antiban.enums.Activity;

import java.util.function.Consumer;

/**
 * The setups the panel offers in its preset picker: three general strengths, the universal setup, and the
 * per-activity templates scripts already use, which until now had no way in from the panel.
 *
 * <p>Slayer is missing on purpose: its template is empty.
 */
public enum AntibanPreset {
    LIGHT("Light", false, AntibanSetupTemplates::applyLightSetup),
    BALANCED("Balanced", false, AntibanSetupTemplates::applyBalancedSetup),
    CAUTIOUS("Cautious", false, AntibanSetupTemplates::applyCautiousSetup),
    MOUSE_ONLY("Mouse only", false, AntibanSetupTemplates::applyGeneralBasicSetup),
    UNIVERSAL("Universal", false, AntibanSetupTemplates::applyUniversalAntibanSetup),
    AGILITY("Agility", true, AntibanSetupTemplates::applyAgilitySetup),
    COMBAT("Combat", true, AntibanSetupTemplates::applyCombatSetup),
    CONSTRUCTION("Construction", true, AntibanSetupTemplates::applyConstructionSetup),
    COOKING("Cooking", true, AntibanSetupTemplates::applyCookingSetup),
    CRAFTING("Crafting", true, AntibanSetupTemplates::applyCraftingSetup),
    FARMING("Farming", true, AntibanSetupTemplates::applyFarmingSetup),
    FIREMAKING("Firemaking", true, AntibanSetupTemplates::applyFiremakingSetup),
    FISHING("Fishing", true, AntibanSetupTemplates::applyFishingSetup),
    FLETCHING("Fletching", true, AntibanSetupTemplates::applyFletchingSetup),
    HERBLORE("Herblore", true, AntibanSetupTemplates::applyHerbloreSetup),
    HUNTER("Hunter", true, AntibanSetupTemplates::applyHunterSetup),
    MINING("Mining", true, AntibanSetupTemplates::applyMiningSetup),
    RUNECRAFTING("Runecrafting", true, AntibanSetupTemplates::applyRunecraftingSetup),
    SMITHING("Smithing", true, AntibanSetupTemplates::applySmithingSetup),
    THIEVING("Thieving", true, AntibanSetupTemplates::applyThievingSetup),
    WOODCUTTING("Woodcutting", true, AntibanSetupTemplates::applyWoodcuttingSetup);

    @Getter
    private final String displayName;

    /** Activity templates are tuned for one skill; the picker labels them so they read as that. */
    @Getter
    private final boolean activityTemplate;

    private final Consumer<AntibanSetupTemplates> setup;

    AntibanPreset(String displayName, boolean activityTemplate, Consumer<AntibanSetupTemplates> setup) {
        this.displayName = displayName;
        this.activityTemplate = activityTemplate;
        this.setup = setup;
    }

    /**
     * Applies the preset to the live settings.
     *
     * <p>The templates were written for scripts, so most of them also switch off universal antiban, dev debug
     * and dynamic activity and intensity. Picked from the panel those are the user's system-level choices, and
     * only the universal setup -- whose whole point is to set them -- is allowed to change them.
     *
     * <p>The activity templates also set an activity. That would change what a running script is doing, so
     * the activity in place beforehand is put back; with none in place the template's activity stays, which
     * gives universal antiban something to work with.
     */
    public void apply() {
        boolean devDebug = Rs2AntibanSettings.devDebug;
        boolean universalAntiban = Rs2AntibanSettings.universalAntiban;
        boolean dynamicActivity = Rs2AntibanSettings.dynamicActivity;
        boolean dynamicIntensity = Rs2AntibanSettings.dynamicIntensity;
        Activity previousActivity = Rs2Antiban.getActivity();

        setup.accept(Rs2Antiban.antibanSetupTemplates);

        if (this != UNIVERSAL) {
            Rs2AntibanSettings.devDebug = devDebug;
            Rs2AntibanSettings.universalAntiban = universalAntiban;
            Rs2AntibanSettings.dynamicActivity = dynamicActivity;
            Rs2AntibanSettings.dynamicIntensity = dynamicIntensity;
        }
        if (activityTemplate && previousActivity != null) {
            Rs2Antiban.setActivity(previousActivity);
        }
    }

    @Override
    public String toString() {
        return activityTemplate ? "Tuned for " + displayName : displayName;
    }
}
