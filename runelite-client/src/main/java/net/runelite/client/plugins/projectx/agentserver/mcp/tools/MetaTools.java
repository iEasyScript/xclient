package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import com.google.gson.JsonElement;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.plugins.projectx.ProjectXSessionLog;
import net.runelite.client.plugins.projectx.agentserver.handler.LocalExchange;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpProtocol;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpTool;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** list_tools, client info, screenshots and the settings search shortcut. */
final class MetaTools
{
	private MetaTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("meta");

		r.add("list_tools",
			"Every tool of this server grouped by category, with the first sentence of each description. Pass "
				+ "category to see one group.",
			Schema.object().str("category", "Only this category").build(),
			a -> ToolResult.json(listTools(r, a.str("category"))));

		r.add("get_client_info",
			"Versions and endpoints: Project X client version and commit, script API version, launcher version, "
				+ "Java and OS, the MCP endpoint and protocol, and this session's log file.",
			Schema.empty(),
			a -> ToolResult.json(clientInfo(ctx)));

		r.add("screenshot",
			"A screenshot of the game canvas as a PNG image. max_width scales it down (keeps the aspect ratio). "
				+ "save=true writes it to disk instead (default ~/.runelite/test-results/screenshots) and returns the path.",
			Schema.object().integer("max_width", "Scale down to at most this width in pixels", 100, 4000)
				.bool("save", "Save to a file instead of returning the image").str("label", "File name prefix when saving")
				.str("dir", "Folder when saving").build(),
			a -> screenshot(ctx, a));

		r.add("search_game_settings",
			"Open the in-game Settings, type term into its search box and return the widgets found (to toggle one, "
				+ "click it with click_widget). Same as 'projectx-cli settings search'.",
			Schema.object().str("term", "What to search for").required("term").build(),
			a -> ToolResult.json(searchSettings(ctx, a.requireStr("term"))));
	}

	private static Map<String, Object> listTools(McpToolRegistry r, String only)
	{
		Map<String, Object> categories = new LinkedHashMap<>();
		for (String cat : r.categories())
		{
			if (only != null && !only.isEmpty() && !cat.equalsIgnoreCase(only))
			{
				continue;
			}
			List<Map<String, Object>> tools = new ArrayList<>();
			for (McpTool t : r.all())
			{
				if (t.getCategory().equals(cat))
				{
					tools.add(GameTools.map("name", t.getName(), "summary", firstSentence(t.getDescription())));
				}
			}
			categories.put(cat, tools);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("count", r.size());
		out.put("categories", categories);
		return out;
	}

	static String firstSentence(String s)
	{
		int dot = s.indexOf(". ");
		return dot > 0 ? s.substring(0, dot + 1) : s;
	}

	private static Map<String, Object> clientInfo(McpContext ctx)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("projectx_version", RuneLiteProperties.getProjectXVersion());
		out.put("projectx_commit", RuneLiteProperties.getProjectXCommit());
		out.put("script_api_version", RuneLiteProperties.getProjectXPluginApiVersion());
		out.put("launcher_version", RuneLiteProperties.getLauncherVersion());
		out.put("java", System.getProperty("java.version"));
		out.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
		out.put("mcp_endpoint", ctx.serverDescription());
		out.put("mcp_protocol", McpProtocol.LATEST_PROTOCOL);
		out.put("mcp_protocols_supported", McpProtocol.SUPPORTED_PROTOCOLS);
		File log = ProjectXSessionLog.current();
		out.put("session_log", log != null ? log.getAbsolutePath() : null);
		return out;
	}

	private static ToolResult screenshot(McpContext ctx, ToolArgs a) throws IOException
	{
		if (a.bool("save", false))
		{
			return ToolResult.json(ctx.rest("GET", "/screenshot" + McpContext.query("save", "true",
				"label", a.str("label"), "dir", a.str("dir")), null));
		}
		LocalExchange.Response r = ctx.restRaw("GET", "/screenshot", null);
		if (r.getStatus() != 200 || r.getContentType() == null || !r.getContentType().startsWith("image/png"))
		{
			throw new ToolFailure("No frame was captured (is the game window showing?)");
		}
		byte[] png = r.getBody();
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
		if (img == null)
		{
			throw new ToolFailure("Captured frame could not be read");
		}
		int width = img.getWidth();
		int height = img.getHeight();
		Integer maxWidth = a.intOrNull("max_width");
		if (maxWidth != null && width > maxWidth)
		{
			int h = Math.max(1, Math.round(height * (maxWidth / (float) width)));
			BufferedImage scaled = new BufferedImage(maxWidth, h, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaled.createGraphics();
			try
			{
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(img, 0, 0, maxWidth, h, null);
			}
			finally
			{
				g.dispose();
			}
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			ImageIO.write(scaled, "PNG", bo);
			png = bo.toByteArray();
			width = maxWidth;
			height = h;
		}
		return ToolResult.image(png, GameTools.map("width", width, "height", height, "bytes", png.length,
			"original_width", img.getWidth(), "original_height", img.getHeight()));
	}

	/** Mirrors 'projectx-cli settings search': escape, open the settings search, type, then search widgets. */
	private static JsonElement searchSettings(McpContext ctx, String term) throws IOException, InterruptedException
	{
		ctx.rest("POST", "/keyboard", ItemTools.body("key", "escape"));
		Thread.sleep(500);
		ctx.rest("POST", "/widgets/click", ItemTools.body("groupId", 134, "childId", 11));
		Thread.sleep(500);
		ctx.rest("POST", "/keyboard", ItemTools.body("text", term));
		Thread.sleep(1000);
		return ctx.rest("GET", "/widgets/search" + McpContext.query("q", term, "limit", 20), null);
	}
}
