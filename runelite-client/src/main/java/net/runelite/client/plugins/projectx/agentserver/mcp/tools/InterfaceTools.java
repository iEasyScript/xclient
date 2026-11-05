package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.WidgetNode;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;
import net.runelite.client.plugins.projectx.util.menu.NewMenuEntry;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;

/** Widgets (interfaces), the right-click menu and dialogue. */
final class InterfaceTools
{
	private InterfaceTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("interfaces");

		r.add("list_open_interfaces",
			"Visible top-level widgets (root layers) with group_id, child_id, type, text, actions and child counts, "
				+ "plus open_group_ids: every interface group currently open. Paged by limit/offset.",
			Schema.object().integer("limit", "Max widgets (default " + ctx.maxResults() + ")").integer("offset", "Skip this many").build(),
			a ->
			{
				JsonElement list = ctx.rest("GET", "/widgets/list" + McpContext.query("limit", a.intOrNull("limit"), "offset", a.intOrNull("offset")), null);
				if (list.isJsonObject())
				{
					JsonArray ids = new JsonArray();
					openGroups().forEach(ids::add);
					list.getAsJsonObject().add("open_group_ids", ids);
				}
				return ToolResult.json(list);
			});

		r.add("get_widget_tree",
			"A widget and its children down to depth levels (default 3, max 15): group_id, child_id, index, type, "
				+ "text, name, actions, hidden, sprite_id, item_id, child_count, path. depth=0 describes just the widget.",
			Schema.object().integer("group_id", "Interface group id").integer("child_id", "Child id (default 0)")
				.integer("depth", "Levels below the widget (default 3, max 15)", 0, 15).required("group_id").build(),
			a -> ToolResult.json(ctx.rest("GET", "/widgets/describe" + McpContext.query(
				"groupId", a.requireInt("group_id"), "childId", a.integer("child_id", 0), "depth", a.integer("depth", 3, 0, 15)), null)));

		r.add("find_widgets",
			"Search visible widgets by text and/or action (case-insensitive substrings), best match first. "
				+ "Each result has group_id, child_id, index, text, actions, hidden and path.",
			Schema.object().str("text", "Text or name to find").str("action", "Action to find, e.g. 'Withdraw-All'")
				.integer("group_id", "Only in this interface group").integer("limit", "Max results (default 50)").build(),
			a -> ToolResult.json(findWidgets(ctx, a)));

		r.add("click_widget",
			"Click a widget by group_id + child_id, or the first visible widget whose text matches. Refuses hidden "
				+ "widgets. For a specific menu option on a widget use invoke_widget_action.",
			Schema.object().integer("group_id", "Interface group id").integer("child_id", "Child id")
				.str("text", "Visible text to click instead of ids").build(),
			a ->
			{
				if (!(a.has("group_id") && a.has("child_id")) && !a.has("text"))
				{
					throw McpError.invalidParams("Give group_id and child_id, or text");
				}
				return ToolResult.json(ctx.rest("POST", "/widgets/click", ItemTools.body(
					"groupId", a.intOrNull("group_id"), "childId", a.intOrNull("child_id"), "text", a.str("text"))));
			});

		r.add("invoke_widget_action",
			"Fire a widget menu option (CC_OP) directly: group_id, child_id, param0 (dynamic child index or slot, -1 "
				+ "for none), action text and identifier (option number, 1-based). E.g. bank Withdraw-All on a slot.",
			Schema.object().integer("group_id", "Interface group id").integer("child_id", "Child id")
				.integer("param0", "Dynamic child index / slot (default -1)").str("action", "Option text (default 'Select')")
				.integer("identifier", "Option number, 1-based (default 1)").required("group_id", "child_id").build(),
			a -> ToolResult.json(ctx.rest("POST", "/widgets/invoke", ItemTools.body(
				"groupId", a.requireInt("group_id"), "childId", a.requireInt("child_id"), "param0", a.intOrNull("param0"),
				"action", a.str("action"), "identifier", a.intOrNull("identifier")))));

		r.add("get_menu_entries",
			"The current right-click menu entries, top option first: option, target, type, identifier, param0, "
				+ "param1, item_id; plus menu_open. Copy an entry into invoke_menu_action to replay it.",
			Schema.empty(),
			a -> ToolResult.json(menuEntries()));

		r.add("invoke_menu_action",
			"Send a raw menu action, as if that menu entry had been clicked. type is a MenuAction name (e.g. "
				+ "NPC_FIRST_OPTION, GAME_OBJECT_FIRST_OPTION, CC_OP, WALK) or its numeric id. Values usually come from "
				+ "get_menu_entries. Low level: prefer interact_* and click_widget.",
			Schema.object().str("option", "Menu option text").str("target", "Menu target text")
				.str("type", "MenuAction name or id").integer("identifier", "Identifier")
				.integer("param0", "param0").integer("param1", "param1").integer("item_id", "Item id (default -1)")
				.integer("world_view_id", "World view id (default -1, top level)")
				.required("option", "type").build(),
			a -> ToolResult.json(invokeMenu(a)));

		r.add("diff_open_interfaces",
			"Compare the open interface groups with a baseline list: opened, closed, persisted and current. Take "
				+ "the baseline from list_open_interfaces.open_group_ids before an action.",
			Schema.object().intArray("baseline", "Group ids that were open before").required("baseline").build(),
			a ->
			{
				List<Integer> base = a.intList("baseline");
				TreeSet<Integer> now = openGroups();
				List<Integer> opened = new ArrayList<>();
				List<Integer> persisted = new ArrayList<>();
				for (Integer g : now)
				{
					(base.contains(g) ? persisted : opened).add(g);
				}
				List<Integer> closed = new ArrayList<>();
				for (Integer g : base)
				{
					if (!now.contains(g))
					{
						closed.add(g);
					}
				}
				return ToolResult.json(GameTools.map("opened", opened, "closed", closed, "persisted", persisted, "current", now));
			});

		r.category("dialogue");

		r.add("get_dialogue",
			"Dialogue state: in_dialogue, has_continue, has_options, dialogue_text, question and options[] (when a "
				+ "choice is shown).",
			Schema.empty(),
			a -> ToolResult.json(ctx.rest("GET", "/dialogue", null)));

		r.add("continue_dialogue", "Press 'Click here to continue' in the open dialogue.",
			Schema.empty(),
			a -> ToolResult.json(ctx.rest("POST", "/dialogue/continue", null)));

		r.add("select_dialogue_option",
			"Choose a dialogue option by text (substring) or by its 1-based index (sent as the number key).",
			Schema.object().str("option", "Option text").integer("index", "Option number, 1-based").build(),
			a ->
			{
				if (!a.has("option") && !a.has("index"))
				{
					throw McpError.invalidParams("Give option or index");
				}
				return ToolResult.json(ctx.rest("POST", "/dialogue/select", ItemTools.body("option", a.str("option"), "index", a.intOrNull("index"))));
			});
	}

	/** Interface groups open right now: the top-level interface and every mounted sub-interface. */
	static TreeSet<Integer> openGroups()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			TreeSet<Integer> ids = new TreeSet<>();
			int top = c.getTopLevelInterfaceId();
			if (top != -1)
			{
				ids.add(top);
			}
			for (WidgetNode node : c.getComponentTable())
			{
				if (node != null)
				{
					ids.add(node.getId());
				}
			}
			return ids;
		});
	}

	private static JsonElement findWidgets(McpContext ctx, ToolArgs a) throws Exception
	{
		String text = a.str("text");
		String action = a.str("action");
		Integer group = a.intOrNull("group_id");
		int limit = a.integer("limit", 50, 1, 1000);
		if ((text == null || text.isEmpty()) && (action == null || action.isEmpty()))
		{
			throw McpError.invalidParams("Give text and/or action");
		}
		String q = ((text != null ? text : "") + " " + (action != null ? action : "")).trim();
		JsonElement res = ctx.rest("GET", "/widgets/search" + McpContext.query("q", q, "limit", 5000), null);
		JsonArray out = new JsonArray();
		JsonArray in = res.isJsonObject() && res.getAsJsonObject().has("results") ? res.getAsJsonObject().getAsJsonArray("results") : new JsonArray();
		for (JsonElement e : in)
		{
			JsonObject w = e.getAsJsonObject();
			if (group != null && (!w.has("group_id") || w.get("group_id").getAsInt() != group))
			{
				continue;
			}
			if (text != null && !text.isEmpty() && !contains(w, "text", text) && !contains(w, "name", text))
			{
				continue;
			}
			if (action != null && !action.isEmpty() && !containsAction(w, action))
			{
				continue;
			}
			out.add(w);
			if (out.size() >= limit)
			{
				break;
			}
		}
		JsonObject o = new JsonObject();
		o.addProperty("count", out.size());
		o.add("items", out);
		return o;
	}

	private static boolean contains(JsonObject w, String field, String needle)
	{
		return w.has(field) && !w.get(field).isJsonNull() && w.get(field).getAsString().toLowerCase().contains(needle.toLowerCase());
	}

	private static boolean containsAction(JsonObject w, String needle)
	{
		if (!w.has("actions") || !w.get("actions").isJsonArray())
		{
			return false;
		}
		for (JsonElement x : w.getAsJsonArray("actions"))
		{
			if (!x.isJsonNull() && x.getAsString().toLowerCase().contains(needle.toLowerCase()))
			{
				return true;
			}
		}
		return false;
	}

	private static Map<String, Object> menuEntries()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			MenuEntry[] entries = c.getMenu().getMenuEntries();
			List<Map<String, Object>> items = new ArrayList<>();
			for (int i = entries.length - 1; i >= 0; i--)
			{
				MenuEntry e = entries[i];
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("option", SceneJson.clean(e.getOption()));
				m.put("target", SceneJson.clean(e.getTarget()));
				m.put("type", e.getType() != null ? e.getType().name() : null);
				m.put("identifier", e.getIdentifier());
				m.put("param0", e.getParam0());
				m.put("param1", e.getParam1());
				m.put("item_id", e.getItemId());
				m.put("world_view_id", e.getWorldViewId());
				items.add(m);
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("menu_open", c.isMenuOpen());
			out.put("count", items.size());
			out.put("items", items);
			return out;
		});
	}

	private static Map<String, Object> invokeMenu(ToolArgs a)
	{
		String typeArg = a.requireStr("type");
		MenuAction type;
		try
		{
			type = typeArg.matches("-?\\d+") ? MenuAction.of(Integer.parseInt(typeArg)) : MenuAction.valueOf(typeArg.trim().toUpperCase());
		}
		catch (IllegalArgumentException e)
		{
			throw McpError.invalidParams("Unknown MenuAction: " + typeArg);
		}
		if (type == null || type == MenuAction.UNKNOWN)
		{
			throw McpError.invalidParams("Unknown MenuAction: " + typeArg);
		}
		NewMenuEntry entry = new NewMenuEntry()
			.option(a.requireStr("option"))
			.target(a.str("target", ""))
			.identifier(a.integer("identifier", 0))
			.type(type)
			.param0(a.integer("param0", 0))
			.param1(a.integer("param1", 0))
			.itemId(a.integer("item_id", -1))
			.worldViewId(a.integer("world_view_id", -1))
			.forceLeftClick(false);
		ProjectX.doInvoke(entry, new Rectangle(1, 1));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("invoked", true);
		out.put("type", type.name());
		out.put("option", a.str("option"));
		return out;
	}
}
