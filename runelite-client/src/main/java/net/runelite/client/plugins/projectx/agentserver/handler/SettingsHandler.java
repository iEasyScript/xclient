package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.ProjectXConfig;
import net.runelite.client.plugins.projectx.util.settings.Rs2Settings;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsHandler extends AgentHandler {

	private static final Map<String, String> VALID_CONFIG_KEYS = Map.of(
		"disableLevelUpInterface", ProjectXConfig.keyDisableLevelUpInterface,
		"disableWorldSwitcherConfirmation", ProjectXConfig.keyDisableWorldSwitcherConfirmation
	);

	public SettingsHandler(Gson gson) {
		super(gson);
	}

	@Override
	public String getPath() {
		return "/settings";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String subPath = getSubPath(exchange, "/settings");

		if ("/level-up".equals(subPath)) {
			handleLevelUp(exchange);
		} else if ("/config".equals(subPath)) {
			handleConfig(exchange);
		} else if ("/plugin".equals(subPath)) {
			handlePluginConfig(exchange);
		} else {
			sendJson(exchange, 404, errorResponse("Unknown sub-path: " + subPath));
		}
	}

	private void handleLevelUp(HttpExchange exchange) throws IOException {
		if ("GET".equals(exchange.getRequestMethod())) {
			boolean enabled = Rs2Settings.isLevelUpNotificationsEnabled();
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("enabled", enabled);
			sendJson(exchange, 200, response);
			return;
		}

		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> request;
		try {
			request = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		Boolean enable = (Boolean) request.get("enable");
		if (enable == null) {
			sendJson(exchange, 400, errorResponse("Required: enable (true/false)"));
			return;
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("action", enable ? "enable" : "disable");

		try {
			boolean result;
			if (enable) {
				result = Rs2Settings.enableLevelUpNotifications(true);
			} else {
				result = Rs2Settings.disableLevelUpNotifications(true);
			}
			response.put("success", result);
			response.put("enabled", Rs2Settings.isLevelUpNotificationsEnabled());
		} catch (Exception e) {
			response.put("success", false);
			response.put("error", e.getMessage());
		}

		sendJson(exchange, 200, response);
	}

	private void handleConfig(HttpExchange exchange) throws IOException {
		ConfigManager configManager = ProjectX.getConfigManager();
		if (configManager == null) {
			sendJson(exchange, 500, errorResponse("ConfigManager not available"));
			return;
		}

		if ("GET".equals(exchange.getRequestMethod())) {
			ProjectXConfig config = configManager.getConfig(ProjectXConfig.class);
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("disableLevelUpInterface", config != null && config.disableLevelUpInterface());
			response.put("disableWorldSwitcherConfirmation", config != null && config.disableWorldSwitcherConfirmation());
			sendJson(exchange, 200, response);
			return;
		}

		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> request;
		try {
			request = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		String key = (String) request.get("key");
		Object value = request.get("value");
		if (key == null || value == null) {
			sendJson(exchange, 400, errorResponse("Required: key, value"));
			return;
		}

		String configKey = VALID_CONFIG_KEYS.get(key);
		if (configKey == null) {
			sendJson(exchange, 400, errorResponse("Unknown key: " + key + ". Valid keys: " + VALID_CONFIG_KEYS.keySet()));
			return;
		}

		configManager.setConfiguration(ProjectXConfig.configGroup, configKey, value.toString());

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("key", key);
		response.put("configKey", configKey);
		response.put("value", value);
		response.put("success", true);
		sendJson(exchange, 200, response);
	}

	private void handlePluginConfig(HttpExchange exchange) throws IOException {
		ConfigManager configManager = ProjectX.getConfigManager();
		if (configManager == null) {
			sendJson(exchange, 500, errorResponse("ConfigManager not available"));
			return;
		}

		if ("GET".equals(exchange.getRequestMethod())) {
			Map<String, String> params = parseQuery(exchange.getRequestURI());
			String group = params.get("group");
			String key = params.get("key");

			if (group == null || group.isEmpty()) {
				sendJson(exchange, 400, errorResponse("Required query parameter: group"));
				return;
			}

			if (key == null || key.isEmpty()) {
				sendJson(exchange, 400, errorResponse("Required query parameters: group, key"));
				return;
			}

			String value = configManager.getConfiguration(group, key);
			Map<String, Object> response = new LinkedHashMap<>();
			response.put("group", group);
			response.put("key", key);
			response.put("value", value);
			sendJson(exchange, 200, response);
			return;
		}

		try {
			requirePost(exchange);
		} catch (HttpMethodException e) {
			sendJson(exchange, 405, errorResponse(e.getMessage()));
			return;
		}

		Map<String, Object> request;
		try {
			request = readJsonBody(exchange);
		} catch (Exception e) {
			sendJson(exchange, 400, errorResponse("Invalid JSON body"));
			return;
		}

		String group = (String) request.get("group");
		String key = (String) request.get("key");
		Object value = request.get("value");

		if (group == null || group.isEmpty() || key == null || key.isEmpty()) {
			sendJson(exchange, 400, errorResponse("Required: group, key, value"));
			return;
		}

		if (value == null) {
			configManager.unsetConfiguration(group, key);
		} else {
			configManager.setConfiguration(group, key, value.toString());
		}

		String current = configManager.getConfiguration(group, key);
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("group", group);
		response.put("key", key);
		response.put("value", current);
		response.put("success", true);
		sendJson(exchange, 200, response);
	}
}
