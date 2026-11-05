package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonObject;

/**
 * One MCP tool: a name, a category (for {@code list_tools}), a description, a JSON Schema for its
 * arguments and the code that runs it. Tool bodies run on the HTTP request thread, never on the
 * client thread: they read game state through {@link McpContext#onClient} and may click and wait.
 */
public final class McpTool
{
	@FunctionalInterface
	public interface Body
	{
		ToolResult call(ToolArgs args) throws Exception;
	}

	private final String name;
	private final String category;
	private final String description;
	private final JsonObject inputSchema;
	private final Body body;

	public McpTool(String name, String category, String description, JsonObject inputSchema, Body body)
	{
		if (name == null || !name.matches("[a-z][a-z0-9_]{0,63}"))
		{
			throw new IllegalArgumentException("Bad tool name: " + name);
		}
		if (description == null || description.isBlank())
		{
			throw new IllegalArgumentException("Tool " + name + " has no description");
		}
		this.name = name;
		this.category = category;
		this.description = description;
		this.inputSchema = inputSchema != null ? inputSchema : Schema.empty();
		this.body = body;
	}

	public String getName()
	{
		return name;
	}

	public String getCategory()
	{
		return category;
	}

	public String getDescription()
	{
		return description;
	}

	public JsonObject getInputSchema()
	{
		return inputSchema;
	}

	public Body getBody()
	{
		return body;
	}

	public JsonObject toListEntry()
	{
		JsonObject o = new JsonObject();
		o.addProperty("name", name);
		o.addProperty("description", description);
		o.add("inputSchema", inputSchema);
		return o;
	}
}
