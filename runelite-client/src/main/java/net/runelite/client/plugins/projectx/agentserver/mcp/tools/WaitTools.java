package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.widgets.Widget;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.ChatLog;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;

/**
 * Waits for game state. Each runs on the MCP request thread (polling with short client-thread
 * reads), never on the client thread, and every one has a timeout capped at
 * {@link McpContext#MAX_WAIT_MS}.
 */
final class WaitTools
{
	private WaitTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("waits");

		r.add("wait_for_widget",
			"Wait until a widget is visible (default) or hidden. Returns matched, timed_out, elapsed_ms.",
			Schema.object().integer("group_id", "Interface group id").integer("child_id", "Child id (default 0)")
				.enumStr("state", "visible (default) or hidden", "visible", "hidden")
				.integer("timeout_ms", "Max wait (default 5000, max 120000)").required("group_id").build(),
			a -> ToolResult.json(waitWidget(a)));

		r.add("wait_for_interface_open",
			"Wait until an interface group opens: a given group_id, or any group not in exclude_ids (default: the "
				+ "groups open when the call starts). Returns opened (new group ids), timed_out, elapsed_ms.",
			Schema.object().integer("group_id", "Wait for this group").intArray("exclude_ids", "Baseline groups to ignore")
				.integer("timeout_ms", "Max wait (default 3000, max 120000)").build(),
			a -> ToolResult.json(waitInterface(a)));

		r.add("wait_for_idle",
			"Wait until the player stands still: no animation, not moving and (unless ignore_interacting) no target, "
				+ "for stable_ms in a row. Use after an action to let it finish.",
			Schema.object().integer("timeout_ms", "Max wait (default 10000, max 120000)")
				.integer("stable_ms", "How long it must stay idle (default 600)")
				.bool("ignore_interacting", "Count the player as idle even with a target (default false)").build(),
			a -> ToolResult.json(waitIdle(a)));

		r.add("wait_for_chat_message",
			"Wait for a chat message matching a regex (case-insensitive), e.g. 'You catch|You fail'. Only messages "
				+ "after the call starts count, unless lookback_ms reaches back. Returns matched, message, timed_out.",
			Schema.object().str("pattern", "Regular expression").str("type", "Only this ChatMessageType, e.g. GAMEMESSAGE")
				.integer("timeout_ms", "Max wait (default 10000, max 120000)")
				.integer("lookback_ms", "Also accept messages this recent (default 0)").required("pattern").build(),
			a -> ToolResult.json(waitChat(ctx.chatLog(), a)));
	}

	private static Map<String, Object> waitWidget(ToolArgs a) throws InterruptedException
	{
		int group = a.requireInt("group_id");
		int child = a.integer("child_id", 0);
		boolean wantVisible = !"hidden".equals(a.str("state", "visible"));
		int timeout = a.integer("timeout_ms", 5000, 0, McpContext.MAX_WAIT_MS);
		long start = System.currentTimeMillis();
		boolean matched = McpContext.waitUntil(() -> visible(group, child) == wantVisible, timeout, 100);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("group_id", group);
		out.put("child_id", child);
		out.put("state", wantVisible ? "visible" : "hidden");
		out.put("matched", matched);
		out.put("timed_out", !matched);
		out.put("elapsed_ms", System.currentTimeMillis() - start);
		return out;
	}

	static boolean visible(int group, int child)
	{
		Boolean v = onClient(() ->
		{
			Widget w = ProjectX.getClient().getWidget(group, child);
			return w != null && !w.isHidden();
		});
		return Boolean.TRUE.equals(v);
	}

	private static Map<String, Object> waitInterface(ToolArgs a) throws InterruptedException
	{
		Integer group = a.intOrNull("group_id");
		int timeout = a.integer("timeout_ms", 3000, 0, McpContext.MAX_WAIT_MS);
		List<Integer> exclude = a.has("exclude_ids") ? a.intList("exclude_ids") : new ArrayList<>(InterfaceTools.openGroups());
		long start = System.currentTimeMillis();
		List<Integer> opened = new ArrayList<>();
		AtomicReference<TreeSet<Integer>> current = new AtomicReference<>(new TreeSet<>());
		boolean matched = McpContext.waitUntil(() ->
		{
			current.set(InterfaceTools.openGroups());
			opened.clear();
			for (Integer g : current.get())
			{
				if (!exclude.contains(g))
				{
					opened.add(g);
				}
			}
			return group != null ? current.get().contains(group) : !opened.isEmpty();
		}, timeout, 50);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("matched", matched);
		out.put("timed_out", !matched);
		out.put("elapsed_ms", System.currentTimeMillis() - start);
		out.put("opened", opened);
		out.put("current", current.get());
		return out;
	}

	private static Map<String, Object> waitIdle(ToolArgs a) throws InterruptedException
	{
		int timeout = a.integer("timeout_ms", 10000, 0, McpContext.MAX_WAIT_MS);
		int stable = a.integer("stable_ms", 600, 0, 30000);
		boolean ignoreTarget = a.bool("ignore_interacting", false);
		long start = System.currentTimeMillis();
		long[] idleSince = {-1};
		boolean idle = McpContext.waitUntil(() ->
		{
			boolean now = Boolean.TRUE.equals(onClient(() ->
			{
				Client c = ProjectX.getClient();
				Player me = c.getLocalPlayer();
				return me != null && me.getAnimation() == -1 && !SceneJson.isMoving(me)
					&& (ignoreTarget || me.getInteracting() == null);
			}));
			long t = System.currentTimeMillis();
			if (!now)
			{
				idleSince[0] = -1;
				return false;
			}
			if (idleSince[0] < 0)
			{
				idleSince[0] = t;
			}
			return t - idleSince[0] >= stable;
		}, timeout, 100);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("idle", idle);
		out.put("timed_out", !idle);
		out.put("elapsed_ms", System.currentTimeMillis() - start);
		out.put("player", GameTools.localPlayer());
		return out;
	}

	private static Map<String, Object> waitChat(ChatLog log, ToolArgs a) throws InterruptedException
	{
		Pattern pattern;
		try
		{
			pattern = Pattern.compile(a.requireStr("pattern"), Pattern.CASE_INSENSITIVE);
		}
		catch (PatternSyntaxException e)
		{
			throw McpError.invalidParams("Bad regex: " + e.getDescription());
		}
		String type = a.str("type");
		int timeout = a.integer("timeout_ms", 10000, 0, McpContext.MAX_WAIT_MS);
		long lookback = a.integer("lookback_ms", 0, 0, 3_600_000);
		long start = System.currentTimeMillis();
		long seen = log.lastSeq();
		if (lookback > 0)
		{
			for (ChatLog.Entry e : log.after(0))
			{
				if (e.timeMs >= start - lookback)
				{
					seen = e.seq - 1;
					break;
				}
			}
		}
		long deadline = start + timeout;
		while (true)
		{
			for (ChatLog.Entry e : log.after(seen))
			{
				seen = e.seq;
				if (type != null && !type.isEmpty() && !type.equalsIgnoreCase(e.type))
				{
					continue;
				}
				if (pattern.matcher(e.message).find())
				{
					Map<String, Object> out = new LinkedHashMap<>();
					out.put("matched", true);
					out.put("timed_out", false);
					out.put("elapsed_ms", System.currentTimeMillis() - start);
					out.put("message", e.toMap());
					return out;
				}
			}
			long left = deadline - System.currentTimeMillis();
			if (left <= 0)
			{
				Map<String, Object> out = new LinkedHashMap<>();
				out.put("matched", false);
				out.put("timed_out", true);
				out.put("elapsed_ms", System.currentTimeMillis() - start);
				return out;
			}
			log.awaitNew(seen, Math.min(left, 250));
		}
	}
}
