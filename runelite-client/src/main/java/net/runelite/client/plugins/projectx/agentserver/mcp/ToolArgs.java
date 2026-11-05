package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed, forgiving access to a tool call's {@code arguments}. Numbers sent as strings ("12") and
 * booleans sent as strings ("true") are accepted; anything else of the wrong type is an
 * {@link McpError#invalidParams invalid-params} error.
 */
public final class ToolArgs
{
	private final JsonObject args;

	public ToolArgs(JsonObject args)
	{
		this.args = args != null ? args : new JsonObject();
	}

	public JsonObject raw()
	{
		return args;
	}

	public boolean has(String name)
	{
		JsonElement e = args.get(name);
		return e != null && !e.isJsonNull();
	}

	public JsonElement get(String name)
	{
		return args.get(name);
	}

	public String str(String name)
	{
		JsonElement e = args.get(name);
		if (e == null || e.isJsonNull())
		{
			return null;
		}
		if (e.isJsonPrimitive())
		{
			return e.getAsString();
		}
		throw McpError.invalidParams("Argument '" + name + "' must be a string");
	}

	public String str(String name, String fallback)
	{
		String s = str(name);
		return s == null || s.isEmpty() ? fallback : s;
	}

	public String requireStr(String name)
	{
		String s = str(name);
		if (s == null || s.isEmpty())
		{
			throw McpError.invalidParams("Missing required argument: " + name);
		}
		return s;
	}

	public Integer intOrNull(String name)
	{
		JsonElement e = args.get(name);
		if (e == null || e.isJsonNull())
		{
			return null;
		}
		if (e.isJsonPrimitive())
		{
			JsonPrimitive p = e.getAsJsonPrimitive();
			try
			{
				if (p.isNumber())
				{
					double d = p.getAsDouble();
					if (d != Math.rint(d))
					{
						throw McpError.invalidParams("Argument '" + name + "' must be a whole number");
					}
					return (int) d;
				}
				if (p.isString())
				{
					String s = p.getAsString().trim();
					if (s.isEmpty())
					{
						return null;
					}
					return Integer.parseInt(s);
				}
			}
			catch (NumberFormatException ex)
			{
				// fall through
			}
		}
		throw McpError.invalidParams("Argument '" + name + "' must be an integer");
	}

	public int integer(String name, int fallback)
	{
		Integer v = intOrNull(name);
		return v == null ? fallback : v;
	}

	/** An integer clamped into [min, max]. */
	public int integer(String name, int fallback, int min, int max)
	{
		int v = integer(name, fallback);
		return Math.max(min, Math.min(max, v));
	}

	public int requireInt(String name)
	{
		Integer v = intOrNull(name);
		if (v == null)
		{
			throw McpError.invalidParams("Missing required argument: " + name);
		}
		return v;
	}

	public Boolean boolOrNull(String name)
	{
		JsonElement e = args.get(name);
		if (e == null || e.isJsonNull())
		{
			return null;
		}
		if (e.isJsonPrimitive())
		{
			JsonPrimitive p = e.getAsJsonPrimitive();
			if (p.isBoolean())
			{
				return p.getAsBoolean();
			}
			if (p.isString())
			{
				String s = p.getAsString().trim().toLowerCase();
				if (s.equals("true"))
				{
					return true;
				}
				if (s.equals("false"))
				{
					return false;
				}
			}
		}
		throw McpError.invalidParams("Argument '" + name + "' must be a boolean");
	}

	public boolean bool(String name, boolean fallback)
	{
		Boolean b = boolOrNull(name);
		return b == null ? fallback : b;
	}

	public List<Integer> intList(String name)
	{
		JsonElement e = args.get(name);
		List<Integer> out = new ArrayList<>();
		if (e == null || e.isJsonNull())
		{
			return out;
		}
		if (!e.isJsonArray())
		{
			throw McpError.invalidParams("Argument '" + name + "' must be an array of integers");
		}
		JsonArray a = e.getAsJsonArray();
		for (JsonElement x : a)
		{
			try
			{
				out.add(x.getAsInt());
			}
			catch (RuntimeException ex)
			{
				throw McpError.invalidParams("Argument '" + name + "' must be an array of integers");
			}
		}
		return out;
	}
}
