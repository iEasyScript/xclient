package net.runelite.client.plugins.projectx.alerts;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigButton;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(AlertsConfig.GROUP)
public interface AlertsConfig extends Config
{
    String GROUP = "projectxAlerts";
    String KEY_WEBHOOK = "discordWebhookUrl";
    String KEY_TEST = "sendTestAlert";

    // ============================================================
    // Where alerts go
    // ============================================================
    @ConfigSection(
        name = "Channels",
        description = "Where alerts are sent",
        position = 0
    )
    String channels = "channels";

    @ConfigItem(
        keyName = "discordEnabled",
        name = "Discord webhook",
        description = "Post alerts to your own Discord channel through a webhook",
        position = 0,
        section = channels
    )
    default boolean discordEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = KEY_WEBHOOK,
        name = "Webhook URL",
        description = "A https://discord.com/api/webhooks/... URL (Discord: channel settings, Integrations, Webhooks)."
            + " Leave blank to use the webhook saved on your account profile. Keep it private: anyone with it can post to your channel.",
        position = 1,
        section = channels,
        secret = true
    )
    default String discordWebhookUrl()
    {
        return "";
    }

    @ConfigItem(
        keyName = "desktopEnabled",
        name = "Desktop notification",
        description = "Show alerts as RuneLite notifications (uses RuneLite's notification settings)",
        position = 2,
        section = channels
    )
    default boolean desktopEnabled()
    {
        return true;
    }

    @ConfigItem(
        keyName = "includePlayerName",
        name = "Include in-game name",
        description = "Put your in-game name in alerts, so you can tell accounts apart",
        position = 3,
        section = channels
    )
    default boolean includePlayerName()
    {
        return true;
    }

    @ConfigItem(
        keyName = KEY_TEST,
        name = "Send test alert",
        description = "Send a test alert to every channel that is switched on. The plugin must be on.",
        position = 4,
        section = channels
    )
    default ConfigButton sendTestAlert()
    {
        return new ConfigButton();
    }

    // ============================================================
    // What to alert on
    // ============================================================
    @ConfigSection(
        name = "Events",
        description = "What to be told about",
        position = 1
    )
    String events = "events";

    @ConfigItem(
        keyName = "scriptStopped",
        name = "Script stopped",
        description = "A script stopped by itself (out of supplies, an error, a goal reached) or your access to it ended, with the reason",
        position = 0,
        section = events
    )
    default boolean scriptStopped()
    {
        return true;
    }

    @ConfigItem(
        keyName = "manualStop",
        name = "Also when I stop it",
        description = "Also alert when you stop a script yourself",
        position = 1,
        section = events
    )
    default boolean manualStop()
    {
        return false;
    }

    @ConfigItem(
        keyName = "death",
        name = "Death",
        description = "Your character died",
        position = 2,
        section = events
    )
    default boolean death()
    {
        return true;
    }

    @ConfigItem(
        keyName = "levelUp",
        name = "Level up",
        description = "A skill went up a level",
        position = 3,
        section = events
    )
    default boolean levelUp()
    {
        return true;
    }

    @ConfigItem(
        keyName = "valuableDrop",
        name = "Valuable drop",
        description = "Loot worth at least the amount below",
        position = 4,
        section = events
    )
    default boolean valuableDrop()
    {
        return true;
    }

    @Range(min = 1)
    @ConfigItem(
        keyName = "dropThreshold",
        name = "Drop value (gp)",
        description = "Alert on loot worth this much or more, all items of the drop together, at Grand Exchange prices",
        position = 5,
        section = events
    )
    default int dropThreshold()
    {
        return 100_000;
    }

    @ConfigItem(
        keyName = "queueFinished",
        name = "Script queue finished",
        description = "The script queue ran its last step",
        position = 6,
        section = events
    )
    default boolean queueFinished()
    {
        return true;
    }

    @ConfigItem(
        keyName = "loggedOut",
        name = "Logged out / disconnected",
        description = "You were logged out or lost connection while a script was running, and it was not a planned break",
        position = 7,
        section = events
    )
    default boolean loggedOut()
    {
        return true;
    }

    @ConfigItem(
        keyName = "onlyWhileScriptRuns",
        name = "Only while a script runs",
        description = "Death, level-up and drop alerts only while a store script is running, not while you play yourself",
        position = 8,
        section = events
    )
    default boolean onlyWhileScriptRuns()
    {
        return true;
    }
}
