package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemComposition;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.projectx.breakhandler.BreakHandlerScript;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2Plugin;
import net.runelite.client.plugins.projectx.breakhandler.breakhandlerv2.BreakHandlerV2State;
import net.runelite.client.plugins.projectx.util.grandexchange.Rs2GrandExchange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;

/** Login, logout, profiles, breaks and the Grand Exchange. */
final class AccountTools
{
	private AccountTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("account");

		r.add("get_login_state",
			"Login status: logged_in, game_state, login_attempt_active, login_cooldown_ms, login_duration_ms, "
				+ "login_index and login_error on the login screen, active_profile (name, members, world) and current_world.",
			Schema.empty(), a -> ToolResult.json(ctx.rest("GET", "/login", null)));

		r.add("login",
			"Log in with the active profile (see list_profiles / switch_profile), optionally to a world. With wait "
				+ "(default true) it returns once logged in and stable, on a fatal login error, or after timeout_s "
				+ "(default 30, max 120). Respects the too-many-attempts cooldown.",
			Schema.object().integer("world", "World number").bool("wait", "Wait for the result (default true)")
				.integer("timeout_s", "Max wait (default 30, max 120)", 1, 120).build(),
			a -> ToolResult.json(ctx.rest("POST", "/login", ItemTools.body("world", a.intOrNull("world"),
				"wait", a.bool("wait", true), "timeout", a.integer("timeout_s", 30, 1, 120)))));

		r.add("logout",
			"Log out. With wait (default true) returns when the login screen shows or after timeout_s (default 30).",
			Schema.object().bool("wait", "Wait for the login screen (default true)")
				.integer("timeout_s", "Max wait (default 30, max 120)", 1, 120).build(),
			a -> ToolResult.json(ctx.rest("POST", "/logout", ItemTools.body("wait", a.bool("wait", true),
				"timeout", a.integer("timeout_s", 30, 1, 120)))));

		r.add("list_profiles", "Login profiles (never passwords): name, is_member, selected_world, active.",
			Schema.empty(), a -> ToolResult.json(ctx.rest("GET", "/profiles", null)));

		r.add("switch_profile", "Make a profile active (exact name, case-insensitive); the next login uses it.",
			Schema.object().str("name", "Profile name").required("name").build(),
			a -> ToolResult.json(ctx.rest("POST", "/profiles", ItemTools.body("name", a.requireStr("name")))));

		r.add("get_break_state",
			"Break handlers: for v1 and v2, whether the plugin is enabled, its state and whether a break is active; "
				+ "v1 also break_in_s, break_duration_s, total_breaks and lock_state. Plus scripts_paused.",
			Schema.empty(), a -> ToolResult.json(breakState()));

		r.add("get_grand_exchange",
			"Grand Exchange: open (the GE window is showing) and the 8 offer slots: slot, state (EMPTY, BUYING, BOUGHT, "
				+ "SELLING, SOLD, CANCELLED_*), item_id, item_name, quantity_sold, total_quantity, price, spent.",
			Schema.empty(), a -> ToolResult.json(grandExchange()));
	}

	private static Map<String, Object> breakState()
	{
		Map<String, Object> v1 = new LinkedHashMap<>();
		v1.put("enabled", ProjectX.isPluginEnabled(BreakHandlerPlugin.class));
		v1.put("state", BreakHandlerScript.getCurrentState() != null ? BreakHandlerScript.getCurrentState().name() : null);
		v1.put("break_active", BreakHandlerScript.isBreakActive());
		v1.put("lock_state", BreakHandlerScript.isLockState());
		v1.put("break_in_s", BreakHandlerScript.breakIn);
		v1.put("break_duration_s", BreakHandlerScript.breakDuration);
		v1.put("total_breaks", BreakHandlerScript.totalBreaks);

		Map<String, Object> v2 = new LinkedHashMap<>();
		v2.put("enabled", ProjectX.isPluginEnabled(BreakHandlerV2Plugin.class));
		BreakHandlerV2State s = BreakHandlerV2State.getCurrentState();
		v2.put("state", s != null ? s.name() : null);
		v2.put("break_active", BreakHandlerV2State.isBreakActive());

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("v1", v1);
		out.put("v2", v2);
		out.put("scripts_paused", ProjectX.pauseAllScripts.get());
		return out;
	}

	private static Map<String, Object> grandExchange()
	{
		boolean open = Rs2GrandExchange.isOpen();
		List<Map<String, Object>> offers = onClient(() ->
		{
			Client c = ProjectX.getClient();
			List<Map<String, Object>> out = new ArrayList<>();
			GrandExchangeOffer[] all = c.getGrandExchangeOffers();
			for (int i = 0; all != null && i < all.length; i++)
			{
				GrandExchangeOffer o = all[i];
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("slot", i);
				GrandExchangeOfferState st = o != null ? o.getState() : GrandExchangeOfferState.EMPTY;
				m.put("state", st.name());
				if (o != null && st != GrandExchangeOfferState.EMPTY)
				{
					ItemComposition comp = c.getItemDefinition(o.getItemId());
					m.put("item_id", o.getItemId());
					m.put("item_name", comp != null ? comp.getName() : null);
					m.put("quantity_sold", o.getQuantitySold());
					m.put("total_quantity", o.getTotalQuantity());
					m.put("price", o.getPrice());
					m.put("spent", o.getSpent());
				}
				out.add(m);
			}
			return out;
		});
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("open", open);
		out.put("offers", offers);
		return out;
	}
}
