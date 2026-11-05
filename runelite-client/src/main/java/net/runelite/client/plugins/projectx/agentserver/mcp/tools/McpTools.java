package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;

/** Registers every MCP tool, by category. */
public final class McpTools
{
	private McpTools()
	{
	}

	public static McpToolRegistry registerAll(McpToolRegistry r, McpContext ctx)
	{
		GameTools.register(r, ctx);
		ItemTools.register(r, ctx);
		WorldTools.register(r, ctx);
		MovementTools.register(r, ctx);
		InterfaceTools.register(r, ctx);
		InputTools.register(r, ctx);
		VarTools.register(r, ctx);
		WaitTools.register(r, ctx);
		LogTools.register(r, ctx);
		ScriptTools.register(r, ctx);
		AccountTools.register(r, ctx);
		MetaTools.register(r, ctx);
		return r;
	}
}
