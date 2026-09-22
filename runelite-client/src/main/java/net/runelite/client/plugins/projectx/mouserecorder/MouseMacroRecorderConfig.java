package net.runelite.client.plugins.projectx.mouserecorder;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigButton;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(MouseMacroRecorderConfig.CONFIG_GROUP)
public interface MouseMacroRecorderConfig extends Config
{
	String CONFIG_GROUP = "mouseMacroRecorder";

	@ConfigSection(
		name = "Recording",
		description = "Configure how movement samples are recorded",
		position = 0
	)
	String recordingSection = "recording";

	@ConfigItem(
		keyName = "recordingEnabled",
		name = "Recording enabled",
		description = "Toggle to start or stop recording",
		position = 0,
		section = recordingSection
	)
	default boolean recordingEnabled()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recordMouseMovement",
		name = "Record mouse movement",
		description = "Capture mouse move/drag events",
		position = 1,
		section = recordingSection
	)
	default boolean recordMouseMovement()
	{
		return true;
	}

	@ConfigItem(
		keyName = "movementSampleIntervalMs",
		name = "Movement sample interval (ms)",
		description = "Minimum time between recorded mouse movement samples",
		position = 2,
		section = recordingSection
	)
	@Range(min = 5, max = 500)
	default int movementSampleIntervalMs()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "movementMinDistance",
		name = "Movement min distance",
		description = "Minimum pixel distance before recording a move",
		position = 3,
		section = recordingSection
	)
	@Range(min = 0, max = 50)
	default int movementMinDistance()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "openRecordingsFolder",
		name = "Open recordings folder",
		description = "Open the folder containing JSONL mouse macro recordings",
		position = 4,
		section = recordingSection
	)
	default ConfigButton openRecordingsFolder()
	{
		return new ConfigButton();
	}
}
