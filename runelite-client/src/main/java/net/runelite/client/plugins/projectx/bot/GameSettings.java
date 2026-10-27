package net.runelite.client.plugins.projectx.bot;

import net.runelite.client.plugins.projectx.util.settings.Rs2Settings;

/**
 * In-game settings. Named {@code GameSettings} to avoid the very common name {@code Settings}.
 *
 * <p>Every static method and field of {@link Rs2Settings} is available here under the shorter name,
 * so {@code Rs2Settings.anything(...)} can be written {@code GameSettings.anything(...)}.
 */
@SuppressWarnings({"deprecation", "removal"})
public final class GameSettings extends Rs2Settings
{
    private GameSettings()
    {
    }
}
