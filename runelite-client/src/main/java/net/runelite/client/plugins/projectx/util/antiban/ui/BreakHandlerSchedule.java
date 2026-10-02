package net.runelite.client.plugins.projectx.util.antiban.ui;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerConfig;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Config;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin;
import net.runelite.client.plugins.projectx.util.antiban.enums.PlaySchedule;

/**
 * The Break Handler's play schedule, read and written from the antiban panel.
 *
 * <p>The antiban panel had its own "Simulate play schedule" toggle that nothing read, while both Break
 * Handlers already enforce a schedule. Rather than build a second one, the panel edits theirs. It writes
 * both versions' settings so they agree whichever one is running, and reads from the one that is on.
 *
 * <p>The key names differ between the two, including in case, and are copied from their
 * {@code @ConfigItem} declarations.
 */
final class BreakHandlerSchedule
{
    private static final String V1_ENABLED = "UsePlaySchedule";
    private static final String V1_SCHEDULE = "PlaySchedule";
    private static final String V2_ENABLED = "usePlaySchedule";
    private static final String V2_SCHEDULE = "playSchedule";

    /** Both Break Handlers default to this when nothing is saved. */
    static final PlaySchedule DEFAULT = PlaySchedule.MEDIUM_DAY;

    private BreakHandlerSchedule()
    {
    }

    static boolean isBreakHandlerRunning()
    {
        return ProjectX.isPluginEnabled(BreakHandlerPlugin.class)
            || ProjectX.isPluginEnabled(BreakHandlerV2Plugin.class);
    }

    static boolean isEnabled()
    {
        ConfigManager config = ProjectX.getConfigManager();
        if (config == null)
        {
            return false;
        }
        Boolean enabled = readV2()
            ? config.getConfiguration(BreakHandlerV2Config.configGroup, V2_ENABLED, Boolean.class)
            : config.getConfiguration(BreakHandlerConfig.configGroup, V1_ENABLED, Boolean.class);
        return Boolean.TRUE.equals(enabled);
    }

    static PlaySchedule schedule()
    {
        ConfigManager config = ProjectX.getConfigManager();
        if (config == null)
        {
            return DEFAULT;
        }
        PlaySchedule schedule = readV2()
            ? config.getConfiguration(BreakHandlerV2Config.configGroup, V2_SCHEDULE, PlaySchedule.class)
            : config.getConfiguration(BreakHandlerConfig.configGroup, V1_SCHEDULE, PlaySchedule.class);
        return schedule == null ? DEFAULT : schedule;
    }

    static void setEnabled(boolean enabled)
    {
        ConfigManager config = ProjectX.getConfigManager();
        if (config == null)
        {
            return;
        }
        config.setConfiguration(BreakHandlerConfig.configGroup, V1_ENABLED, enabled);
        config.setConfiguration(BreakHandlerV2Config.configGroup, V2_ENABLED, enabled);
    }

    static void setSchedule(PlaySchedule schedule)
    {
        ConfigManager config = ProjectX.getConfigManager();
        if (config == null)
        {
            return;
        }
        config.setConfiguration(BreakHandlerConfig.configGroup, V1_SCHEDULE, schedule);
        config.setConfiguration(BreakHandlerV2Config.configGroup, V2_SCHEDULE, schedule);
    }

    /** V2's settings are the ones that matter only when V2 is the one running. */
    private static boolean readV2()
    {
        return ProjectX.isPluginEnabled(BreakHandlerV2Plugin.class)
            && !ProjectX.isPluginEnabled(BreakHandlerPlugin.class);
    }
}
