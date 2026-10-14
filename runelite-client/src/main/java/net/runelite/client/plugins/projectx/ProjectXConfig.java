package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.Level;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigButton;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import net.runelite.client.plugins.projectx.util.reliability.ReliabilitySettings;

@ConfigGroup(ProjectXConfig.configGroup)
public interface ProjectXConfig extends Config
{
	String configGroup = "projectx";

	@ConfigSection(
		name = "General",
		description = "The overall global settings for projectx",
		position = 0
	)
	String generalSection = "generalSection";

	String keyDisableLevelUpInterface = "disableLevelUpInterface";
	@ConfigItem(
		keyName = keyDisableLevelUpInterface,
		name = "Disable level-up interface",
		description = "Automatically close the level-up interface when it appears",
		position = 0,
		section = generalSection
	)
	default boolean disableLevelUpInterface()
	{
		return true;
	}

	String keyDisableWorldSwitcherConfirmation = "disableWorldSwitcherConfirmation";
	@ConfigItem(
		keyName = keyDisableWorldSwitcherConfirmation,
		name = "Disable world switcher confirmation",
		description = "Automatically disable the world switcher confirmation prompt",
		position = 1,
		section = generalSection
	)
	default boolean disableWorldSwitcherConfirmation()
	{
		return true;
	}

	String keyHideRoofs = "hideRoofs";
	@ConfigItem(
		keyName = keyHideRoofs,
		name = "Hide roofs",
		description = "Automatically enable the hide roofs display setting",
		position = 2,
		section = generalSection
	)
	default boolean hideRoofs()
	{
		return true;
	}

	@ConfigSection(
		name = "Movement",
		description = "Movement and stamina settings. Plugins may override these, but any changes will be reflected here.",
		position = 1,
		closedByDefault = true
	)
	String movementSection = "movementSection";

	String keyEnableAutoRunOn = "enableAutoRunOn";
	@ConfigItem(
		keyName = keyEnableAutoRunOn,
		name = "Enable auto run",
		description = "Automatically toggle run on when you have run energy",
		position = 0,
		section = movementSection
	)
	default boolean enableAutoRunOn()
	{
		return true;
	}

	String keyUseStaminaPotsIfNeeded = "useStaminaPotsIfNeeded";
	@ConfigItem(
		keyName = keyUseStaminaPotsIfNeeded,
		name = "Use stamina potions",
		description = "Automatically use stamina potions from inventory when run energy is low and the player is moving",
		position = 1,
		section = movementSection
	)
	default boolean useStaminaPotsIfNeeded()
	{
		return true;
	}

	@ConfigSection(
		name = "Logging",
		description = "Game chat logging configuration",
		position = 2
	)
	String loggingSection = "loggingSection";
	@ConfigSection(
			name = "Caching",
			description = "Caching ingame data",
			position = 3
	)
	String cacheSection = "cacheSection";

	@ConfigSection(
			name = "Project X account",
			description = "Links this client to your Project X website account",
			position = 4
	)
	String accountSection = "accountSection";

	String keyAccountToken = "accountToken";
	@ConfigItem(
		keyName = keyAccountToken,
		name = "API token",
		description = "Needed to run scripts you've bought. Create one on the Project X website under Account.",
		secret = true,
		position = 0,
		section = accountSection
	)
	default String accountToken() {
		return "";
	}

	String keySiteUrl = "siteUrl";
	@ConfigItem(
		keyName = keySiteUrl,
		name = "Site URL",
		description = "Where the Project X website lives, for the plugin hub and access checks. " +
				"Leave blank for the default. Overridden by -Dprojectx.siteUrl or PROJECTX_SITE_URL.",
		position = 1,
		section = accountSection
	)
	default String siteUrl() {
		return "";
	}

	String keyEnableGameChatLogging = "enableGameChatLogging";
	@ConfigItem(
		keyName = keyEnableGameChatLogging,
		name = "Enable Game Chat Logging",
		description = "Enable or disable logging to game chat",
		position = 0,
		section = loggingSection
	)
	default boolean enableGameChatLogging() {
		return true;
	}

	String keyGameChatLogLevel = "gameChatLogLevel";
	@ConfigItem(
		keyName = keyGameChatLogLevel,
		name = "Log Level",
		description = "Minimum log level to show in game chat:<br>" +
				"• ERROR: Only error messages<br>" +
				"• WARN: Warning and error messages<br>" +
				"• INFO: Info, warning, and error messages<br>" +
				"• DEBUG: All messages (debug mode only)",
		position = 1,
		section = loggingSection
	)
	default GameChatLogLevel getGameChatLogLevel() {
		return GameChatLogLevel.WARN;
	}

	String keyGameChatLogPattern = "gameChatLogPattern";
	@ConfigItem(
		keyName = keyGameChatLogPattern,
		name = "Log Pattern",
		description = "Format of log messages in game chat",
		position = 2,
		section = loggingSection
	)
	default GameChatLogPattern getGameChatLogPattern() {
		return GameChatLogPattern.SIMPLE;
	}

	String keyOnlyProjectXLogging = "onlyProjectXLogging";
	@ConfigItem(
		keyName = keyOnlyProjectXLogging,
		name = "Only Project X Logs",
		description = "Show only Project X plugin logs in game chat (filters out other RuneLite logs)",
		position = 3,
		section = loggingSection
	)
	default boolean onlyProjectXLogging() {
		return false;
	}

	String keyEnableMenuEntryLogging = "enableMenuEntryLogging";

	@ConfigItem(
			keyName = keyEnableMenuEntryLogging,
			name = "Enable Menu Entry Logging",
			description = "Enable or disable logging menu entry clicked",
			position = 4,
			section = loggingSection
	)
	default boolean enableMenuEntryLogging() {
		return false;
	}

	@AllArgsConstructor
	enum GameChatLogLevel {
		ERROR("Error", Level.ERROR),
		WARN("Warning", Level.WARN),
		INFO("Info", Level.INFO),
		DEBUG("Debug", Level.DEBUG);

		private final String displayName;
		@Getter
		private final Level level;

		@Override
		public String toString() {
			return displayName;
		}
	}

	enum GameChatLogPattern {
		SIMPLE("Simple", "[%d{HH:mm:ss}] %msg%ex{0}%n"),
		DETAILED("Detailed", "%d{HH:mm:ss} [%thread] %-5level %logger{36} - %msg%ex{0}%n");

		private final String displayName;
		@Getter
        private final String pattern;

		GameChatLogPattern(String displayName, String pattern) {
			this.displayName = displayName;
			this.pattern = pattern;
		}

		@Override
		public String toString() {
			return displayName;
		}
    }

	@ConfigItem(
		keyName = "showCacheInfo",
		name = "Show Cache Information",
		description = "Display cache statistics and management button in overlays",
		position = 2,
		section = generalSection
	)
	default boolean showCacheInfo() {
		return false;
	}

	@ConfigSection(
		name = "Script reliability",
		description = "How scripts recover when something goes wrong: retrying a failed action, "
			+ "recovering, and stopping safely instead of hanging or carrying on in a broken state. "
			+ "Applies to scripts that use it; changes take effect straight away.",
		position = 5,
		closedByDefault = true
	)
	String reliabilitySection = "reliabilitySection";

	String keyReliabilityEnabled = "reliabilityEnabled";
	@ConfigItem(
		keyName = keyReliabilityEnabled,
		name = "Enable script reliability",
		description = "Let scripts retry failed actions, recover, and stop safely when they are stuck. "
			+ "When off, each action is tried once and nothing is stopped automatically.",
		position = 0,
		section = reliabilitySection
	)
	default boolean reliabilityEnabled()
	{
		return ReliabilitySettings.DEFAULT_ENABLED;
	}

	String keyReliabilityMaxRetries = "reliabilityMaxRetries";
	@Range(min = ReliabilitySettings.MIN_RETRIES, max = ReliabilitySettings.MAX_RETRIES)
	@ConfigItem(
		keyName = keyReliabilityMaxRetries,
		name = "Retries per action",
		description = "How many more times a script tries an action that did not work, waiting a little longer each time (0 to 10).",
		position = 1,
		section = reliabilitySection
	)
	default int reliabilityMaxRetries()
	{
		return ReliabilitySettings.DEFAULT_RETRIES;
	}

	String keyReliabilityMaxConsecutiveFailures = "reliabilityMaxConsecutiveFailures";
	@Range(min = ReliabilitySettings.MIN_CONSECUTIVE_FAILURES, max = ReliabilitySettings.MAX_CONSECUTIVE_FAILURES)
	@ConfigItem(
		keyName = keyReliabilityMaxConsecutiveFailures,
		name = "Failures before stopping",
		description = "Stop the script safely after this many failed steps in a row, "
			+ "or when the same step fails this many times without working once (1 to 50).",
		position = 2,
		section = reliabilitySection
	)
	default int reliabilityMaxConsecutiveFailures()
	{
		return ReliabilitySettings.DEFAULT_CONSECUTIVE_FAILURES;
	}

	String keyReliabilityNoProgressTimeout = "reliabilityNoProgressTimeout";
	@Range(min = ReliabilitySettings.MIN_NO_PROGRESS_SECONDS, max = ReliabilitySettings.MAX_NO_PROGRESS_SECONDS)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = keyReliabilityNoProgressTimeout,
		name = "No-progress timeout",
		description = "If a script makes no progress for this long it tries to recover once, "
			+ "then stops safely if another timeout passes. Time logged out, paused or on a break "
			+ "does not count (15 to 1800 seconds).",
		position = 3,
		section = reliabilitySection
	)
	default int reliabilityNoProgressTimeout()
	{
		return ReliabilitySettings.DEFAULT_NO_PROGRESS_SECONDS;
	}

	String keyReliabilityDetailedLogging = "reliabilityDetailedLogging";
	@ConfigItem(
		keyName = keyReliabilityDetailedLogging,
		name = "Detailed diagnostic logging",
		description = "Write every attempt, retry and checkpoint to the session log, for problem reports. "
			+ "Failures, recoveries and stops are always logged.",
		position = 4,
		section = reliabilitySection
	)
	default boolean reliabilityDetailedLogging()
	{
		return ReliabilitySettings.DEFAULT_DETAILED_LOGGING;
	}

	String keyReliabilityResetDefaults = "reliabilityResetDefaults";
	@ConfigItem(
		keyName = keyReliabilityResetDefaults,
		name = "Reset reliability settings",
		description = "Put the script reliability settings back to their defaults. Other settings are not changed.",
		position = 5,
		section = reliabilitySection
	)
	default ConfigButton reliabilityResetDefaults()
	{
		return new ConfigButton();
	}

	/** The stored values of the reliability section, which "Reset reliability settings" clears. */
	String[] RELIABILITY_KEYS = {
		keyReliabilityEnabled,
		keyReliabilityMaxRetries,
		keyReliabilityMaxConsecutiveFailures,
		keyReliabilityNoProgressTimeout,
		keyReliabilityDetailedLogging
	};

	String keyDisableTelemetry = "disableTelemetry";
	@ConfigItem(
		keyName = keyDisableTelemetry,
		name = "Disable telemetry",
		description = "Stop outbound calls to the Project X site (update check, random-fact splash, session ping). " +
				"Equivalent to launching with -Dprojectx.disableTelemetry=true.",
		position = 6,
		section = generalSection
	)
	default boolean disableTelemetry() {
		return false;
	}
}
