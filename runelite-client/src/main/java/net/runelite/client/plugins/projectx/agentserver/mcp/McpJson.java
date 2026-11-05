package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.coords.WorldPoint;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** JSON helpers shared by the MCP tools. */
public final class McpJson
{
	/** camelCase identifiers only: never touches class names, paths or other data used as keys. */
	private static final Pattern CAMEL = Pattern.compile("[a-z][a-z0-9]*([A-Z][a-z0-9]*)+");

	private McpJson()
	{
	}

	/**
	 * Renames camelCase keys to snake_case, recursively, so results that come from the REST
	 * handlers read the same as the MCP-only tools. Keys that are not plain camelCase words
	 * (class names, quest enum names, profile names) are left alone.
	 */
	public static JsonElement snake(JsonElement e)
	{
		if (e == null)
		{
			return null;
		}
		if (e.isJsonArray())
		{
			JsonArray out = new JsonArray();
			for (JsonElement x : e.getAsJsonArray())
			{
				out.add(snake(x));
			}
			return out;
		}
		if (e.isJsonObject())
		{
			JsonObject out = new JsonObject();
			for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet())
			{
				out.add(snakeKey(en.getKey()), snake(en.getValue()));
			}
			return out;
		}
		return e;
	}

	static String snakeKey(String key)
	{
		if (!CAMEL.matcher(key).matches())
		{
			return key;
		}
		StringBuilder sb = new StringBuilder(key.length() + 4);
		for (int i = 0; i < key.length(); i++)
		{
			char c = key.charAt(i);
			if (Character.isUpperCase(c))
			{
				sb.append('_').append(Character.toLowerCase(c));
			}
			else
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static Map<String, Object> tile(WorldPoint p)
	{
		if (p == null)
		{
			return null;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("x", p.getX());
		m.put("y", p.getY());
		m.put("plane", p.getPlane());
		return m;
	}

	/** An ordered map from alternating keys and values. */
	public static Map<String, Object> map(Object... kv)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2)
		{
			m.put(String.valueOf(kv[i]), kv[i + 1]);
		}
		return m;
	}
}
