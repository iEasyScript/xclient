package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import com.google.common.base.Strings;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.util.Text;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility helpers for mapping BreakHandler V2 "Stop plugin" selections to actual plugins.
 */
public final class PluginStopHelper {

    private PluginStopHelper() {
    }

    /**
     * Normalises a stored config value to a fully qualified class name or {@link PluginStopOption#NONE_VALUE}.
     * Accepts legacy enum names and display names from {@link ProjectXPluginChoice}.
     */
    public static String normalizeStoredValue(String rawValue, PluginManager pluginManager) {
        if (Strings.isNullOrEmpty(rawValue)) {
            return PluginStopOption.NONE_VALUE;
        }

        Optional<ProjectXPluginChoice> builtIn = ProjectXPluginChoice.fromConfigValue(rawValue);
        if (builtIn.isPresent()) {
            String className = builtIn.get().getClassName();
            return className == null ? PluginStopOption.NONE_VALUE : className;
        }

        String sanitizedRaw = sanitize(rawValue);

        if (pluginManager != null) {
            for (PluginStopOption option : buildOptions(pluginManager)) {
                if (sanitize(option.getDisplayName()).equalsIgnoreCase(sanitizedRaw)) {
                    return option.getClassName();
                }
            }

            for (Plugin plugin : pluginManager.getPlugins()) {
                PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                String className = plugin.getClass().getName();
                String simple = plugin.getClass().getSimpleName();
                String descriptorName = descriptor != null ? sanitize(descriptor.name()) : "";

                if (sanitizedRaw.equalsIgnoreCase(simple)
                        || (!descriptorName.isEmpty() && sanitizedRaw.equalsIgnoreCase(descriptorName))) {
                    return className;
                }
            }
        }

        return rawValue;
    }

    /**
     * Backwards-compatible normalizer that doesn't have PluginManager context.
     */
    public static String normalizeStoredValue(String rawValue) {
        return normalizeStoredValue(rawValue, null);
    }

    public static boolean isNone(String normalizedValue) {
        return Strings.isNullOrEmpty(normalizedValue)
                || PluginStopOption.NONE_VALUE.equalsIgnoreCase(normalizedValue);
    }

    /**
     * Builds the combined list of built-in ProjectX plugins and installed external (Plugin Hub) plugins.
     */
    public static List<PluginStopOption> buildOptions(PluginManager pluginManager) {
        List<PluginStopOption> options = new ArrayList<>();
        options.add(PluginStopOption.none());

        for (ProjectXPluginChoice choice : ProjectXPluginChoice.values()) {
            if (choice == ProjectXPluginChoice.NONE || choice.getClassName() == null) {
                continue;
            }
            options.add(PluginStopOption.builtIn(choice.toString(), choice.getClassName()));
        }

        if (pluginManager != null) {
            List<PluginStopOption> external = pluginManager.getPlugins().stream()
                    .filter(p -> {
                        PluginDescriptor descriptor = p.getClass().getAnnotation(PluginDescriptor.class);
                        return descriptor != null && descriptor.isExternal() && !descriptor.hidden();
                    })
                    .map(p -> {
                        PluginDescriptor descriptor = p.getClass().getAnnotation(PluginDescriptor.class);
                        String rawName = descriptor != null && !Strings.isNullOrEmpty(descriptor.name())
                                ? descriptor.name()
                                : p.getClass().getSimpleName();
                        String displayName = sanitize(rawName);
                        return PluginStopOption.external(displayName, p.getClass().getName());
                    })
                    .sorted(Comparator.comparing(PluginStopOption::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            options.addAll(external);
        }

        // Deduplicate by class name while preserving order (built-ins first, then external).
        Map<String, PluginStopOption> deduped = options.stream()
                .collect(Collectors.toMap(
                        PluginStopOption::getClassName,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));

        return new ArrayList<>(deduped.values());
    }

    /**
     * Resolves a friendly display name for the stored config value.
     */
    public static String resolveDisplayName(String rawValue, PluginManager pluginManager) {
        String normalized = normalizeStoredValue(rawValue, pluginManager);

        if (isNone(normalized)) {
            return ProjectXPluginChoice.NONE.toString();
        }

        Optional<ProjectXPluginChoice> builtIn = ProjectXPluginChoice.fromConfigValue(normalized);
        if (builtIn.isPresent()) {
            return sanitize(builtIn.get().toString());
        }

        if (pluginManager != null) {
            for (Plugin plugin : pluginManager.getPlugins()) {
                if (plugin.getClass().getName().equals(normalized)) {
                    PluginDescriptor descriptor = plugin.getClass().getAnnotation(PluginDescriptor.class);
                    if (descriptor != null && !Strings.isNullOrEmpty(descriptor.name())) {
                        return sanitize(descriptor.name());
                    }
                    return sanitize(plugin.getClass().getSimpleName());
                }
            }
        }

        return sanitize(normalized);
    }

    public static String sanitize(String value) {
        return Text.removeTags(Strings.nullToEmpty(value)).trim();
    }

    /**
     * Attempts to find a plugin instance using a normalized class name or a raw/sanitized label.
     */
    public static Plugin findPluginInstance(String normalizedClassName, String rawValue, PluginManager pluginManager) {
        if (pluginManager == null) {
            return null;
        }

        String sanitizedRaw = sanitize(rawValue);

        for (Plugin plugin : pluginManager.getPlugins()) {
            Class<?> clazz = plugin.getClass();
            PluginDescriptor descriptor = clazz.getAnnotation(PluginDescriptor.class);
            String className = clazz.getName();
            String simpleName = clazz.getSimpleName();
            String descriptorName = descriptor != null ? sanitize(descriptor.name()) : "";

            if (className.equals(normalizedClassName)
                    || className.equalsIgnoreCase(rawValue)
                    || simpleName.equalsIgnoreCase(normalizedClassName)
                    || simpleName.equalsIgnoreCase(sanitizedRaw)
                    || (!descriptorName.isEmpty() && descriptorName.equalsIgnoreCase(sanitizedRaw))) {
                return plugin;
            }
        }

        return null;
    }
}
