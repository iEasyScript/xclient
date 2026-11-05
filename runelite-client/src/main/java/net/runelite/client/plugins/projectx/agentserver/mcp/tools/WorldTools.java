package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.api.Client;
import net.runelite.api.GraphicsObject;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Projectile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;
import net.runelite.client.plugins.projectx.api.npc.models.Rs2NpcModel;
import net.runelite.client.plugins.projectx.api.tileitem.models.Rs2TileItemModel;
import net.runelite.client.plugins.projectx.api.tileobject.models.Rs2TileObjectModel;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;
import static net.runelite.client.plugins.projectx.agentserver.mcp.McpJson.tile;

/** NPCs, objects, ground items, players, projectiles and spot anims: list, find closest, interact. */
final class WorldTools
{
	private static final Gson GSON = new GsonBuilder().serializeNulls().create();

	private WorldTools()
	{
	}

	/** The common list filters: name (substring unless exact), id, max_distance, limit, at x/y. */
	static final class Filter
	{
		final String name;
		final boolean exact;
		final Integer id;
		final int maxDistance;
		final int limit;
		final Integer x;
		final Integer y;

		Filter(ToolArgs a, int defaultLimit)
		{
			name = a.str("name");
			exact = a.bool("exact", false);
			id = a.intOrNull("id");
			maxDistance = a.integer("max_distance", 20, 0, 200);
			limit = a.integer("limit", defaultLimit, 1, 5000);
			x = a.intOrNull("x");
			y = a.intOrNull("y");
		}

		boolean nameOk(String n)
		{
			if (name == null || name.isEmpty())
			{
				return true;
			}
			if (n == null)
			{
				return false;
			}
			return exact ? n.equalsIgnoreCase(name) : n.toLowerCase().contains(name.toLowerCase());
		}

		boolean idOk(int... ids)
		{
			if (id == null)
			{
				return true;
			}
			for (int i : ids)
			{
				if (i == id)
				{
					return true;
				}
			}
			return false;
		}

		boolean placeOk(WorldPoint tile, Integer distance)
		{
			if (x != null && (tile == null || tile.getX() != x))
			{
				return false;
			}
			if (y != null && (tile == null || tile.getY() != y))
			{
				return false;
			}
			return distance != null && distance <= maxDistance;
		}

		/** Exact-name matches first, then nearest. */
		Comparator<Map<String, Object>> order()
		{
			return Comparator.<Map<String, Object>>comparingInt(m -> name != null && !exact && !name.equalsIgnoreCase(String.valueOf(m.get("name"))) ? 1 : 0)
				.thenComparingInt(m -> m.get("distance") == null ? Integer.MAX_VALUE : (Integer) m.get("distance"));
		}
	}

	static Schema filterSchema(String what)
	{
		return Schema.object()
			.str("name", what + " name; case-insensitive substring match unless exact=true")
			.bool("exact", "Match the name exactly (case-insensitive). Default false")
			.integer("id", what + " id")
			.integer("max_distance", "Max distance in tiles from the player (default 20)", 0, 200)
			.integer("x", "Only at this tile x (scene 'tile' coordinates)")
			.integer("y", "Only at this tile y (scene 'tile' coordinates)");
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("world");
		int defLimit = ctx.maxResults();

		r.add("list_npcs",
			"List NPCs near the player, nearest first. Each item: index, id, name, combat_level, tile, map_tile "
				+ "(instances only), distance, animation, moving, interacting, interacting_with_me, health_percent, dead, "
				+ "actions. verbose adds pose/orientation/size/graphic. Related: find_closest_npc, interact_npc, get_entity_info.",
			filterSchema("NPC").integer("limit", "Max items (default " + defLimit + ")")
				.bool("interacting_with_me", "Only NPCs whose target is the local player")
				.bool("verbose", "Add pose, orientation, size, graphic and raw health fields").build(),
			a -> ToolResult.json(listNpcs(a, defLimit)));

		r.add("find_closest_npc",
			"The nearest NPC matching name/id (exact-name matches win over substring matches). Returns found=false "
				+ "when none. reachable=true skips NPCs the player cannot walk to.",
			filterSchema("NPC").bool("reachable", "Only NPCs on a reachable tile (default false)").build(),
			a -> ToolResult.json(closest(listNpcs(a, 50), a.bool("reachable", false), "npc")));

		r.add("interact_npc",
			"Click an option on the nearest matching NPC (e.g. action 'Talk-to', 'Attack', 'Bank'). Target by index "
				+ "(from list_npcs), or name and/or id. Returns once the click is sent; check the result with "
				+ "get_local_player, get_dialogue or a wait_for_* tool.",
			filterSchema("NPC").integer("index", "NPC index from list_npcs (most precise)")
				.str("action", "Menu option, e.g. 'Talk-to'").required("action").build(),
			a -> interactNpc(a));

		r.add("list_objects",
			"List game objects (scenery, doors, trees, banks, ...) near the player, nearest first. Each item: id, name, "
				+ "type (GAME/WALL/GROUND/DECORATIVE), tile, map_tile (instances), distance, actions. Unnamed objects are "
				+ "skipped unless include_unnamed=true. Related: find_closest_object, interact_object.",
			filterSchema("Object").integer("limit", "Max items (default " + defLimit + ")")
				.str("action", "Only objects that offer this option (case-insensitive substring)")
				.bool("include_unnamed", "Include objects whose name is 'null' or empty (default false)").build(),
			a -> ToolResult.json(listObjects(a, defLimit)));

		r.add("find_closest_object",
			"The nearest object matching name/id/action. reachable=true only returns an object the player can walk up to.",
			filterSchema("Object").str("action", "Only objects that offer this option")
				.bool("reachable", "Only objects the player can reach (default false)").build(),
			a -> ToolResult.json(closest(listObjects(a, 50), a.bool("reachable", false), "object")));

		r.add("interact_object",
			"Click an option on the nearest matching object (e.g. 'Open', 'Chop down', 'Bank'). Use x/y to pick one "
				+ "tile. Returns once the click is sent.",
			filterSchema("Object").str("action", "Menu option, e.g. 'Open'").required("action").build(),
			a -> interactObject(a));

		r.add("list_ground_items",
			"List items on the ground near the player, nearest first. Each item: id, name, quantity, tile, map_tile, "
				+ "distance, noted, stackable, owned, ha_price.",
			filterSchema("Item").integer("limit", "Max items (default " + defLimit + ")").build(),
			a -> ToolResult.json(listGroundItems(a, defLimit)));

		r.add("find_closest_ground_item", "The nearest ground item matching name/id.",
			filterSchema("Item").build(),
			a -> ToolResult.json(closest(listGroundItems(a, 50), false, "item")));

		r.add("interact_ground_item",
			"Pick up (action 'Take', the default) or use another option on the nearest matching ground item.",
			filterSchema("Item").str("action", "Menu option (default 'Take')").build(),
			a -> interactGroundItem(a));

		r.add("list_players",
			"List other players near the local player, nearest first: name, combat_level, tile, distance, animation, "
				+ "interacting, skull, overhead, team.",
			Schema.object().str("name", "Name substring").integer("max_distance", "Max distance in tiles (default 20)", 0, 200)
				.integer("limit", "Max items").bool("verbose", "Add pose, orientation, friend/clan flags").build(),
			a -> ToolResult.json(listPlayers(a, defLimit)));

		r.add("list_projectiles",
			"Projectiles in flight: id, source and target (actor or tile), current tile, distance, remaining_cycles "
				+ "(client cycles, 20 ms each). Use for dodge/prayer logic.",
			Schema.object().integer("id", "Projectile id").integer("max_distance", "Max distance in tiles (default 30)", 0, 200).build(),
			a -> ToolResult.json(listProjectiles(a)));

		r.add("list_spot_anims",
			"Graphics objects on the ground (spot anims such as falling rocks or AoE markers): id, tile, distance, "
				+ "start_cycle, finished. Pass include_actors=true to also list spot anims on NPCs and players.",
			Schema.object().integer("id", "Spot anim id").integer("max_distance", "Max distance in tiles (default 30)", 0, 200)
				.bool("include_actors", "Also list spot anims playing on NPCs and players").build(),
			a -> ToolResult.json(listSpotAnims(a)));

		r.add("get_entity_info",
			"Everything about one NPC or player: all list fields plus verbose ones, its definition (size, actions, "
				+ "combat level) and active spot anims. Target an NPC by index or name, a player by name.",
			Schema.object().enumStr("type", "npc (default) or player", "npc", "player")
				.integer("index", "NPC index").str("name", "Name (substring)").build(),
			a -> ToolResult.json(entityInfo(a)));
	}

	// ---- NPCs

	static Map<String, Object> listNpcs(ToolArgs a, int defLimit)
	{
		Filter f = new Filter(a, defLimit);
		Integer index = a.intOrNull("index");
		boolean onlyMine = a.bool("interacting_with_me", false);
		boolean verbose = a.bool("verbose", false);
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			WorldPoint from = SceneJson.myTile(c);
			List<Map<String, Object>> items = new ArrayList<>();
			for (Rs2NpcModel model : ProjectX.getRs2NpcCache().query().toList())
			{
				NPC npc = model.getNpc();
				if (npc == null || (index != null && npc.getIndex() != index))
				{
					continue;
				}
				Map<String, Object> m = SceneJson.npc(c, npc, from, verbose);
				if (!f.idOk((Integer) m.get("id"), npc.getId()) || !f.nameOk((String) m.get("name"))
					|| !f.placeOk(npc.getWorldLocation(), (Integer) m.get("distance")))
				{
					continue;
				}
				if (onlyMine && !Boolean.TRUE.equals(m.get("interacting_with_me")))
				{
					continue;
				}
				items.add(m);
			}
			return page(items, f);
		});
	}

	private static ToolResult interactNpc(ToolArgs a) throws InterruptedException
	{
		String action = a.requireStr("action");
		requireTarget(a, "index");
		Integer index = a.intOrNull("index");
		Filter f = new Filter(a, 50);
		Object[] hit = onClient(() ->
		{
			Client c = ProjectX.getClient();
			WorldPoint from = SceneJson.myTile(c);
			List<Object[]> found = new ArrayList<>();
			for (Rs2NpcModel model : ProjectX.getRs2NpcCache().query().toList())
			{
				NPC npc = model.getNpc();
				if (npc == null || (index != null && npc.getIndex() != index))
				{
					continue;
				}
				Map<String, Object> m = SceneJson.npc(c, npc, from, false);
				if (f.idOk((Integer) m.get("id"), npc.getId()) && f.nameOk((String) m.get("name"))
					&& f.placeOk(npc.getWorldLocation(), (Integer) m.get("distance")))
				{
					found.add(new Object[]{model, m});
				}
			}
			found.sort((x, y) -> f.order().compare(cast(x[1]), cast(y[1])));
			return found.isEmpty() ? null : found.get(0);
		});
		if (hit == null)
		{
			throw new ToolFailure("No matching NPC within " + f.maxDistance + " tiles");
		}
		Map<String, Object> info = cast(hit[1]);
		checkAction(info, action, "NPC");
		boolean clicked = ((Rs2NpcModel) hit[0]).click(action);
		return clickResult(clicked, action, "npc", info);
	}

	// ---- objects

	static Map<String, Object> listObjects(ToolArgs a, int defLimit)
	{
		Filter f = new Filter(a, defLimit);
		String action = a.str("action");
		boolean unnamed = a.bool("include_unnamed", false);
		return onClient(() -> page(collectObjects(f, action, unnamed, null), f));
	}

	/** On the client thread. When {@code models} is given, the matching models are added to it in the same order. */
	private static List<Map<String, Object>> collectObjects(Filter f, String action, boolean unnamed, List<Rs2TileObjectModel> models)
	{
		Client c = ProjectX.getClient();
		WorldPoint from = SceneJson.myTile(c);
		List<Object[]> found = new ArrayList<>();
		for (Rs2TileObjectModel model : ProjectX.getRs2TileObjectCache().query().toList())
		{
			WorldPoint tile = model.getWorldLocation();
			Integer distance = SceneJson.distance(tile, from);
			if (!f.placeOk(tile, distance))
			{
				continue;
			}
			int id = model.getId();
			ObjectComposition comp = SceneJson.objectComposition(c, id);
			int shownId = comp != null ? comp.getId() : id;
			if (!f.idOk(id, shownId))
			{
				continue;
			}
			String name = comp != null ? SceneJson.clean(comp.getName()) : null;
			if (!unnamed && (name == null || name.isEmpty() || "null".equals(name)))
			{
				continue;
			}
			if (!f.nameOk(name))
			{
				continue;
			}
			List<String> actions = comp != null ? SceneJson.actions(comp.getActions()) : new ArrayList<>();
			if (action != null && !action.isEmpty() && actions.stream().noneMatch(x -> x.toLowerCase().contains(action.toLowerCase())))
			{
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", id);
			if (shownId != id)
			{
				m.put("shown_id", shownId);
			}
			m.put("name", name);
			m.put("type", model.getTileObjectType().name());
			LocalPoint lp = model.getLocalLocation();
			SceneJson.putWhere(m, c, tile, lp, model.getWorldView(), from);
			m.put("actions", actions);
			if (comp != null && (comp.getSizeX() > 1 || comp.getSizeY() > 1))
			{
				m.put("size", comp.getSizeX() + "x" + comp.getSizeY());
			}
			found.add(new Object[]{model, m});
		}
		found.sort((x, y) -> f.order().compare(cast(x[1]), cast(y[1])));
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object[] o : found)
		{
			out.add(cast(o[1]));
			if (models != null)
			{
				models.add((Rs2TileObjectModel) o[0]);
			}
		}
		return out;
	}

	private static ToolResult interactObject(ToolArgs a) throws InterruptedException
	{
		String action = a.requireStr("action");
		requireTarget(a, "x");
		Filter f = new Filter(a, 50);
		Object[] hit = onClient(() ->
		{
			List<Rs2TileObjectModel> models = new ArrayList<>();
			List<Map<String, Object>> found = collectObjects(f, null, true, models);
			return found.isEmpty() ? null : new Object[]{models.get(0), found.get(0)};
		});
		if (hit == null)
		{
			throw new ToolFailure("No matching object within " + f.maxDistance + " tiles");
		}
		Map<String, Object> info = cast(hit[1]);
		checkAction(info, action, "Object");
		boolean clicked = ((Rs2TileObjectModel) hit[0]).click(action);
		return clickResult(clicked, action, "object", info);
	}

	// ---- ground items

	static Map<String, Object> listGroundItems(ToolArgs a, int defLimit)
	{
		Filter f = new Filter(a, defLimit);
		return onClient(() -> page(collectGroundItems(f, null), f));
	}

	private static List<Map<String, Object>> collectGroundItems(Filter f, List<Rs2TileItemModel> models)
	{
		Client c = ProjectX.getClient();
		WorldPoint from = SceneJson.myTile(c);
		List<Object[]> found = new ArrayList<>();
		for (Rs2TileItemModel model : ProjectX.getRs2TileItemCache().query().toList())
		{
			WorldPoint tile = model.getWorldLocation();
			Integer distance = SceneJson.distance(tile, from);
			if (!f.placeOk(tile, distance))
			{
				continue;
			}
			TileItem item = model.getTileItem();
			int id = item.getId();
			if (!f.idOk(id))
			{
				continue;
			}
			ItemComposition comp = c.getItemDefinition(id);
			String name = comp != null ? SceneJson.clean(comp.getName()) : null;
			if (!f.nameOk(name))
			{
				continue;
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", id);
			m.put("name", name);
			m.put("quantity", item.getQuantity());
			SceneJson.putWhere(m, c, tile, model.getLocalLocation(), model.getWorldView(), from);
			if (comp != null)
			{
				m.put("noted", comp.getNote() != -1);
				m.put("stackable", comp.isStackable());
				m.put("ha_price", comp.getHaPrice());
			}
			m.put("owned", item.getOwnership() == TileItem.OWNERSHIP_SELF);
			found.add(new Object[]{model, m});
		}
		found.sort((x, y) -> f.order().compare(cast(x[1]), cast(y[1])));
		List<Map<String, Object>> out = new ArrayList<>();
		for (Object[] o : found)
		{
			out.add(cast(o[1]));
			if (models != null)
			{
				models.add((Rs2TileItemModel) o[0]);
			}
		}
		return out;
	}

	private static ToolResult interactGroundItem(ToolArgs a) throws InterruptedException
	{
		String action = a.str("action", "Take");
		requireTarget(a, "x");
		Filter f = new Filter(a, 50);
		Object[] hit = onClient(() ->
		{
			List<Rs2TileItemModel> models = new ArrayList<>();
			List<Map<String, Object>> found = collectGroundItems(f, models);
			return found.isEmpty() ? null : new Object[]{models.get(0), found.get(0)};
		});
		if (hit == null)
		{
			throw new ToolFailure("No matching ground item within " + f.maxDistance + " tiles");
		}
		Rs2TileItemModel model = (Rs2TileItemModel) hit[0];
		boolean clicked = "take".equalsIgnoreCase(action) ? model.pickup() : model.click(action);
		return clickResult(clicked, action, "item", cast(hit[1]));
	}

	// ---- players, projectiles, spot anims

	private static Map<String, Object> listPlayers(ToolArgs a, int defLimit)
	{
		String name = a.str("name");
		int maxDistance = a.integer("max_distance", 20, 0, 200);
		int limit = a.integer("limit", defLimit, 1, 5000);
		boolean verbose = a.bool("verbose", false);
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Player me = c.getLocalPlayer();
			WorldPoint from = SceneJson.myTile(c);
			List<Map<String, Object>> items = new ArrayList<>();
			for (Player p : c.getTopLevelWorldView().players())
			{
				if (p == null || p == me)
				{
					continue;
				}
				String n = SceneJson.clean(p.getName());
				if (name != null && !name.isEmpty() && (n == null || !n.toLowerCase().contains(name.toLowerCase())))
				{
					continue;
				}
				Map<String, Object> m = SceneJson.player(c, p, from, verbose);
				Integer d = (Integer) m.get("distance");
				if (d == null || d > maxDistance)
				{
					continue;
				}
				items.add(m);
			}
			items.sort(Comparator.comparingInt(m -> (Integer) m.get("distance")));
			return pageOf(items, limit);
		});
	}

	private static Map<String, Object> listProjectiles(ToolArgs a)
	{
		Integer id = a.intOrNull("id");
		int maxDistance = a.integer("max_distance", 30, 0, 200);
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Player me = c.getLocalPlayer();
			WorldPoint from = SceneJson.myTile(c);
			int cycle = c.getGameCycle();
			List<Map<String, Object>> items = new ArrayList<>();
			for (Projectile p : c.getProjectiles())
			{
				if (p == null || (id != null && p.getId() != id))
				{
					continue;
				}
				WorldPoint at = WorldPoint.fromLocal(c, (int) p.getX(), (int) p.getY(), p.getFloor());
				Integer d = SceneJson.distance(at, from);
				if (d != null && d > maxDistance)
				{
					continue;
				}
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", p.getId());
				m.put("tile", tile(at));
				m.put("distance", d);
				m.put("source", p.getSourceActor() != null ? SceneJson.actorRef(p.getSourceActor(), me) : tile(p.getSourcePoint()));
				m.put("target", p.getTargetActor() != null ? SceneJson.actorRef(p.getTargetActor(), me) : tile(p.getTargetPoint()));
				m.put("targets_me", p.getTargetActor() != null && p.getTargetActor() == me);
				m.put("remaining_cycles", p.getRemainingCycles());
				m.put("start_cycle", p.getStartCycle());
				m.put("end_cycle", p.getEndCycle());
				m.put("height", p.getHeight());
				items.add(m);
			}
			Map<String, Object> out = pageOf(items, 500);
			out.put("game_cycle", cycle);
			return out;
		});
	}

	private static Map<String, Object> listSpotAnims(ToolArgs a)
	{
		Integer id = a.intOrNull("id");
		int maxDistance = a.integer("max_distance", 30, 0, 200);
		boolean actors = a.bool("include_actors", false);
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Player me = c.getLocalPlayer();
			WorldPoint from = SceneJson.myTile(c);
			List<Map<String, Object>> items = new ArrayList<>();
			for (GraphicsObject g : c.getTopLevelWorldView().getGraphicsObjects())
			{
				if (g == null || (id != null && g.getId() != id))
				{
					continue;
				}
				WorldPoint at = WorldPoint.fromLocal(c, g.getLocation());
				Integer d = SceneJson.distance(at, from);
				if (d != null && d > maxDistance)
				{
					continue;
				}
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", g.getId());
				m.put("on", "ground");
				SceneJson.putWhere(m, c, at, g.getLocation(), c.getTopLevelWorldView(), from);
				m.put("start_cycle", g.getStartCycle());
				m.put("finished", g.finished());
				items.add(m);
			}
			if (actors)
			{
				List<net.runelite.api.Actor> all = new ArrayList<>();
				c.getTopLevelWorldView().npcs().forEach(all::add);
				c.getTopLevelWorldView().players().forEach(all::add);
				for (net.runelite.api.Actor actor : all)
				{
					if (actor == null || actor.getSpotAnims() == null)
					{
						continue;
					}
					for (net.runelite.api.ActorSpotAnim s : actor.getSpotAnims())
					{
						if (id != null && s.getId() != id)
						{
							continue;
						}
						WorldPoint at = actor.getWorldLocation();
						Integer d = SceneJson.distance(at, from);
						if (d != null && d > maxDistance)
						{
							continue;
						}
						Map<String, Object> m = new LinkedHashMap<>();
						m.put("id", s.getId());
						m.put("on", SceneJson.actorRef(actor, me));
						m.put("tile", tile(at));
						m.put("distance", d);
						items.add(m);
					}
				}
			}
			return pageOf(items, 500);
		});
	}

	private static Map<String, Object> entityInfo(ToolArgs a)
	{
		String type = a.str("type", "npc");
		Integer index = a.intOrNull("index");
		String name = a.str("name");
		if (index == null && (name == null || name.isEmpty()))
		{
			throw McpError.invalidParams("Give index (NPCs) or name");
		}
		Map<String, Object> info = onClient(() ->
		{
			Client c = ProjectX.getClient();
			WorldPoint from = SceneJson.myTile(c);
			Player me = c.getLocalPlayer();
			if ("player".equals(type))
			{
				Player best = null;
				int bestD = Integer.MAX_VALUE;
				for (Player p : c.getTopLevelWorldView().players())
				{
					String n = SceneJson.clean(p.getName());
					if (n == null || !n.toLowerCase().contains(name == null ? "" : name.toLowerCase()))
					{
						continue;
					}
					Integer d = SceneJson.distance(p.getWorldLocation(), from);
					int dd = d == null ? Integer.MAX_VALUE - 1 : d;
					if (dd < bestD)
					{
						best = p;
						bestD = dd;
					}
				}
				if (best == null)
				{
					return null;
				}
				Map<String, Object> m = SceneJson.player(c, best, from, true);
				m.put("is_me", best == me);
				m.put("spot_anims", spotAnimIds(best));
				if (best.getPlayerComposition() != null)
				{
					m.put("equipment_ids", best.getPlayerComposition().getEquipmentIds());
				}
				return m;
			}
			NPC best = null;
			int bestD = Integer.MAX_VALUE;
			for (NPC npc : c.getTopLevelWorldView().npcs())
			{
				if (npc == null)
				{
					continue;
				}
				if (index != null)
				{
					if (npc.getIndex() == index)
					{
						best = npc;
						break;
					}
					continue;
				}
				String n = SceneJson.clean(npc.getName());
				if (n == null || !n.toLowerCase().contains(name.toLowerCase()))
				{
					continue;
				}
				Integer d = SceneJson.distance(npc.getWorldLocation(), from);
				int dd = d == null ? Integer.MAX_VALUE - 1 : d;
				if (dd < bestD)
				{
					best = npc;
					bestD = dd;
				}
			}
			if (best == null)
			{
				return null;
			}
			Map<String, Object> m = SceneJson.npc(c, best, from, true);
			m.put("spot_anims", spotAnimIds(best));
			m.put("world_area", best.getWorldArea() != null
				? best.getWorldArea().getX() + "," + best.getWorldArea().getY() + " " + best.getWorldArea().getWidth() + "x" + best.getWorldArea().getHeight()
				: null);
			return m;
		});
		if (info == null)
		{
			throw new ToolFailure("No such " + type + " in the scene");
		}
		return info;
	}

	private static List<Integer> spotAnimIds(net.runelite.api.Actor actor)
	{
		List<Integer> ids = new ArrayList<>();
		if (actor.getSpotAnims() != null)
		{
			for (net.runelite.api.ActorSpotAnim s : actor.getSpotAnims())
			{
				ids.add(s.getId());
			}
		}
		return ids;
	}

	// ---- shared

	@SuppressWarnings("unchecked")
	static Map<String, Object> cast(Object o)
	{
		return (Map<String, Object>) o;
	}

	private static Map<String, Object> page(List<Map<String, Object>> items, Filter f)
	{
		items.sort(f.order());
		return pageOf(items, f.limit);
	}

	static Map<String, Object> pageOf(List<Map<String, Object>> items, int limit)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		int total = items.size();
		List<Map<String, Object>> window = items.size() > limit ? new ArrayList<>(items.subList(0, limit)) : items;
		out.put("count", window.size());
		out.put("total", total);
		out.put("truncated", total > window.size());
		out.put("items", window);
		return out;
	}

	private static Map<String, Object> closest(Map<String, Object> list, boolean reachable, String what)
	{
		List<Map<String, Object>> items = castList(list.get("items"));
		for (Map<String, Object> m : items)
		{
			if (reachable)
			{
				Map<String, Object> t = cast(m.get("tile"));
				WorldPoint wp = new WorldPoint((Integer) t.get("x"), (Integer) t.get("y"), (Integer) t.get("plane"));
				if (!Rs2Tile.isTileReachable(wp) && !reachableBeside(wp))
				{
					continue;
				}
			}
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("found", true);
			out.put(what, m);
			return out;
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("found", false);
		return out;
	}

	/** An object's own tile is solid; it is reachable when a tile next to it is. */
	private static boolean reachableBeside(WorldPoint wp)
	{
		int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] d : around)
		{
			if (Rs2Tile.isTileReachable(wp.dx(d[0]).dy(d[1])))
			{
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> castList(Object o)
	{
		return (List<Map<String, Object>>) o;
	}

	private static void requireTarget(ToolArgs a, String extra)
	{
		if (!a.has("name") && !a.has("id") && !a.has(extra))
		{
			throw McpError.invalidParams("Give name, id or " + extra + " to choose the target");
		}
	}

	private static void checkAction(Map<String, Object> info, String action, String what)
	{
		Object raw = info.get("actions");
		if (!(raw instanceof List) || ((List<?>) raw).isEmpty())
		{
			return;
		}
		for (Object o : (List<?>) raw)
		{
			if (action.equalsIgnoreCase(String.valueOf(o)))
			{
				return;
			}
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("error", what + " '" + info.get("name") + "' has no option '" + action + "'");
		payload.put("actions", raw);
		payload.put("target", info);
		throw new ToolFailure((String) payload.get("error"), GSON.toJsonTree(payload));
	}

	static ToolResult clickResult(boolean clicked, String action, String key, Map<String, Object> target)
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", clicked);
		out.put("action", action);
		out.put(key, target);
		if (!clicked)
		{
			out.put("error", "The click was not sent (target off screen, blocked or gone)");
			return ToolResult.error(GSON.toJsonTree(out));
		}
		return ToolResult.json(out);
	}
}
