package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;
import net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin;
import net.runelite.client.plugins.projectx.util.magic.Runes;
import net.runelite.client.plugins.projectx.util.magic.Spell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Ready-made {@link Requirement}s. All are required; call {@link Requirement#recommended()}
 * on one that is only recommended.
 *
 * <pre>{@code
 * List.of(
 *     Requirements.skill(Skill.MAGIC, 55),
 *     Requirements.questCompleted(Quest.LUNAR_DIPLOMACY),
 *     Requirements.item("Rune pouch", 1, ItemSource.ANYWHERE, ItemID.BH_RUNE_POUCH),
 *     Requirements.runes(Map.of(Runes.ASTRAL, 100, Runes.NATURE, 100), true),
 *     Requirements.membersWorld(),
 *     Requirements.inventorySetup(config.inventorySetup()).recommended());
 * }</pre>
 */
public final class Requirements
{
    static final String LOG_IN = "Log in to check";
    static final String OPEN_BANK = "Bank unknown: open your bank once";

    private static final String[] SPELLBOOKS = {"Standard", "Ancient", "Lunar", "Arceuus"};

    private Requirements()
    {
    }

    // ---- skills and quests

    /** The real (unboosted) level in a skill is at least {@code level}. */
    public static Requirement skill(Skill skill, int level)
    {
        Objects.requireNonNull(skill, "skill");
        String name = skill.getName();
        return Requirement.of(name + " " + level, ctx ->
        {
            if (!ctx.isLoggedIn())
            {
                return RequirementResult.unknown(LOG_IN);
            }
            int have = ctx.realLevel(skill);
            if (have >= level)
            {
                return RequirementResult.met(name + " " + have);
            }
            return RequirementResult.unmet(name + " " + level + " needed, you have " + have);
        }).withKey("skill:" + skill.name() + ":" + level);
    }

    /** The quest is completed. */
    public static Requirement questCompleted(Quest quest)
    {
        Objects.requireNonNull(quest, "quest");
        return Requirement.of(quest.getName(), ctx ->
        {
            if (!ctx.isLoggedIn())
            {
                return RequirementResult.unknown(LOG_IN);
            }
            QuestState state = ctx.questState(quest);
            if (state == null)
            {
                return RequirementResult.unknown("Couldn't read the quest");
            }
            switch (state)
            {
                case FINISHED:
                    return RequirementResult.met("Completed");
                case IN_PROGRESS:
                    return RequirementResult.unmet("In progress: finish the quest first");
                default:
                    return RequirementResult.unmet("Not started");
            }
        }).withKey("quest:" + quest.name());
    }

    /** The quest is at least started (in progress or completed). */
    public static Requirement questStarted(Quest quest)
    {
        Objects.requireNonNull(quest, "quest");
        return Requirement.of(quest.getName() + " (started)", ctx ->
        {
            if (!ctx.isLoggedIn())
            {
                return RequirementResult.unknown(LOG_IN);
            }
            QuestState state = ctx.questState(quest);
            if (state == null)
            {
                return RequirementResult.unknown("Couldn't read the quest");
            }
            return state == QuestState.NOT_STARTED
                ? RequirementResult.unmet("Not started")
                : RequirementResult.met(state == QuestState.FINISHED ? "Completed" : "Started");
        }).withKey("queststarted:" + quest.name());
    }

    // ---- world

    /** The player is on a members world. */
    public static Requirement membersWorld()
    {
        return Requirement.of("Members world", ctx ->
        {
            if (!ctx.isLoggedIn())
            {
                return RequirementResult.unknown(LOG_IN);
            }
            return ctx.isMembersWorld()
                ? RequirementResult.met("You're on a members world")
                : RequirementResult.unmet("Hop to a members world");
        }).withKey("membersworld");
    }

    // ---- items

    /**
     * At least {@code quantity} of any of {@code itemIds}, counted across {@code where}.
     *
     * @param label what the player reads, e.g. "Rune pouch"
     */
    public static Requirement item(String label, int quantity, Set<ItemSource> where, int... itemIds)
    {
        Objects.requireNonNull(itemIds, "itemIds");
        int[] ids = itemIds.clone();
        Predicate<HeldItem> matches = held -> Arrays.stream(ids).anyMatch(id -> id == held.getId());
        return items(label, quantity, where, matches)
            .withKey("item:" + Arrays.toString(ids) + ":" + quantity + ":" + where);
    }

    /** At least {@code quantity} of any of {@code itemIds}, anywhere (inventory, worn or bank). */
    public static Requirement item(String label, int quantity, int... itemIds)
    {
        return item(label, quantity, ItemSource.ANYWHERE, itemIds);
    }

    /** At least {@code quantity} of the item with this exact name (case ignored), counted across {@code where}. */
    public static Requirement itemNamed(String name, int quantity, Set<ItemSource> where)
    {
        Objects.requireNonNull(name, "name");
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return items(name, quantity, where, held -> held.getName().trim().toLowerCase(Locale.ROOT).equals(wanted))
            .withKey("itemnamed:" + wanted + ":" + quantity + ":" + where);
    }

    /** At least {@code quantity} of the item with this exact name (case ignored), anywhere. */
    public static Requirement itemNamed(String name, int quantity)
    {
        return itemNamed(name, quantity, ItemSource.ANYWHERE);
    }

    private static Requirement items(String label, int quantity, Set<ItemSource> where, Predicate<HeldItem> matches)
    {
        Objects.requireNonNull(label, "label");
        Set<ItemSource> sources = where == null || where.isEmpty()
            ? ItemSource.ANYWHERE
            : Collections.unmodifiableSet(EnumSet.copyOf(where));
        int need = Math.max(1, quantity);
        String description = label + (need > 1 ? " x" + need : "") + whereSuffix(sources);
        return Requirement.of(description, ctx ->
        {
            if (!ctx.isLoggedIn())
            {
                return RequirementResult.unknown(LOG_IN);
            }
            long have = 0;
            if (sources.contains(ItemSource.INVENTORY))
            {
                have += count(ctx.inventory(), matches);
            }
            if (sources.contains(ItemSource.EQUIPMENT))
            {
                have += count(ctx.equipment(), matches);
            }
            boolean bankUnknown = false;
            if (sources.contains(ItemSource.BANK))
            {
                Optional<List<HeldItem>> bank = ctx.bank();
                if (bank.isPresent())
                {
                    have += count(bank.get(), matches);
                }
                else
                {
                    bankUnknown = true;
                }
            }
            if (have >= need)
            {
                return RequirementResult.met("You have " + have);
            }
            if (bankUnknown)
            {
                return RequirementResult.unknown((have == 0 ? "Not carried" : "You carry " + have + " of " + need)
                    + ". " + OPEN_BANK);
            }
            return RequirementResult.unmet(have == 0 ? "You have none" : "Need " + need + ", you have " + have);
        });
    }

    private static String whereSuffix(Set<ItemSource> sources)
    {
        if (sources.equals(EnumSet.of(ItemSource.INVENTORY)))
        {
            return " in inventory";
        }
        if (sources.equals(EnumSet.of(ItemSource.EQUIPMENT)))
        {
            return " worn";
        }
        if (sources.equals(ItemSource.CARRIED))
        {
            return " carried";
        }
        if (sources.equals(EnumSet.of(ItemSource.BANK)))
        {
            return " in bank";
        }
        return "";
    }

    static long count(List<HeldItem> items, Predicate<HeldItem> matches)
    {
        if (items == null)
        {
            return 0;
        }
        long total = 0;
        for (HeldItem item : items)
        {
            if (item != null && matches.test(item))
            {
                total += Math.max(0, item.getQuantity());
            }
        }
        return total;
    }

    // ---- runes

    /**
     * Enough runes: counts the inventory, rune pouch, staves and tomes, and combination
     * runes towards their base runes (as {@code Rs2Magic.getRunes} does), plus the bank
     * when {@code includeBank}.
     */
    public static Requirement runes(Map<Runes, Integer> needed, boolean includeBank)
    {
        Objects.requireNonNull(needed, "needed");
        Map<Runes, Integer> want = new LinkedHashMap<>();
        needed.forEach((rune, qty) ->
        {
            if (rune != null && qty != null && qty > 0)
            {
                want.merge(rune, qty, Integer::sum);
            }
        });
        String description = "Runes: " + describeRunes(want);
        return runes(description, want, includeBank);
    }

    /** Enough runes to cast {@code spell} {@code casts} times. */
    public static Requirement runesFor(Spell spell, int casts, boolean includeBank)
    {
        Objects.requireNonNull(spell, "spell");
        Map<Runes, Integer> want = new LinkedHashMap<>(spell.getRequiredRunes(Math.max(1, casts)));
        String spellName = spell.getMagicAction() != null ? spell.getMagicAction().getName() : String.valueOf(spell);
        return runes("Runes for " + (casts > 1 ? casts + " x " : "") + spellName, want, includeBank);
    }

    private static Requirement runes(String description, Map<Runes, Integer> want, boolean includeBank)
    {
        return Requirement.of(description, ctx ->
        {
            if (!ctx.isLoggedIn())
            {
                return RequirementResult.unknown(LOG_IN);
            }
            if (want.isEmpty())
            {
                return RequirementResult.met("No runes needed");
            }
            boolean bankKnown = includeBank && ctx.bank().isPresent();
            Map<Runes, Integer> missing = missingRunes(want, ctx.availableRunes(bankKnown));
            if (missing.isEmpty())
            {
                return RequirementResult.met("You have enough");
            }
            if (includeBank && !bankKnown)
            {
                return RequirementResult.unknown("Short of " + describeRunes(missing) + " on you. " + OPEN_BANK);
            }
            return RequirementResult.unmet("Missing " + describeRunes(missing));
        }).withKey("runes:" + want + ":" + includeBank);
    }

    /** What is still needed after {@code available}; empty when there is enough of everything. */
    static Map<Runes, Integer> missingRunes(Map<Runes, Integer> want, Map<Runes, Integer> available)
    {
        Map<Runes, Integer> missing = new LinkedHashMap<>();
        want.forEach((rune, qty) ->
        {
            int have = available == null ? 0 : available.getOrDefault(rune, 0);
            if (have < qty)
            {
                missing.put(rune, qty - have);
            }
        });
        return missing;
    }

    static String describeRunes(Map<Runes, Integer> runes)
    {
        return runes.entrySet().stream()
            .map(e -> e.getValue() + " " + runeName(e.getKey()))
            .collect(Collectors.joining(", "));
    }

    private static String runeName(Runes rune)
    {
        String n = rune.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    // ---- inventory setups

    /**
     * Everything in the setup is somewhere the script can get it from: inventory, worn,
     * rune pouch, or the bank. See {@link InventorySetupDiff} for the rules.
     */
    public static Requirement inventorySetup(InventorySetup setup)
    {
        if (setup == null || setup.getName() == null)
        {
            return Requirement.of("Inventory setup", ctx -> RequirementResult.unmet("No inventory setup selected in the script's settings"))
                .withKey("setup:");
        }
        return Requirement.of("Inventory setup \"" + setup.getName() + "\"", ctx -> checkSetup(setup, ctx))
            .withKey(setupKey(setup.getName()));
    }

    /** As {@link #inventorySetup(InventorySetup)}, finding the setup by name (case ignored) when checked. */
    public static Requirement inventorySetup(String setupName)
    {
        Objects.requireNonNull(setupName, "setupName");
        return Requirement.of("Inventory setup \"" + setupName + "\"", ctx ->
        {
            List<InventorySetup> all = MInventorySetupsPlugin.getInventorySetups();
            InventorySetup found = all == null ? null : all.stream()
                .filter(Objects::nonNull)
                .filter(s -> s.getName() != null && s.getName().equalsIgnoreCase(setupName))
                .findFirst()
                .orElse(null);
            if (found == null)
            {
                return RequirementResult.unmet("There is no inventory setup called \"" + setupName + "\"");
            }
            return checkSetup(found, ctx);
        }).withKey(setupKey(setupName));
    }

    static String setupKey(String setupName)
    {
        return "setup:" + setupName.trim().toLowerCase(Locale.ROOT);
    }

    private static RequirementResult checkSetup(InventorySetup setup, RequirementContext ctx)
    {
        if (!ctx.isLoggedIn())
        {
            return RequirementResult.unknown(LOG_IN);
        }
        return InventorySetupDiff.compute(setup, ctx.inventory(), ctx.equipment(), ctx.bank().orElse(null),
            ctx.runePouch(), ctx.spellbook()).toResult();
    }

    static String spellbookName(int value)
    {
        return value >= 0 && value < SPELLBOOKS.length ? SPELLBOOKS[value] : "unknown";
    }

    /** For tests and callers that build lists: drops nulls. */
    static List<Requirement> nonNull(List<Requirement> list)
    {
        List<Requirement> out = new ArrayList<>();
        if (list != null)
        {
            for (Requirement r : list)
            {
                if (r != null)
                {
                    out.add(r);
                }
            }
        }
        return out;
    }
}
