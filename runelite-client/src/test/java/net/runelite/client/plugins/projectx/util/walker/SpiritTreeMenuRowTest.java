package net.runelite.client.plugins.projectx.util.walker;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SpiritTreeMenuRowTest {
    /** The new menu interface (947): keys coloured ffffff, unusable rows greyed. */
    private static final List<String> NEW_MENU = Arrays.asList(
            "<col=ffffff>1</col>: Tree Gnome Village",
            "<col=ffffff>2</col>: Gnome Stronghold",
            "<col=ffffff>3</col>: Battlefield of Khazard",
            "<col=ffffff>4</col>: Grand Exchange",
            "<col=ffffff>5</col>: Feldip Hills",
            "<col=ffffff>6</col>: <col=5f5f5f>Prifddinas",
            "<col=ffffff>7</col>: <col=5f5f5f>Port Sarim",
            "<col=ffffff>8</col>: Hosidius",
            "<col=ffffff>9</col>: Farming Guild",
            "<col=ffffff>A</col>: Poison Waste",
            "<col=ffffff>B</col>: Cancel");
    /** The classic menu interface (187): keys coloured 735a28. */
    private static final List<String> CLASSIC_MENU = Arrays.asList(
            "<col=735a28>1</col>: Tree Gnome Village",
            "<col=735a28>2</col>: Gnome Stronghold",
            "<col=735a28>3</col>: Battlefield of Khazard",
            "<col=735a28>4</col>: Grand Exchange",
            "<col=735a28>5</col>: Feldip Hills");

    @Test
    public void picksRowsByNameInBothMenus() {
        assertEquals(3, Rs2Walker.listMenuRowIndex("4: Grand Exchange", NEW_MENU));
        assertEquals(1, Rs2Walker.listMenuRowIndex("2: Gnome Stronghold", NEW_MENU));
        assertEquals(3, Rs2Walker.listMenuRowIndex("4: Grand Exchange", CLASSIC_MENU));
        assertEquals(0, Rs2Walker.listMenuRowIndex("1: Tree Gnome Village", CLASSIC_MENU));
    }

    @Test
    public void ignoresTheKeyInTheTsv() {
        // spirit_trees.tsv says "A: Hosidius" and "D: Poison Waste"; this menu shows them under 8 and A.
        assertEquals(7, Rs2Walker.listMenuRowIndex("A: Hosidius", NEW_MENU));
        assertEquals(9, Rs2Walker.listMenuRowIndex("D: Poison Waste", NEW_MENU));
    }

    @Test
    public void greyedOrMissingRowIsNotOffered() {
        assertEquals(-1, Rs2Walker.listMenuRowIndex("6: Prifddinas", NEW_MENU));
        assertEquals(-1, Rs2Walker.listMenuRowIndex("7: Port Sarim", NEW_MENU));
        assertEquals(-1, Rs2Walker.listMenuRowIndex("8: Etceteria", NEW_MENU));
        assertEquals(-1, Rs2Walker.listMenuRowIndex("B: Farming Guild", CLASSIC_MENU));
        assertEquals(-1, Rs2Walker.listMenuRowIndex("", NEW_MENU));
    }

    @Test
    public void readsTheRowsOwnKeyAndName() {
        assertEquals('A', Rs2Walker.listMenuRowKey("<col=ffffff>A</col>: Poison Waste"));
        assertEquals('4', Rs2Walker.listMenuRowKey("<col=735a28>4</col>: Grand Exchange"));
        assertEquals(0, Rs2Walker.listMenuRowKey("Spirit Tree Locations"));
        assertEquals("Prifddinas", Rs2Walker.listMenuRowName("<col=ffffff>6</col>: <col=5f5f5f>Prifddinas"));
    }
}
