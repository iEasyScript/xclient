package net.runelite.client.plugins.projectx.util.events;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.BlockingEvent;
import net.runelite.client.plugins.projectx.BlockingEventPriority;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.plugins.projectx.util.settings.Rs2Settings;

public class HideRoofsEvent implements BlockingEvent
{
	@Override
	public boolean validate()
	{
		return isConfigEnabled() && ProjectX.isLoggedIn() && !Rs2Settings.isHideRoofsEnabled();
	}

	@Override
	public boolean execute()
	{
		if (!isConfigEnabled())
		{
			return true;
		}
		return Rs2Settings.hideRoofs();
	}

	private boolean isConfigEnabled()
	{
		ConfigManager configManager = ProjectX.getConfigManager();
		if (configManager == null)
		{
			return true;
		}

		ProjectXConfig config = configManager.getConfig(ProjectXConfig.class);
		return config == null || config.hideRoofs();
	}

	@Override
	public BlockingEventPriority priority()
	{
		return BlockingEventPriority.HIGH;
	}
}
