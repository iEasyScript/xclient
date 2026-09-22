package net.runelite.client.plugins.projectx;

import ch.qos.logback.classic.Level;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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

	String keyDisableTelemetry = "disableTelemetry";
	@ConfigItem(
		keyName = keyDisableTelemetry,
		name = "Disable telemetry",
		description = "Stop outbound calls to microbot.cloud (update check, random-fact splash, session ping). " +
				"Equivalent to launching with -Dprojectx.disableTelemetry=true.",
		position = 6,
		section = generalSection
	)
	default boolean disableTelemetry() {
		return false;
	}
}
