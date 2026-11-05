package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Base64;

/**
 * An MCP {@code tools/call} result: a list of content blocks plus {@code isError}. Data is sent as
 * one {@code text} block holding JSON; screenshots add an {@code image} block (base64 PNG).
 */
public final class ToolResult
{
	private static final Gson GSON = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

	private final JsonArray content = new JsonArray();
	private boolean error;

	private ToolResult()
	{
	}

	/** A JSON text block from any Gson-serialisable value (maps, lists, JsonElements). */
	public static ToolResult json(Object value)
	{
		ToolResult r = new ToolResult();
		r.addText(toJsonString(value));
		return r;
	}

	public static ToolResult text(String text)
	{
		ToolResult r = new ToolResult();
		r.addText(text);
		return r;
	}

	public static ToolResult error(String message)
	{
		JsonObject o = new JsonObject();
		o.addProperty("error", message);
		return error(o);
	}

	public static ToolResult error(JsonElement payload)
	{
		ToolResult r = new ToolResult();
		r.addText(toJsonString(payload));
		r.error = true;
		return r;
	}

	/** An image block (PNG) followed by a JSON text block describing it. */
	public static ToolResult image(byte[] png, Object meta)
	{
		ToolResult r = new ToolResult();
		JsonObject img = new JsonObject();
		img.addProperty("type", "image");
		img.addProperty("data", Base64.getEncoder().encodeToString(png));
		img.addProperty("mimeType", "image/png");
		r.content.add(img);
		if (meta != null)
		{
			r.addText(toJsonString(meta));
		}
		return r;
	}

	public boolean isError()
	{
		return error;
	}

	public JsonArray getContent()
	{
		return content;
	}

	public JsonObject toJson()
	{
		JsonObject o = new JsonObject();
		o.add("content", content);
		o.addProperty("isError", error);
		return o;
	}

	static String toJsonString(Object value)
	{
		if (value instanceof JsonElement)
		{
			return GSON.toJson((JsonElement) value);
		}
		return GSON.toJson(value);
	}

	private void addText(String text)
	{
		JsonObject t = new JsonObject();
		t.addProperty("type", "text");
		t.addProperty("text", text);
		content.add(t);
	}
}
