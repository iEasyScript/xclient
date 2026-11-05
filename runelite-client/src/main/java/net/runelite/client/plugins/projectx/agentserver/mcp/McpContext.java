package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.handler.AgentHandler;
import net.runelite.client.plugins.projectx.agentserver.handler.LocalExchange;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;

/**
 * What the MCP tools share: the REST handlers (called in-process), the chat log, settings, and
 * the threading helpers. Reads go through {@link #onClient}; waits through {@link #waitUntil}, which
 * sleeps on the calling (HTTP request) thread and never on the client thread.
 */
public final class McpContext
{
	/** Cap on any single wait, so a tool call can never hold a request thread for ever. */
	public static final int MAX_WAIT_MS = 120_000;

	private final Client client;
	private final Map<String, AgentHandler> handlers = new LinkedHashMap<>();
	private final ChatLog chatLog;
	private final int maxResults;
	private final ServerInfo serverInfo;

	/** Where the server listens; shown by get_client_info. */
	public interface ServerInfo
	{
		String describe();
	}

	public McpContext(Client client, List<AgentHandler> restHandlers, ChatLog chatLog, int maxResults, ServerInfo serverInfo)
	{
		this.client = client;
		for (AgentHandler h : restHandlers)
		{
			handlers.put(h.getPath(), h);
		}
		this.chatLog = chatLog;
		this.maxResults = maxResults;
		this.serverInfo = serverInfo;
	}

	public Client client()
	{
		return client;
	}

	public ChatLog chatLog()
	{
		return chatLog;
	}

	public int maxResults()
	{
		return maxResults;
	}

	public String serverDescription()
	{
		return serverInfo != null ? serverInfo.describe() : "";
	}

	// ---- REST handlers, in-process

	/** Calls a REST route and returns its JSON with snake_case keys; a 4xx/5xx becomes a {@link ToolFailure}. */
	public JsonElement rest(String method, String pathAndQuery, Object body) throws IOException
	{
		LocalExchange.Response r = restRaw(method, pathAndQuery, body);
		JsonElement json = r.isJson() ? McpJson.snake(r.json()) : null;
		if (r.getStatus() >= 400 || r.getStatus() < 0)
		{
			String message = "Request failed (" + r.getStatus() + ")";
			if (json != null && json.isJsonObject() && json.getAsJsonObject().has("error"))
			{
				message = json.getAsJsonObject().get("error").getAsString();
			}
			else if (json != null && json.isJsonObject() && json.getAsJsonObject().has("message"))
			{
				message = json.getAsJsonObject().get("message").getAsString();
			}
			if (json == null || !json.isJsonObject())
			{
				JsonObject o = new JsonObject();
				o.addProperty("error", message);
				json = o;
			}
			else if (!json.getAsJsonObject().has("error"))
			{
				json.getAsJsonObject().addProperty("error", message);
			}
			throw new ToolFailure(message, json);
		}
		return json != null ? json : new JsonObject();
	}

	public LocalExchange.Response restRaw(String method, String pathAndQuery, Object body) throws IOException
	{
		String path = pathAndQuery;
		int q = path.indexOf('?');
		if (q >= 0)
		{
			path = path.substring(0, q);
		}
		AgentHandler best = null;
		int bestLen = -1;
		for (Map.Entry<String, AgentHandler> e : handlers.entrySet())
		{
			String p = e.getKey();
			if (path.startsWith(p) && p.length() > bestLen)
			{
				best = e.getValue();
				bestLen = p.length();
			}
		}
		if (best == null)
		{
			throw new ToolFailure("No REST handler for " + path);
		}
		return best.invokeLocally(method, pathAndQuery, body);
	}

	/** {@code ?a=1&b=x} from alternating names and values; null values are skipped. */
	public static String query(Object... kv)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i + 1 < kv.length; i += 2)
		{
			if (kv[i + 1] == null)
			{
				continue;
			}
			sb.append(sb.length() == 0 ? '?' : '&');
			sb.append(kv[i]).append('=').append(URLEncoder.encode(String.valueOf(kv[i + 1]), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	// ---- threading

	private static final class Box<T>
	{
		final T value;
		final Exception error;

		Box(T value, Exception error)
		{
			this.value = value;
			this.error = error;
		}
	}

	/**
	 * Runs {@code read} on the client thread and returns its value (which may be null). Throws a
	 * {@link ToolFailure} when the game thread does not answer, and rethrows what {@code read} threw.
	 * Must not be used for clicks or waits.
	 */
	public static <T> T onClient(Callable<T> read)
	{
		ClientThread ct = ProjectX.getClientThread();
		if (ct == null || ProjectX.getClient() == null)
		{
			throw new ToolFailure("The game client is not ready yet");
		}
		Box<T> box = ct.runOnClientThreadOptional(() ->
		{
			try
			{
				return new Box<>(read.call(), null);
			}
			catch (Exception e)
			{
				return new Box<T>(null, e);
			}
		}).orElse(null);
		if (box == null)
		{
			throw new ToolFailure("The game thread did not answer in time");
		}
		if (box.error != null)
		{
			if (box.error instanceof RuntimeException)
			{
				throw (RuntimeException) box.error;
			}
			throw new ToolFailure(box.error.toString());
		}
		return box.value;
	}

	/**
	 * Polls {@code condition} on the calling thread until it is true or {@code timeoutMs} passes.
	 * Refuses to run on the client thread.
	 *
	 * @return true when the condition became true
	 */
	public static boolean waitUntil(BooleanSupplier condition, long timeoutMs, long pollMs) throws InterruptedException
	{
		Client c = ProjectX.getClient();
		if (c != null && c.isClientThread())
		{
			throw new IllegalStateException("Waits must not run on the client thread");
		}
		long deadline = System.currentTimeMillis() + Math.max(0, Math.min(timeoutMs, MAX_WAIT_MS));
		long poll = Math.max(10, pollMs);
		while (true)
		{
			if (condition.getAsBoolean())
			{
				return true;
			}
			long left = deadline - System.currentTimeMillis();
			if (left <= 0)
			{
				return false;
			}
			Thread.sleep(Math.min(poll, left));
		}
	}
}
