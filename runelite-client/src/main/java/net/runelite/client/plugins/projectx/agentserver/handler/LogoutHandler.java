package net.runelite.client.plugins.projectx.agentserver.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.util.player.Rs2Player;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class LogoutHandler extends AgentHandler {

	private static final int DEFAULT_TIMEOUT_SECONDS = 30;
	private static final int MAX_TIMEOUT_SECONDS = 120;
	private static final int POLL_INTERVAL_MS = 600;

	private final Client client;

	public LogoutHandler(Gson gson, Client client) {
		super(gson);
		this.client = client;
	}

	@Override
	public String getPath() {
		return "/logout";
	}

	@Override
	protected void handleRequest(HttpExchange exchange) throws IOException {
		String method = exchange.getRequestMethod().toUpperCase();

		if ("GET".equals(method)) {
			handleStatus(exchange);
		} else if ("POST".equals(method)) {
			handleLogout(exchange);
		} else {
			sendJson(exchange, 405, errorResponse("Use GET or POST"));
		}
	}

	private void handleStatus(HttpExchange exchange) throws IOException {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("loggedIn", ProjectX.isLoggedIn());
		result.put("gameState", client.getGameState().name());
		sendJson(exchange, 200, result);
	}

	private void handleLogout(HttpExchange exchange) throws IOException {
		if (!ProjectX.isLoggedIn()) {
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("success", true);
			result.put("message", "Already logged out");
			result.put("gameState", client.getGameState().name());
			sendJson(exchange, 200, result);
			return;
		}

		Map<String, Object> body = readJsonBody(exchange);

		boolean wait = true;
		int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

		if (body != null) {
			if (body.containsKey("wait")) {
				wait = Boolean.TRUE.equals(body.get("wait"));
			}
			if (body.containsKey("timeout")) {
				Object raw = body.get("timeout");
				int parsed = 0;
				if (raw instanceof Number) {
					parsed = ((Number) raw).intValue();
				} else if (raw instanceof String) {
					try {
						parsed = Integer.parseInt(((String) raw).trim());
					} catch (NumberFormatException ignored) {
					}
				}
				if (parsed > 0) {
					timeoutSeconds = Math.min(parsed, MAX_TIMEOUT_SECONDS);
				}
			}
		}

		long startTime = System.currentTimeMillis();

		try {
			Rs2Player.logout();
		} catch (Exception e) {
			log.error("Logout attempt failed", e);
			sendJson(exchange, 500, errorResponse("Logout failed: " + e.getMessage()));
			return;
		}

		if (!wait) {
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("success", true);
			result.put("message", "Logout initiated");
			sendJson(exchange, 200, result);
			return;
		}

		LogoutResult logoutResult = waitForLogoutResult(timeoutSeconds);
		long durationMs = System.currentTimeMillis() - startTime;

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("success", logoutResult.success);
		result.put("gameState", client.getGameState().name());
		result.put("durationMs", durationMs);
		result.put("message", logoutResult.message);

		sendJson(exchange, logoutResult.success ? 200 : 408, result);
	}

	private LogoutResult waitForLogoutResult(int timeoutSeconds) {
		long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

		while (System.currentTimeMillis() < deadline) {
			if (!ProjectX.isLoggedIn() && client.getGameState() == GameState.LOGIN_SCREEN) {
				return LogoutResult.ok("Logout successful");
			}

			try {
				Thread.sleep(POLL_INTERVAL_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return LogoutResult.fail("Logout interrupted");
			}
		}

		return LogoutResult.fail("Logout timed out after " + timeoutSeconds + "s");
	}

	private static class LogoutResult {
		final boolean success;
		final String message;

		LogoutResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		static LogoutResult ok(String message) {
			return new LogoutResult(true, message);
		}

		static LogoutResult fail(String message) {
			return new LogoutResult(false, message);
		}
	}
}
