package net.runelite.client.plugins.projectx.util.requirements;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigDescriptor;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigItemDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.projectx.inventorysetups.InventorySetup;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds the inventory setups a script will use, from its settings, with no change to the
 * script: every visible {@code InventorySetup} setting that has a setup chosen.
 *
 * <p>A setting is skipped when a yes/no setting in the same section whose key mentions
 * "setup" (such as {@code useInventorySetup}) is switched off. Inferred setups are only
 * {@link RequirementSeverity#RECOMMENDED recommended}: the client can't tell which of several
 * setups the script's current mode uses.
 */
@Slf4j
public final class InventorySetupInference
{
    private InventorySetupInference()
    {
    }

    /** What inference needs to know about one setting. */
    public static final class Entry
    {
        public enum Kind
        {
            INVENTORY_SETUP,
            BOOLEAN,
            OTHER
        }

        final String key;
        final String section;
        final boolean hidden;
        final Kind kind;
        final Object value;

        public Entry(String key, String section, boolean hidden, Kind kind, Object value)
        {
            this.key = key == null ? "" : key;
            this.section = section == null ? "" : section;
            this.hidden = hidden;
            this.kind = kind;
            this.value = value;
        }
    }

    /** The setups to check, in settings order, each once (by name, case ignored). */
    public static List<InventorySetup> setupsToCheck(List<Entry> entries)
    {
        if (entries == null || entries.isEmpty())
        {
            return Collections.emptyList();
        }
        Set<String> switchedOffSections = new HashSet<>();
        for (Entry e : entries)
        {
            if (e.kind == Entry.Kind.BOOLEAN && e.key.toLowerCase(Locale.ROOT).contains("setup")
                && Boolean.FALSE.equals(e.value))
            {
                switchedOffSections.add(e.section);
            }
        }
        List<InventorySetup> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Entry e : entries)
        {
            if (e.kind != Entry.Kind.INVENTORY_SETUP || e.hidden || switchedOffSections.contains(e.section))
            {
                continue;
            }
            if (!(e.value instanceof InventorySetup))
            {
                continue;
            }
            InventorySetup setup = (InventorySetup) e.value;
            String name = setup.getName();
            if (name == null || name.isBlank())
            {
                continue;
            }
            if (seen.add(name.trim().toLowerCase(Locale.ROOT)))
            {
                out.add(setup);
            }
        }
        return out;
    }

    /** Recommended setup requirements for the plugin's settings; empty if it has none. */
    public static List<Requirement> fromConfig(Plugin plugin, PluginManager pluginManager, ConfigManager configManager)
    {
        if (plugin == null || pluginManager == null || configManager == null)
        {
            return Collections.emptyList();
        }
        Config config = pluginManager.getPluginConfigProxy(plugin);
        if (config == null)
        {
            return Collections.emptyList();
        }
        ConfigDescriptor descriptor = configManager.getConfigDescriptor(config);
        if (descriptor == null || descriptor.getGroup() == null || descriptor.getItems() == null)
        {
            return Collections.emptyList();
        }
        String group = descriptor.getGroup().value();
        List<Entry> entries = new ArrayList<>();
        for (ConfigItemDescriptor cid : descriptor.getItems())
        {
            ConfigItem item = cid.getItem();
            if (item == null)
            {
                continue;
            }
            Type type = cid.getType();
            Entry.Kind kind = type == InventorySetup.class ? Entry.Kind.INVENTORY_SETUP
                : type == boolean.class || type == Boolean.class ? Entry.Kind.BOOLEAN
                : Entry.Kind.OTHER;
            if (kind == Entry.Kind.OTHER)
            {
                continue;
            }
            Object value = null;
            try
            {
                if (kind == Entry.Kind.INVENTORY_SETUP)
                {
                    value = configManager.getConfiguration(group, item.keyName(), InventorySetup.class);
                }
                else
                {
                    String raw = configManager.getConfiguration(group, item.keyName());
                    value = raw == null ? null : Boolean.parseBoolean(raw);
                }
            }
            catch (RuntimeException e)
            {
                log.debug("Requirement inference: can't read {}.{}: {}", group, item.keyName(), e.getMessage());
            }
            entries.add(new Entry(item.keyName(), item.section(), item.hidden(), kind, value));
        }
        List<Requirement> out = new ArrayList<>();
        for (InventorySetup setup : setupsToCheck(entries))
        {
            out.add(Requirements.inventorySetup(setup).recommended());
        }
        return out;
    }
}
