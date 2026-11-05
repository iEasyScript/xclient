package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Model Context Protocol over JSON-RPC 2.0, independent of HTTP so it can be unit tested.
 * Implements the Streamable HTTP transport's request side: every POST gets one
 * {@code application/json} answer (or 202 for notifications); no server-initiated streams.
 *
 * <p>Methods: {@code initialize}, {@code notifications/initialized}, {@code notifications/cancelled},
 * {@code ping}, {@code tools/list}, {@code tools/call}. Anything else is "method not found".
 */
@Slf4j
public final class McpProtocol
{
	public static final String LATEST_PROTOCOL = "2025-06-18";
	public static final List<String> SUPPORTED_PROTOCOLS = Collections.unmodifiableList(Arrays.asList("2025-06-18", "2025-03-26"));
	public static final String SESSION_HEADER = "Mcp-Session-Id";
	public static final String PROTOCOL_HEADER = "MCP-Protocol-Version";

	private static final int MAX_SESSIONS = 256;

	private static final String INSTRUCTIONS =
		"Project X OSRS client. Call list_tools for the tools by category. Coordinates: 'tile' is the "
			+ "scene tile the game uses for NPCs, objects and ground items (inside instances these are "
			+ "instance coordinates); 'map_tile' is the original map location and is only present inside "
			+ "an instance. Compare positions with the 'distance' field rather than by hand. Actions "
			+ "(interact_*, walk_to, click_widget) return once the click is sent; use the wait_for_* tools "
			+ "to wait for the result. Every wait has a timeout.";

	private final McpToolRegistry registry;
	private final String serverName;
	private final String serverVersion;
	private final Set<String> sessions = ConcurrentHashMap.newKeySet();

	public McpProtocol(McpToolRegistry registry, String serverName, String serverVersion)
	{
		this.registry = registry;
		this.serverName = serverName;
		this.serverVersion = serverVersion;
	}

	public McpToolRegistry getRegistry()
	{
		return registry;
	}

	/** One HTTP answer: status, optional JSON body, extra headers. */
	public static final class Reply
	{
		public final int status;
		public final String body;
		public final Map<String, String> headers = new LinkedHashMap<>();

		Reply(int status, String body)
		{
			this.status = status;
			this.body = body;
		}

		Reply header(String name, String value)
		{
			headers.put(name, value);
			return this;
		}
	}

	/**
	 * Handles a POST body.
	 *
	 * @param body           the raw request body
	 * @param sessionId      the {@code Mcp-Session-Id} header, or null
	 * @param protocolHeader the {@code MCP-Protocol-Version} header, or null
	 */
	public Reply handlePost(String body, String sessionId, String protocolHeader)
	{
		if (protocolHeader != null && !protocolHeader.isBlank() && !SUPPORTED_PROTOCOLS.contains(protocolHeader.trim()))
		{
			return new Reply(400, errorJson(JsonNull.INSTANCE, McpError.INVALID_REQUEST,
				"Unsupported MCP-Protocol-Version: " + protocolHeader + ". Supported: " + SUPPORTED_PROTOCOLS).toString());
		}
		if (sessionId != null && !sessionId.isBlank())
		{
			// Sessions are not tied to state here, so an id from before a plugin restart is simply
			// adopted rather than refused: a restarted client keeps working without a re-initialize.
			remember(sessionId.trim());
		}

		JsonElement parsed;
		try
		{
			parsed = new JsonParser().parse(body == null ? "" : body);
		}
		catch (JsonParseException e)
		{
			return new Reply(400, errorJson(JsonNull.INSTANCE, McpError.PARSE_ERROR, "Parse error: " + rootMessage(e)).toString());
		}
		if (parsed == null || parsed.isJsonNull())
		{
			return new Reply(400, errorJson(JsonNull.INSTANCE, McpError.INVALID_REQUEST, "Empty request").toString());
		}

		Map<String, String> headers = new LinkedHashMap<>();
		if (parsed.isJsonArray())
		{
			JsonArray batch = parsed.getAsJsonArray();
			if (batch.size() == 0)
			{
				return new Reply(400, errorJson(JsonNull.INSTANCE, McpError.INVALID_REQUEST, "Empty batch").toString());
			}
			JsonArray answers = new JsonArray();
			for (JsonElement message : batch)
			{
				JsonObject answer = process(message, headers);
				if (answer != null)
				{
					answers.add(answer);
				}
			}
			Reply r = answers.size() == 0 ? new Reply(202, null) : new Reply(200, answers.toString());
			r.headers.putAll(headers);
			return r;
		}

		JsonObject answer = process(parsed, headers);
		Reply r;
		if (answer == null)
		{
			r = new Reply(202, null);
		}
		else
		{
			boolean malformed = answer.has("error")
				&& answer.getAsJsonObject("error").get("code").getAsInt() == McpError.INVALID_REQUEST;
			r = new Reply(malformed ? 400 : 200, answer.toString());
		}
		r.headers.putAll(headers);
		return r;
	}

	/** DELETE ends a session. */
	public Reply handleDelete(String sessionId)
	{
		if (sessionId != null)
		{
			sessions.remove(sessionId.trim());
		}
		return new Reply(204, null);
	}

	public boolean hasSession(String sessionId)
	{
		return sessionId != null && sessions.contains(sessionId);
	}

	/** One JSON-RPC message; null when it needs no answer (notification or client response). */
	JsonObject process(JsonElement message, Map<String, String> headers)
	{
		if (!message.isJsonObject())
		{
			return errorJson(JsonNull.INSTANCE, McpError.INVALID_REQUEST, "A JSON-RPC message must be an object");
		}
		JsonObject msg = message.getAsJsonObject();
		JsonElement id = msg.get("id");
		boolean hasId = id != null && !id.isJsonNull();
		if (hasId && !(id.isJsonPrimitive() && (id.getAsJsonPrimitive().isString() || id.getAsJsonPrimitive().isNumber())))
		{
			return errorJson(JsonNull.INSTANCE, McpError.INVALID_REQUEST, "id must be a string or a number");
		}
		JsonElement idOut = hasId ? id : JsonNull.INSTANCE;

		JsonElement version = msg.get("jsonrpc");
		if (version == null || !version.isJsonPrimitive() || !"2.0".equals(version.getAsString()))
		{
			return errorJson(idOut, McpError.INVALID_REQUEST, "jsonrpc must be \"2.0\"");
		}

		JsonElement methodEl = msg.get("method");
		if (methodEl == null || methodEl.isJsonNull())
		{
			if (msg.has("result") || msg.has("error"))
			{
				// A response to a server request. We never send any, so there is nothing to match.
				return null;
			}
			return errorJson(idOut, McpError.INVALID_REQUEST, "Missing method");
		}
		if (!methodEl.isJsonPrimitive() || !methodEl.getAsJsonPrimitive().isString())
		{
			return errorJson(idOut, McpError.INVALID_REQUEST, "method must be a string");
		}
		String method = methodEl.getAsString();

		JsonElement paramsEl = msg.get("params");
		if (paramsEl != null && !paramsEl.isJsonNull() && !paramsEl.isJsonObject())
		{
			return hasId ? errorJson(idOut, McpError.INVALID_PARAMS, "params must be an object") : null;
		}
		JsonObject params = paramsEl != null && paramsEl.isJsonObject() ? paramsEl.getAsJsonObject() : new JsonObject();

		if (!hasId)
		{
			// Notifications: initialized, cancelled, anything else - none needs an answer.
			return null;
		}

		try
		{
			JsonElement result = dispatch(method, params, headers);
			JsonObject response = new JsonObject();
			response.addProperty("jsonrpc", "2.0");
			response.add("id", idOut);
			response.add("result", result);
			return response;
		}
		catch (McpError e)
		{
			return errorJson(idOut, e.getCode(), e.getMessage());
		}
		catch (RuntimeException e)
		{
			log.warn("MCP {} failed: {}", method, e.toString());
			return errorJson(idOut, McpError.INTERNAL_ERROR, "Internal error: " + e);
		}
	}

	private JsonElement dispatch(String method, JsonObject params, Map<String, String> headers)
	{
		switch (method)
		{
			case "initialize":
				return initialize(params, headers);
			case "ping":
				return new JsonObject();
			case "tools/list":
				return listTools();
			case "tools/call":
				return callTool(params);
			default:
				throw new McpError(McpError.METHOD_NOT_FOUND, "Method not found: " + method);
		}
	}

	private JsonObject initialize(JsonObject params, Map<String, String> headers)
	{
		String requested = params.has("protocolVersion") && params.get("protocolVersion").isJsonPrimitive()
			? params.get("protocolVersion").getAsString() : null;
		String negotiated = requested != null && SUPPORTED_PROTOCOLS.contains(requested) ? requested : LATEST_PROTOCOL;

		String sessionId = UUID.randomUUID().toString();
		remember(sessionId);
		headers.put(SESSION_HEADER, sessionId);

		JsonObject result = new JsonObject();
		result.addProperty("protocolVersion", negotiated);

		JsonObject capabilities = new JsonObject();
		JsonObject tools = new JsonObject();
		tools.addProperty("listChanged", false);
		capabilities.add("tools", tools);
		result.add("capabilities", capabilities);

		JsonObject info = new JsonObject();
		info.addProperty("name", serverName);
		info.addProperty("title", "Project X OSRS client");
		info.addProperty("version", serverVersion);
		result.add("serverInfo", info);
		result.addProperty("instructions", INSTRUCTIONS);
		return result;
	}

	private JsonObject listTools()
	{
		JsonArray list = new JsonArray();
		for (McpTool t : registry.all())
		{
			list.add(t.toListEntry());
		}
		JsonObject result = new JsonObject();
		result.add("tools", list);
		return result;
	}

	private JsonObject callTool(JsonObject params)
	{
		JsonElement nameEl = params.get("name");
		if (nameEl == null || !nameEl.isJsonPrimitive() || !nameEl.getAsJsonPrimitive().isString())
		{
			throw McpError.invalidParams("tools/call needs params.name (string)");
		}
		String name = nameEl.getAsString();
		McpTool tool = registry.get(name);
		if (tool == null)
		{
			throw McpError.invalidParams("Unknown tool: " + name);
		}
		JsonElement argsEl = params.get("arguments");
		if (argsEl != null && !argsEl.isJsonNull() && !argsEl.isJsonObject())
		{
			throw McpError.invalidParams("arguments must be an object");
		}
		JsonObject arguments = argsEl != null && argsEl.isJsonObject() ? argsEl.getAsJsonObject() : new JsonObject();
		checkRequired(tool, arguments);

		ToolResult result;
		try
		{
			result = tool.getBody().call(new ToolArgs(arguments));
			if (result == null)
			{
				result = ToolResult.json(new JsonObject());
			}
		}
		catch (McpError e)
		{
			throw e;
		}
		catch (ToolFailure e)
		{
			result = ToolResult.error(e.getPayload());
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			result = ToolResult.error("Interrupted");
		}
		catch (Exception e)
		{
			log.warn("MCP tool {} failed", name, e);
			result = ToolResult.error(name + " failed: " + e);
		}
		return result.toJson();
	}

	private static void checkRequired(McpTool tool, JsonObject arguments)
	{
		JsonElement required = tool.getInputSchema().get("required");
		if (required == null || !required.isJsonArray())
		{
			return;
		}
		for (JsonElement r : required.getAsJsonArray())
		{
			String key = r.getAsString();
			JsonElement v = arguments.get(key);
			if (v == null || v.isJsonNull() || (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() && v.getAsString().isEmpty()))
			{
				throw McpError.invalidParams("Missing required argument '" + key + "' for " + tool.getName());
			}
		}
	}

	private void remember(String sessionId)
	{
		if (sessions.size() >= MAX_SESSIONS)
		{
			sessions.clear();
		}
		sessions.add(sessionId);
	}

	static JsonObject errorJson(JsonElement id, int code, String message)
	{
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message);
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", "2.0");
		response.add("id", id == null ? JsonNull.INSTANCE : id);
		response.add("error", error);
		return response;
	}

	private static String rootMessage(Throwable t)
	{
		while (t.getCause() != null)
		{
			t = t.getCause();
		}
		return t.getMessage();
	}
}
