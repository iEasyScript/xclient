package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.util.requirements.InventorySetupInference.Entry;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InventorySetupInferenceTest
{
    private static InventorySetup setup(String name)
    {
        return new InventorySetup(List.of(), List.of(), null, null, null, new HashMap<>(),
            name, "", null, false, null, false, false, 4, false, -1);
    }

    private static Entry setupEntry(String key, String section, boolean hidden, InventorySetup value)
    {
        return new Entry(key, section, hidden, Entry.Kind.INVENTORY_SETUP, value);
    }

    private static Entry bool(String key, String section, Boolean value)
    {
        return new Entry(key, section, false, Entry.Kind.BOOLEAN, value);
    }

    private static List<String> names(List<InventorySetup> setups)
    {
        return setups.stream().map(InventorySetup::getName).collect(Collectors.toList());
    }

    @Test
    public void visibleChosenSetupsAreChecked()
    {
        List<InventorySetup> found = InventorySetupInference.setupsToCheck(List.of(
            setupEntry("inventorySetup", "", false, setup("Main")),
            setupEntry("rangedSetup", "gear", false, setup("Ranged"))));
        assertEquals(List.of("Main", "Ranged"), names(found));
    }

    @Test
    public void hiddenAndUnsetSetupsAreSkipped()
    {
        List<InventorySetup> found = InventorySetupInference.setupsToCheck(List.of(
            setupEntry("currentInventorySetup", "", true, setup("Hidden")),
            setupEntry("inventorySetup", "", false, null),
            setupEntry("blank", "", false, setup(" "))));
        assertTrue(found.isEmpty());
    }

    @Test
    public void aSwitchedOffUseSetupToggleSkipsItsSection()
    {
        List<InventorySetup> found = InventorySetupInference.setupsToCheck(List.of(
            bool("useInventorySetup", "gear", false),
            setupEntry("InventorySetupName", "gear", false, setup("Gear")),
            setupEntry("DefaultInventorySetup", "slayer", false, setup("Slayer"))));
        assertEquals(List.of("Slayer"), names(found));
    }

    @Test
    public void aSwitchedOnOrUnsetToggleKeepsTheSetup()
    {
        assertEquals(List.of("Gear"), names(InventorySetupInference.setupsToCheck(List.of(
            bool("useInventorySetup", "gear", true),
            setupEntry("inventorySetup", "gear", false, setup("Gear"))))));
        assertEquals(List.of("Gear"), names(InventorySetupInference.setupsToCheck(List.of(
            bool("useInventorySetup", "gear", null),
            setupEntry("inventorySetup", "gear", false, setup("Gear"))))));
    }

    @Test
    public void unrelatedTogglesDontSkip()
    {
        assertEquals(List.of("Gear"), names(InventorySetupInference.setupsToCheck(List.of(
            bool("bank", "gear", false),
            setupEntry("inventorySetup", "gear", false, setup("Gear"))))));
    }

    @Test
    public void theSameSetupIsCheckedOnce()
    {
        List<InventorySetup> found = InventorySetupInference.setupsToCheck(List.of(
            setupEntry("a", "", false, setup("Main")),
            setupEntry("b", "", false, setup("main"))));
        assertEquals(List.of("Main"), names(found));
    }
}
