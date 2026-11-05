package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Client;
import net.runelite.api.VarbitComposition;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;

/** Varps, varbits, client vars, and waiting for them to change. */
final class VarTools
{
	/** Config archive holding the varbit definitions. */
	private static final int VARBIT_ARCHIVE = 14;
	/** Varbits looked up per client-thread call while building the varp index, so the game never stalls. */
	private static final int INDEX_CHUNK = 1500;

	/** base varp -> {varbit id, lsb, msb}; built on first use. */
	private static volatile Map<Integer, List<int[]>> varbitsByVarp;

	private VarTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("variables");

		r.add("get_varp", "Read a player variable (varp / VarPlayer) by id.",
			Schema.object().integer("id", "Varp id").required("id").build(),
			a -> ToolResult.json(ctx.rest("GET", "/varp" + McpContext.query("id", a.requireInt("id")), null)));

		r.add("get_varbit",
			"Read a varbit by id, with where it lives: value, base_varp, lsb, msb and the raw varp value.",
			Schema.object().integer("id", "Varbit id").required("id").build(),
			a -> ToolResult.json(varbit(a.requireInt("id"))));

		r.add("get_var_client", "Read a client variable (VarClientInt, or VarClientStr with type=string).",
			Schema.object().integer("id", "VarClient id").enumStr("type", "int (default) or string", "int", "string")
				.required("id").build(),
			a ->
			{
				int id = a.requireInt("id");
				boolean str = "string".equals(a.str("type", "int"));
				Object value = onClient(() -> str ? ProjectX.getClient().getVarcStrValue(id) : (Object) ProjectX.getClient().getVarcIntValue(id));
				return ToolResult.json(GameTools.map("id", id, "type", str ? "string" : "int", "value", value));
			});

		r.add("list_varps",
			"Varp values in an id range (default only non-zero ones). Handy to diff before/after an action.",
			Schema.object().integer("from", "First id (default 0)").integer("to", "Last id (default all)")
				.bool("non_zero_only", "Skip zero values (default true)").integer("limit", "Max items (default 500)").build(),
			a -> ToolResult.json(listVarps(a)));

		r.add("list_varbits_for_varp",
			"Every varbit stored in one varp: varbit_id, lsb, msb and its current value. The first call builds an "
				+ "index from the cache (a second or two).",
			Schema.object().integer("varp", "Base varp id").required("varp").build(),
			a -> ToolResult.json(varbitsForVarp(a.requireInt("varp"))));

		r.category("waits");

		r.add("wait_for_varbit_change",
			"Wait until a varbit differs from baseline (default: its value when the call starts). Returns changed, "
				+ "timed_out, elapsed_ms, baseline, value.",
			Schema.object().integer("id", "Varbit id").integer("baseline", "Value to compare against")
				.integer("timeout_ms", "Max wait (default 3000, max 120000)").integer("poll_ms", "Poll interval (default 50)")
				.required("id").build(),
			a -> ToolResult.json(waitVar(a, true)));

		r.add("wait_for_varp_change",
			"Wait until a varp differs from baseline (default: its value when the call starts).",
			Schema.object().integer("id", "Varp id").integer("baseline", "Value to compare against")
				.integer("timeout_ms", "Max wait (default 3000, max 120000)").integer("poll_ms", "Poll interval (default 50)")
				.required("id").build(),
			a -> ToolResult.json(waitVar(a, false)));
	}

	private static Map<String, Object> varbit(int id)
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			VarbitComposition vc = c.getVarbit(id);
			if (vc == null)
			{
				throw new ToolFailure("No varbit " + id);
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("varbit_id", id);
			m.put("value", c.getVarbitValue(id));
			m.put("base_varp", vc.getIndex());
			m.put("lsb", vc.getLeastSignificantBit());
			m.put("msb", vc.getMostSignificantBit());
			m.put("raw_varp_value", c.getVarpValue(vc.getIndex()));
			return m;
		});
	}

	private static Map<String, Object> listVarps(ToolArgs a)
	{
		int from = Math.max(0, a.integer("from", 0));
		Integer to = a.intOrNull("to");
		boolean nonZero = a.bool("non_zero_only", true);
		int limit = a.integer("limit", 500, 1, 10000);
		int[] varps = onClient(() -> ProjectX.getClient().getVarps().clone());
		List<Map<String, Object>> items = new ArrayList<>();
		int last = to == null ? varps.length - 1 : Math.min(to, varps.length - 1);
		int matched = 0;
		for (int i = from; i <= last; i++)
		{
			if (nonZero && varps[i] == 0)
			{
				continue;
			}
			matched++;
			if (items.size() < limit)
			{
				items.add(GameTools.map("id", i, "value", varps[i]));
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("count", items.size());
		out.put("matched", matched);
		out.put("truncated", matched > items.size());
		out.put("items", items);
		return out;
	}

	private static Map<Integer, List<int[]>> varbitIndex()
	{
		Map<Integer, List<int[]>> index = varbitsByVarp;
		if (index != null)
		{
			return index;
		}
		synchronized (VarTools.class)
		{
			if (varbitsByVarp != null)
			{
				return varbitsByVarp;
			}
			int[] ids = onClient(() -> ProjectX.getClient().getIndexConfig().getFileIds(VARBIT_ARCHIVE));
			if (ids == null)
			{
				throw new ToolFailure("Varbit definitions are not available yet");
			}
			Map<Integer, List<int[]>> built = new HashMap<>();
			for (int start = 0; start < ids.length; start += INDEX_CHUNK)
			{
				int from = start;
				int to = Math.min(ids.length, start + INDEX_CHUNK);
				List<int[]> chunk = onClient(() ->
				{
					Client c = ProjectX.getClient();
					List<int[]> out = new ArrayList<>();
					for (int i = from; i < to; i++)
					{
						VarbitComposition vc = c.getVarbit(ids[i]);
						if (vc != null)
						{
							out.add(new int[]{ids[i], vc.getIndex(), vc.getLeastSignificantBit(), vc.getMostSignificantBit()});
						}
					}
					return out;
				});
				for (int[] v : chunk)
				{
					built.computeIfAbsent(v[1], k -> new ArrayList<>()).add(new int[]{v[0], v[2], v[3]});
				}
			}
			varbitsByVarp = built;
			return built;
		}
	}

	private static Map<String, Object> varbitsForVarp(int varp)
	{
		if (varp < 0)
		{
			throw McpError.invalidParams("varp must be >= 0");
		}
		List<int[]> list = varbitIndex().getOrDefault(varp, new ArrayList<>());
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			List<Map<String, Object>> items = new ArrayList<>();
			for (int[] v : list)
			{
				items.add(GameTools.map("varbit_id", v[0], "lsb", v[1], "msb", v[2], "value", c.getVarbitValue(v[0])));
			}
			items.sort((x, y) -> Integer.compare((Integer) x.get("lsb"), (Integer) y.get("lsb")));
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("base_varp", varp);
			out.put("raw_varp_value", c.getVarpValue(varp));
			out.put("count", items.size());
			out.put("items", items);
			return out;
		});
	}

	private static int read(boolean varbit, int id)
	{
		Integer v = onClient(() -> varbit ? ProjectX.getClient().getVarbitValue(id) : ProjectX.getClient().getVarpValue(id));
		return v == null ? 0 : v;
	}

	private static Map<String, Object> waitVar(ToolArgs a, boolean varbit) throws InterruptedException
	{
		int id = a.requireInt("id");
		int timeout = a.integer("timeout_ms", 3000, 0, McpContext.MAX_WAIT_MS);
		int poll = a.integer("poll_ms", 50, 10, 5000);
		int baseline = a.has("baseline") ? a.requireInt("baseline") : read(varbit, id);
		long start = System.currentTimeMillis();
		int[] last = {baseline};
		boolean changed = McpContext.waitUntil(() ->
		{
			last[0] = read(varbit, id);
			return last[0] != baseline;
		}, timeout, poll);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put(varbit ? "varbit_id" : "varp_id", id);
		out.put("changed", changed);
		out.put("timed_out", !changed);
		out.put("elapsed_ms", System.currentTimeMillis() - start);
		out.put("baseline", baseline);
		out.put("value", last[0]);
		return out;
	}
}
