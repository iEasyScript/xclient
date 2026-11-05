package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.projectx.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.projectx.util.inventory.Rs2ItemModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;

/** Inventory, equipment, bank and item containers; item actions and use-on. */
final class ItemTools
{
	private ItemTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("items");

		r.add("get_inventory",
			"The 28-slot inventory: count, free_slots, full, and items[] {id, name, quantity, slot, stackable, noted, actions}.",
			Schema.empty(),
			a -> ToolResult.json(ctx.rest("GET", "/inventory", null)));

		r.add("get_equipment",
			"Worn equipment: items[] {slot, slot_name (HEAD, WEAPON, ...), id, name, quantity}; empty slots are left out.",
			Schema.empty(),
			a -> ToolResult.json(equipment()));

		r.add("get_bank",
			"Bank contents when the bank is open: open, count and items[] {id, name, quantity}. Filter with name "
				+ "(substring) and limit. When closed only open=false is returned; open it with bank_open.",
			Schema.object().str("name", "Item name substring").integer("limit", "Max items (default 500)").build(),
			a -> ToolResult.json(bank(ctx, a)));

		r.add("list_item_containers",
			"Item containers the client currently holds (inventory, equipment, bank, GE collection, trade, ...): "
				+ "id, name when known, size and used slots. Use get_item_container to read one.",
			Schema.empty(),
			a -> ToolResult.json(containers()));

		r.add("get_item_container",
			"Read any item container by id (see list_item_containers): slot, id, name, quantity for each non-empty slot.",
			Schema.object().integer("id", "Container id").required("id").build(),
			a -> ToolResult.json(container(a.requireInt("id"))));

		r.add("interact_item",
			"Click an option on an inventory item, e.g. 'Eat', 'Wield', 'Drink', 'Bury', 'Empty'. Target by name or id.",
			Schema.object().str("name", "Item name").integer("id", "Item id").str("action", "Option, e.g. 'Eat'")
				.required("action").build(),
			a ->
			{
				requireItem(a);
				return ToolResult.json(ctx.rest("POST", "/inventory/interact",
					body("name", a.str("name"), "id", a.intOrNull("id"), "action", a.requireStr("action"))));
			});

		r.add("drop_item",
			"Drop one of an inventory item by name, or every one with all=true.",
			Schema.object().str("name", "Item name").bool("all", "Drop every matching item (default false)")
				.required("name").build(),
			a -> ToolResult.json(ctx.rest("POST", "/inventory/drop",
				body("name", a.requireStr("name"), "all", a.bool("all", false)))));

		r.add("use_item_on",
			"Use an inventory item on another inventory item, an NPC or an object (e.g. tinderbox on logs, bones on an "
				+ "altar). Item by item_name/item_id; target by target_type plus target_name/target_id.",
			Schema.object().str("item_name", "Item to use").integer("item_id", "Item id to use")
				.enumStr("target_type", "What to use it on", "item", "npc", "object")
				.str("target_name", "Target name (item/NPC/object)").integer("target_id", "Target id")
				.integer("max_distance", "NPC/object search radius (default 20)", 0, 100)
				.required("target_type").build(),
			a -> ToolResult.json(useItemOn(a)));

		r.add("bank_open", "Open the nearest bank (booth, chest or banker) and wait up to 5 s for it.",
			Schema.empty(), a -> ToolResult.json(ctx.rest("POST", "/bank/open", null)));

		r.add("bank_close", "Close the bank window if it is open.", Schema.empty(),
			a -> ToolResult.json(ctx.rest("POST", "/bank/close", null)));

		r.add("bank_deposit",
			"Deposit every item with this name, or the whole inventory with all=true and no name.",
			Schema.object().str("name", "Item name").bool("all", "Deposit the whole inventory").build(),
			a ->
			{
				if (!a.has("name") && !a.bool("all", false))
				{
					throw McpError.invalidParams("Give name, or all=true");
				}
				return ToolResult.json(ctx.rest("POST", "/bank/deposit", body("name", a.str("name"), "all", a.bool("all", false))));
			});

		r.add("bank_withdraw",
			"Withdraw an item by name or id. quantity 0 or less withdraws all. Note: a request for more than the bank "
				+ "stack holds can silently do nothing.",
			Schema.object().str("name", "Item name").integer("id", "Item id").integer("quantity", "How many (default 1; 0 = all)").build(),
			a ->
			{
				requireItem(a);
				return ToolResult.json(ctx.rest("POST", "/bank/withdraw",
					body("name", a.str("name"), "id", a.intOrNull("id"), "quantity", a.integer("quantity", 1))));
			});
	}

	private static void requireItem(ToolArgs a)
	{
		if (!a.has("name") && !a.has("id"))
		{
			throw McpError.invalidParams("Give name or id");
		}
	}

	static Map<String, Object> body(Object... kv)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2)
		{
			if (kv[i + 1] != null)
			{
				m.put(String.valueOf(kv[i]), kv[i + 1]);
			}
		}
		return m;
	}

	private static JsonElement bank(McpContext ctx, ToolArgs a) throws Exception
	{
		JsonElement json = ctx.rest("GET", "/bank", null);
		String name = a.str("name");
		int limit = a.integer("limit", 500, 1, 5000);
		if (!json.isJsonObject() || !json.getAsJsonObject().has("items"))
		{
			return json;
		}
		JsonObject o = json.getAsJsonObject();
		JsonArray in = o.getAsJsonArray("items");
		JsonArray out = new JsonArray();
		int matched = 0;
		for (JsonElement e : in)
		{
			String n = e.getAsJsonObject().has("name") && !e.getAsJsonObject().get("name").isJsonNull()
				? e.getAsJsonObject().get("name").getAsString() : "";
			if (name != null && !name.isEmpty() && !n.toLowerCase().contains(name.toLowerCase()))
			{
				continue;
			}
			matched++;
			if (out.size() < limit)
			{
				out.add(e);
			}
		}
		o.add("items", out);
		o.addProperty("matched", matched);
		o.addProperty("truncated", matched > out.size());
		return o;
	}

	private static Map<String, Object> equipment()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			ItemContainer worn = c.getItemContainer(InventoryID.EQUIPMENT);
			List<Map<String, Object>> items = new ArrayList<>();
			if (worn != null)
			{
				for (EquipmentInventorySlot slot : EquipmentInventorySlot.values())
				{
					Item item = worn.getItem(slot.getSlotIdx());
					if (item == null || item.getId() <= 0)
					{
						continue;
					}
					ItemComposition comp = c.getItemDefinition(item.getId());
					Map<String, Object> m = new LinkedHashMap<>();
					m.put("slot", slot.getSlotIdx());
					m.put("slot_name", slot.name());
					m.put("id", item.getId());
					m.put("name", comp != null ? comp.getName() : null);
					m.put("quantity", item.getQuantity());
					items.add(m);
				}
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("count", items.size());
			out.put("items", items);
			return out;
		});
	}

	private static Map<String, Object> containers()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Map<Integer, String> names = new LinkedHashMap<>();
			for (InventoryID id : InventoryID.values())
			{
				names.put(id.getId(), id.name());
			}
			List<Map<String, Object>> items = new ArrayList<>();
			for (ItemContainer ic : c.getItemContainers())
			{
				if (ic == null)
				{
					continue;
				}
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", ic.getId());
				m.put("name", names.get(ic.getId()));
				m.put("size", ic.size());
				m.put("used", ic.count());
				items.add(m);
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("count", items.size());
			out.put("items", items);
			return out;
		});
	}

	private static Map<String, Object> container(int id)
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			ItemContainer ic = c.getItemContainer(id);
			if (ic == null)
			{
				throw new ToolFailure("Container " + id + " is not loaded");
			}
			List<Map<String, Object>> items = new ArrayList<>();
			Item[] all = ic.getItems();
			for (int slot = 0; slot < all.length; slot++)
			{
				Item item = all[slot];
				if (item == null || item.getId() <= 0)
				{
					continue;
				}
				ItemComposition comp = c.getItemDefinition(item.getId());
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("slot", slot);
				m.put("id", item.getId());
				m.put("name", comp != null ? comp.getName() : null);
				m.put("quantity", item.getQuantity());
				items.add(m);
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("id", id);
			out.put("size", ic.size());
			out.put("count", items.size());
			out.put("items", items);
			return out;
		});
	}

	private static Rs2ItemModel inventoryItem(String name, Integer id, String what)
	{
		Rs2ItemModel item = null;
		if (id != null)
		{
			item = Rs2Inventory.get(id);
		}
		else if (name != null && !name.isEmpty())
		{
			item = Rs2Inventory.get(name, true);
			if (item == null)
			{
				item = Rs2Inventory.get(name, false);
			}
		}
		if (item == null)
		{
			throw new ToolFailure(what + " not in the inventory: " + (id != null ? "id " + id : name));
		}
		return item;
	}

	private static Map<String, Object> useItemOn(ToolArgs a) throws Exception
	{
		String type = a.requireStr("target_type");
		if (!a.has("item_name") && !a.has("item_id"))
		{
			throw McpError.invalidParams("Give item_name or item_id");
		}
		if (!a.has("target_name") && !a.has("target_id"))
		{
			throw McpError.invalidParams("Give target_name or target_id");
		}
		Rs2ItemModel item = inventoryItem(a.str("item_name"), a.intOrNull("item_id"), "Item");
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("item", body("id", item.getId(), "name", item.getName(), "slot", item.getSlot()));
		boolean ok;
		switch (type)
		{
			case "item":
			{
				Rs2ItemModel target = inventoryItem(a.str("target_name"), a.intOrNull("target_id"), "Target item");
				ok = Rs2Inventory.combine(item, target);
				out.put("target", body("id", target.getId(), "name", target.getName(), "slot", target.getSlot()));
				break;
			}
			case "npc":
			{
				ToolArgs search = searchArgs(a);
				Object[] hit = onClient(() ->
				{
					WorldTools.Filter f = new WorldTools.Filter(search, 50);
					Client c = ProjectX.getClient();
					Rs2NpcModel best = null;
					Map<String, Object> bestInfo = null;
					for (Rs2NpcModel model : ProjectX.getRs2NpcCache().query().toList())
					{
						if (model.getNpc() == null)
						{
							continue;
						}
						Map<String, Object> m = SceneJson.npc(c, model.getNpc(), SceneJson.myTile(c), false);
						if (f.idOk((Integer) m.get("id"), model.getNpc().getId()) && f.nameOk((String) m.get("name"))
							&& f.placeOk(model.getNpc().getWorldLocation(), (Integer) m.get("distance"))
							&& (bestInfo == null || f.order().compare(m, bestInfo) < 0))
						{
							best = model;
							bestInfo = m;
						}
					}
					return best == null ? null : new Object[]{best, bestInfo};
				});
				if (hit == null)
				{
					throw new ToolFailure("No matching NPC nearby");
				}
				ok = Rs2Inventory.useItemOnNpc(item.getId(), new net.runelite.client.plugins.projectx.util.npc.Rs2NpcModel(((Rs2NpcModel) hit[0]).getNpc()));
				out.put("target", hit[1]);
				break;
			}
			case "object":
			{
				Map<String, Object> list = WorldTools.listObjects(searchArgs(a), 1);
				List<?> items = (List<?>) list.get("items");
				if (items == null || items.isEmpty())
				{
					throw new ToolFailure("No matching object nearby");
				}
				Map<String, Object> target = WorldTools.cast(items.get(0));
				ok = Rs2Inventory.useItemOnObject(item.getId(), (Integer) target.get("id"));
				out.put("target", target);
				break;
			}
			default:
				throw McpError.invalidParams("target_type must be item, npc or object");
		}
		out.put("success", ok);
		if (!ok)
		{
			throw new ToolFailure("Use did not go through", new com.google.gson.Gson().toJsonTree(out));
		}
		return out;
	}

	private static ToolArgs searchArgs(ToolArgs a)
	{
		JsonObject o = new JsonObject();
		if (a.has("target_name"))
		{
			o.addProperty("name", a.str("target_name"));
		}
		if (a.has("target_id"))
		{
			o.addProperty("id", a.intOrNull("target_id"));
		}
		o.addProperty("max_distance", a.integer("max_distance", 20, 0, 100));
		return new ToolArgs(o);
	}
}
