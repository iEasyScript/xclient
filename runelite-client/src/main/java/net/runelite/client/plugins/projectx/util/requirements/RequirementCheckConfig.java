package net.runelite.client.plugins.projectx.util.requirements;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(RequirementCheckConfig.GROUP)
public interface RequirementCheckConfig extends Config
{
    String GROUP = "projectxrequirements";
    String SKIPPED_SCRIPTS = "skippedScripts";

    @ConfigItem(
        keyName = "enabled",
        name = "Check before starting",
        description = "Before a script starts, check what it needs (levels, quests, items, setup, members world) and show what's missing",
        position = 0
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "warnRecommended",
        name = "Warn about recommended",
        description = "Also show the checklist when only recommended things are missing, such as an inventory setup chosen in the script's settings",
        position = 1
    )
    default boolean warnRecommended()
    {
        return true;
    }

    @ConfigItem(
        keyName = SKIPPED_SCRIPTS,
        name = "Scripts not checked",
        description = "Scripts you chose not to check again, comma separated. Remove one to check it again",
        position = 2
    )
    default String skippedScripts()
    {
        return "";
    }
}
