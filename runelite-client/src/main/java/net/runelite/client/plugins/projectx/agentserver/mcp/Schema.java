package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Small builder for a tool's JSON Schema {@code inputSchema} (always an object schema).
 */
public final class Schema
{
	private final JsonObject properties = new JsonObject();
	private final JsonArray required = new JsonArray();

	private Schema()
	{
	}

	public static Schema object()
	{
		return new Schema();
	}

	/** An object schema with no arguments. */
	public static JsonObject empty()
	{
		return new Schema().build();
	}

	public Schema str(String name, String description)
	{
		return prop(name, "string", description);
	}

	public Schema integer(String name, String description)
	{
		return prop(name, "integer", description);
	}

	public Schema integer(String name, String description, int min, int max)
	{
		JsonObject p = typed("integer", description);
		p.addProperty("minimum", min);
		p.addProperty("maximum", max);
		properties.add(name, p);
		return this;
	}

	public Schema bool(String name, String description)
	{
		return prop(name, "boolean", description);
	}

	public Schema number(String name, String description)
	{
		return prop(name, "number", description);
	}

	public Schema enumStr(String name, String description, String... values)
	{
		JsonObject p = typed("string", description);
		JsonArray e = new JsonArray();
		for (String v : values)
		{
			e.add(v);
		}
		p.add("enum", e);
		properties.add(name, p);
		return this;
	}

	public Schema intArray(String name, String description)
	{
		JsonObject p = typed("array", description);
		JsonObject items = new JsonObject();
		items.addProperty("type", "integer");
		p.add("items", items);
		properties.add(name, p);
		return this;
	}

	/** A value that may be a string, number, boolean or null (plugin settings). */
	public Schema scalar(String name, String description)
	{
		JsonObject p = new JsonObject();
		JsonArray types = new JsonArray();
		types.add("string");
		types.add("number");
		types.add("boolean");
		types.add("null");
		p.add("type", types);
		p.addProperty("description", description);
		properties.add(name, p);
		return this;
	}

	public Schema required(String... names)
	{
		for (String n : names)
		{
			required.add(n);
		}
		return this;
	}

	public JsonObject build()
	{
		JsonObject s = new JsonObject();
		s.addProperty("type", "object");
		s.add("properties", properties.deepCopy());
		if (required.size() > 0)
		{
			s.add("required", required.deepCopy());
		}
		return s;
	}

	private Schema prop(String name, String type, String description)
	{
		properties.add(name, typed(type, description));
		return this;
	}

	private static JsonObject typed(String type, String description)
	{
		JsonObject p = new JsonObject();
		p.addProperty("type", type);
		p.addProperty("description", description);
		return p;
	}
}
