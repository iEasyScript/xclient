package net.runelite.client.plugins.projectx.agentserver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

/**
 * Settings of the MCP server plugin. The group key stays "agentServer" so existing settings,
 * including the auth token, carry over.
 */
@ConfigGroup(AgentServerConfig.GROUP)
public interface AgentServerConfig extends Config {

	String GROUP = "agentServer";
	String KEY_TOKEN = "authToken";
	String KEY_PORT = "port";

	@ConfigItem(
			keyName = "port",
			name = "Port",
			description = "Port of the MCP server (bound to 127.0.0.1 only). MCP clients connect to http://127.0.0.1:<port>/mcp; the older REST API and projectx-cli use the same port.",
			position = 0
	)
	@Range(min = 1024, max = 65535)
	default int port() {
		return 8081;
	}

	@ConfigItem(
			keyName = "maxResults",
			name = "Max Results",
			description = "Default max results for list tools and REST list endpoints",
			position = 1
	)
	@Range(min = 10, max = 5000)
	default int maxResults() {
		return 200;
	}

	@ConfigItem(
			keyName = KEY_TOKEN,
			name = "Auth token",
			description = "Shared secret every request must carry, as 'Authorization: Bearer <token>' (MCP clients) or the X-Agent-Token header (REST, projectx-cli). Auto-generated on first start and written to ~/.runelite/.agent-token. Clear it to generate a new one.",
			secret = true,
			position = 2
	)
	default String authToken() {
		return "";
	}

	@ConfigItem(
			keyName = KEY_TOKEN,
			name = "",
			description = ""
	)
	void authToken(String token);

	@ConfigItem(
			keyName = "bindOnlyWhileScriptsActive",
			name = "Stealth bind",
			description = "Only open the MCP server's socket while a Project X script is actively running. Eliminates the localhost port fingerprint during manual play, but MCP clients cannot connect until a script runs.",
			position = 3
	)
	default boolean bindOnlyWhileScriptsActive() {
		return false;
	}

	enum BindMode { TCP, UDS }

	@ConfigItem(
			keyName = "bindMode",
			name = "Bind mode",
			description = "TCP: 127.0.0.1:<port> (what MCP clients such as Claude Code need). UDS: Unix domain socket at ~/.runelite/.agent.sock, invisible to port scanners but clients must speak UDS.",
			position = 4
	)
	default BindMode bindMode() {
		return BindMode.TCP;
	}
}
