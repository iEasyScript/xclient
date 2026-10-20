package net.runelite.client.plugins.projectx.remote;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

/**
 * What this client lets the website do. All on by default: the client is signed
 * in to the player's own account, and only that account sees it on the website.
 */
@ConfigGroup(RemoteConfig.GROUP)
public interface RemoteConfig extends Config
{
    String GROUP = "projectxremote";
    String KEY_CONTROL = "allowControl";
    String KEY_MONITORING = "allowMonitoring";
    String KEY_SHARE_NAME = "shareName";

    @ConfigItem(
        keyName = KEY_CONTROL,
        name = "Allow control from the website",
        description = "Let the Bots page on the website start and stop scripts, the queue and breaks, log out and close this client."
            + " Only you can see this client there.",
        position = 0
    )
    default boolean allowControl()
    {
        return true;
    }

    @ConfigItem(
        keyName = KEY_MONITORING,
        name = "Show live details on the website",
        description = "Send what this client is doing to the Bots page: logged in or not, world, total level, HP and prayer,"
            + " the scripts running and their experience, breaks and the queue. Never your password or location.",
        position = 1
    )
    default boolean allowMonitoring()
    {
        return true;
    }

    @ConfigItem(
        keyName = KEY_SHARE_NAME,
        name = "Include in-game name",
        description = "Show your in-game name on the Bots page, so you can tell accounts apart",
        position = 2
    )
    default boolean shareName()
    {
        return true;
    }
}
