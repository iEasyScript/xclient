package net.runelite.client.plugins.projectx.alerts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LootValueTest
{
    private static final long THRESHOLD = 100_000;

    private static LootValue.Stack stack(String name, long qty, long price)
    {
        return new LootValue.Stack(name, qty, price);
    }

    @Test
    public void totalAddsEveryStack()
    {
        List<LootValue.Stack> loot = Arrays.asList(stack("Coins", 5_000, 1), stack("Rune arrow", 100, 200), stack("Bones", 1, 0));
        assertEquals(25_000, LootValue.total(loot));
        assertEquals(0, LootValue.total(Collections.emptyList()));
    }

    @Test
    public void atOrAboveTheThresholdAlerts()
    {
        assertFalse(LootValue.isValuable(99_999, THRESHOLD));
        assertTrue("at the threshold counts", LootValue.isValuable(100_000, THRESHOLD));
        assertTrue(LootValue.isValuable(100_001, THRESHOLD));
    }

    @Test
    public void theWholeDropCountsNotOneStack()
    {
        // Two stacks of 60k each: neither alone, but the drop together does.
        List<LootValue.Stack> loot = Arrays.asList(stack("Rune platelegs", 1, 60_000), stack("Rune med helm", 3, 20_000));
        assertTrue(LootValue.isValuable(LootValue.total(loot), THRESHOLD));
    }

    @Test
    public void worthlessLootNeverAlerts()
    {
        assertFalse(LootValue.isValuable(0, 0));
        assertFalse(LootValue.isValuable(0, THRESHOLD));
        // A threshold below 1 still needs some value.
        assertTrue(LootValue.isValuable(1, 0));
        assertFalse(LootValue.isValuable(LootValue.total(Collections.singletonList(stack("Ashes", 10, 0))), 1));
    }

    @Test
    public void bigStacksDoNotOverflow()
    {
        LootValue.Stack huge = stack("Dear thing", Integer.MAX_VALUE, Long.MAX_VALUE / 2);
        assertEquals(Long.MAX_VALUE, huge.value());
        assertEquals(Long.MAX_VALUE, LootValue.total(Arrays.asList(huge, huge)));
        assertTrue(LootValue.isValuable(LootValue.total(Arrays.asList(huge, huge)), THRESHOLD));
        // A max cash stack is still worth more than the threshold.
        assertEquals(2_147_483_647L, stack("Coins", Integer.MAX_VALUE, 1).value());
    }

    @Test
    public void describeListsDearestFirstAndFoldsTheRest()
    {
        List<LootValue.Stack> loot = new ArrayList<>();
        loot.add(stack("Bones", 1, 0));
        loot.add(stack("Coins", 2_000, 1));
        loot.add(stack("Dragon bones", 2, 3_000));
        loot.add(stack("Abyssal whip", 1, 1_500_000));
        loot.add(stack("Rune arrow", 50, 200));
        loot.add(stack("Law rune", 10, 150));
        loot.add(stack("Nature rune", 4, 100));
        loot.add(stack("Air rune", 30, 5));

        String text = LootValue.describe(loot);
        String[] lines = text.split("\n");
        assertEquals(LootValue.LISTED + 1, lines.length);
        assertEquals("Abyssal whip (1,500,000 gp)", lines[0]);
        assertEquals("50 x Rune arrow (10,000 gp)", lines[1]);
        assertEquals("2 x Dragon bones (6,000 gp)", lines[2]);
        assertEquals("2,000 x Coins (2,000 gp)", lines[3]);
        assertEquals("10 x Law rune (1,500 gp)", lines[4]);
        assertFalse("worthless items are left out", text.contains("Bones ("));
        assertEquals("2 other items (550 gp)", lines[LootValue.LISTED]);
    }
}
