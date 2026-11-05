package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The tools the MCP server offers, in registration order. */
public final class McpToolRegistry
{
	private final Map<String, McpTool> tools = new LinkedHashMap<>();
	private String category = "misc";

	/** Category given to the tools added after this call. */
	public McpToolRegistry category(String category)
	{
		this.category = category;
		return this;
	}

	public McpToolRegistry add(String name, String description, JsonObject inputSchema, McpTool.Body body)
	{
		return add(new McpTool(name, category, description, inputSchema, body));
	}

	public synchronized McpToolRegistry add(McpTool tool)
	{
		if (tools.containsKey(tool.getName()))
		{
			throw new IllegalArgumentException("Duplicate tool: " + tool.getName());
		}
		tools.put(tool.getName(), tool);
		return this;
	}

	public synchronized McpTool get(String name)
	{
		return tools.get(name);
	}

	public synchronized Collection<McpTool> all()
	{
		return Collections.unmodifiableList(new ArrayList<>(tools.values()));
	}

	public synchronized List<String> categories()
	{
		List<String> out = new ArrayList<>();
		for (McpTool t : tools.values())
		{
			if (!out.contains(t.getCategory()))
			{
				out.add(t.getCategory());
			}
		}
		return out;
	}

	public synchronized int size()
	{
		return tools.size();
	}
}
