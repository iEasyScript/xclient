package net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2;

import lombok.Getter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.projectx.accountselector.AutoLoginPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin;
import net.runelite.client.plugins.projectx.example.ExamplePlugin;
import net.runelite.client.plugins.projectx.inventorysetups.MInventorySetupsPlugin;
import net.runelite.client.plugins.projectx.mouserecorder.MouseMacroRecorderPlugin;
import net.runelite.client.plugins.projectx.questhelper.QuestHelperPlugin;
import net.runelite.client.plugins.projectx.shortestpath.ShortestPathPlugin;
import net.runelite.client.plugins.projectx.util.antiban.AntibanPlugin;

@Getter
public enum ProjectXPluginChoice {
    NONE("None", null),
    ANTIBAN("Antiban", AntibanPlugin.class),
    AUTO_LOGIN("Auto Login", AutoLoginPlugin.class),
    QUEST_HELPER("Quest Helper", QuestHelperPlugin.class),
    SHORTEST_PATH("Shortest Path", ShortestPathPlugin.class),
    MOUSE_MACRO_RECORDER("Mouse Macro Recorder", MouseMacroRecorderPlugin.class),
    INVENTORY_SETUPS("Inventory Setups", MInventorySetupsPlugin.class),
    BREAK_HANDLER("BreakHandler (v1)", BreakHandlerPlugin.class),
    BREAK_HANDLER_V2("BreakHandler V2", BreakHandlerV2Plugin.class),
    EXAMPLE("Example Plugin", ExamplePlugin.class);

    private final String displayName;
    private final Class<? extends Plugin> pluginClass;

    ProjectXPluginChoice(String displayName, Class<? extends Plugin> pluginClass) {
        this.displayName = displayName;
        this.pluginClass = pluginClass;
    }

    /**
     * Returns the fully qualified class name for this choice, or {@code null} for NONE.
     */
    public String getClassName() {
        return pluginClass != null ? pluginClass.getName() : null;
    }

    /**
     * Attempts to resolve a stored configuration value (enum name, display name, or class name)
     * back to a {@link ProjectXPluginChoice}.
     *
     * @param value persisted config value
     * @return optional matching choice
     */
    public static java.util.Optional<ProjectXPluginChoice> fromConfigValue(String value) {
        if (value == null) {
            return java.util.Optional.empty();
        }

        for (ProjectXPluginChoice choice : values()) {
            if (choice.name().equalsIgnoreCase(value)
                || choice.displayName.equalsIgnoreCase(value)
                || (choice.pluginClass != null && choice.pluginClass.getName().equalsIgnoreCase(value))
                || (choice.pluginClass != null && choice.pluginClass.getSimpleName().equalsIgnoreCase(value))) {
                return java.util.Optional.of(choice);
            }
        }

        return java.util.Optional.empty();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
