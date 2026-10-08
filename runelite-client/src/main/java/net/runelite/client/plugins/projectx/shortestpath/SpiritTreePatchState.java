/*
 * Adapted from Skretzo/shortest-path (SpiritTreePatchState, BSD 2-Clause).
 * Copyright (c) Skretzo and the shortest-path contributors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.projectx.shortestpath;

import net.runelite.api.Client;
import net.runelite.api.HashTable;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which planted spirit trees this account can travel to.
 *
 * <p>A planted tree is usable only once it is fully grown and healthy, and the game
 * only says so through a farming varbit that means that patch only while the player
 * stands in the patch's region. Three sources feed it: that varbit, sampled once the
 * region has settled; the travel menu of any spirit tree, which lists every planted
 * tree and greys out the unusable ones; and what was last seen, saved per account.
 *
 * <p>Nothing observed means nothing known, and the walker then leaves planted trees
 * out rather than send a player to one they may not have.
 */
@Singleton
public class SpiritTreePatchState
{
    private static final String CONFIG_KEY_PREFIX = "spiritTree.";

    /** The varbit value of a grown, healthy tree; anything else is not travelable. */
    private static final int GROWN = 20;

    // patch -> {regionID, varbitID, x1, y1, x2, y2}; bounds stay inside the region.
    private static final Map<String, int[]> PATCHES;
    private static final Map<Integer, String> PATCH_BY_REGION;

    static
    {
        Map<String, int[]> patches = new LinkedHashMap<>();
        patches.put("Port Sarim", new int[]{12082, VarbitID.FARMING_TRANSMIT_A, 3058, 3256, 3062, 3260});
        patches.put("Etceteria", new int[]{10300, VarbitID.FARMING_TRANSMIT_B, 2611, 3855, 2615, 3860});
        patches.put("Brimhaven", new int[]{11058, VarbitID.FARMING_TRANSMIT_B, 2800, 3201, 2804, 3205});
        patches.put("Hosidius", new int[]{6711, VarbitID.FARMING_TRANSMIT_F, 1691, 3540, 1695, 3544});
        patches.put("Farming Guild", new int[]{4922, VarbitID.FARMING_TRANSMIT_A, 1251, 3748, 1255, 3752});
        PATCHES = Collections.unmodifiableMap(patches);

        Map<Integer, String> byRegion = new HashMap<>();
        for (Map.Entry<String, int[]> entry : patches.entrySet())
        {
            byRegion.put(entry.getValue()[0], entry.getKey());
        }
        PATCH_BY_REGION = Collections.unmodifiableMap(byRegion);
    }

    /** A travel-menu row: "<col=..>1</col>: Tree Gnome Village", greyed rows carry a 5f5f5f colour. */
    private static final Pattern MENU_ROW = Pattern.compile("<col=735a28>(.+)</col>: (<col=5f5f5f>)?(.+)");
    private static final Pattern MENU_ROW_NEW = Pattern.compile("<col=ffffff>(.+)</col>: (<col=5f5f5f>)?(.+)");

    private final ConfigManager configManager;
    private final Map<String, Integer> observed = new HashMap<>();
    private final Map<String, Integer> persisted = new HashMap<>();
    private boolean dirty;

    private int lastRegion = -1;
    private int lastRegionTick = -1;
    private int settledRegion = -1;

    @Inject
    public SpiritTreePatchState(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

    SpiritTreePatchState()
    {
        this(null);
    }

    public static boolean travelable(int varbitValue)
    {
        return varbitValue == GROWN;
    }

    public static Set<String> patchNames()
    {
        return PATCHES.keySet();
    }

    public static String patchNameForRegion(int regionId)
    {
        return PATCH_BY_REGION.get(regionId);
    }

    public static int varbitForPatch(String patch)
    {
        int[] entry = PATCHES.get(patch);
        return entry == null ? -1 : entry[1];
    }

    /** The planted patch whose tiles include (x, y), or null. */
    public static String patchNameForTile(int x, int y)
    {
        for (Map.Entry<String, int[]> entry : PATCHES.entrySet())
        {
            int[] p = entry.getValue();
            if (x >= p[2] && x <= p[4] && y >= p[3] && y <= p[5])
            {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Varbits are not sent while a modal interface is open, so a read then may be stale. */
    public static boolean modalWidgetOpen(Client client)
    {
        HashTable<WidgetNode> table = client.getComponentTable();
        if (table == null)
        {
            return false;
        }
        for (WidgetNode node : table)
        {
            if (node.getModalMode() != WidgetModalMode.NON_MODAL)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Called each tick with the player's region. A region counts as settled from the
     * second consecutive tick in it: on the entry tick the shared varbit can still
     * hold the previous region's patch.
     */
    public void notePlayerRegion(int region, int tick)
    {
        settledRegion = region != -1 && region == lastRegion && tick == lastRegionTick + 1 ? region : -1;
        lastRegion = region;
        lastRegionTick = tick;
    }

    public boolean isRegionSettled(int region)
    {
        return region != -1 && settledRegion == region;
    }

    /** @return true when the set of travelable trees changed */
    public boolean applyVarbitSample(String patch, int varbitValue)
    {
        if (!PATCHES.containsKey(patch))
        {
            return false;
        }
        boolean before = isTravelable(patch);
        Integer previous = observed.get(patch);
        if (previous == null || previous != varbitValue)
        {
            observed.put(patch, varbitValue);
            dirty = true;
        }
        return isTravelable(patch) != before;
    }

    /**
     * Reads an open spirit tree travel menu. Authoritative for the trees it lists;
     * trees it does not list keep what was known.
     *
     * @return true when the set of travelable trees changed
     */
    public boolean applyMenu(Widget[] rows, boolean newMenu)
    {
        if (rows == null || rows.length == 0)
        {
            return false;
        }
        // Tree Gnome Village is always the first row. The menu interface is shared with
        // other dialogs, so anything else is not a spirit tree menu.
        String first = (newMenu ? "<col=ffffff>1</col>: " : "<col=735a28>1</col>: ") + "Tree Gnome Village";
        if (!first.equals(rows[0].getText()))
        {
            return false;
        }
        Pattern pattern = newMenu ? MENU_ROW_NEW : MENU_ROW;
        boolean changed = false;
        for (Widget row : rows)
        {
            String text = row.getText();
            if (text == null)
            {
                continue;
            }
            Matcher m = pattern.matcher(text);
            if (!m.matches())
            {
                continue;
            }
            String name = Text.removeTags(m.group(3)).trim();
            if (PATCHES.containsKey(name))
            {
                changed |= applyVarbitSample(name, m.group(2) == null ? GROWN : 0);
            }
        }
        return changed;
    }

    /** Trees known to be grown and usable; null while nothing has been observed at all. */
    public Set<String> getTravelableTreesOrNull()
    {
        if (observed.isEmpty())
        {
            return null;
        }
        Set<String> trees = new HashSet<>();
        for (Map.Entry<String, Integer> e : observed.entrySet())
        {
            if (travelable(e.getValue()))
            {
                trees.add(e.getKey());
            }
        }
        return trees;
    }

    /** Loads what was last seen for the logged-in account. */
    public void loadFromProfile()
    {
        dirty = false;
        observed.clear();
        persisted.clear();
        if (configManager == null)
        {
            return;
        }
        for (String patch : PATCHES.keySet())
        {
            Integer value = parseStored(configManager.getRSProfileConfiguration(ShortestPathPlugin.CONFIG_GROUP, configKey(patch)));
            if (value != null)
            {
                observed.put(patch, value);
                persisted.put(patch, value);
            }
        }
    }

    /** Saves what changed. Only grown trees are kept; an unusable one removes its entry. */
    public void persistIfDirty()
    {
        if (!dirty || configManager == null)
        {
            dirty = false;
            return;
        }
        dirty = false;
        long now = System.currentTimeMillis() / 1000L;
        for (Map.Entry<String, Integer> e : observed.entrySet())
        {
            Integer last = persisted.get(e.getKey());
            if (travelable(e.getValue()))
            {
                if (!e.getValue().equals(last))
                {
                    configManager.setRSProfileConfiguration(ShortestPathPlugin.CONFIG_GROUP, configKey(e.getKey()), e.getValue() + ":" + now);
                    persisted.put(e.getKey(), e.getValue());
                }
            }
            else if (last != null)
            {
                configManager.unsetRSProfileConfiguration(ShortestPathPlugin.CONFIG_GROUP, configKey(e.getKey()));
                persisted.remove(e.getKey());
            }
        }
    }

    private boolean isTravelable(String patch)
    {
        Integer value = observed.get(patch);
        return value != null && travelable(value);
    }

    static String configKey(String patch)
    {
        int[] entry = PATCHES.get(patch);
        return entry == null ? null : CONFIG_KEY_PREFIX + entry[0] + "." + entry[1];
    }

    /** "value:unixSeconds" -> value, or null for anything else. */
    static Integer parseStored(String stored)
    {
        if (stored == null)
        {
            return null;
        }
        int split = stored.indexOf(':');
        if (split <= 0 || split == stored.length() - 1 || stored.indexOf(':', split + 1) >= 0)
        {
            return null;
        }
        try
        {
            Long.parseLong(stored.substring(split + 1));
            return Integer.parseInt(stored.substring(0, split));
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
