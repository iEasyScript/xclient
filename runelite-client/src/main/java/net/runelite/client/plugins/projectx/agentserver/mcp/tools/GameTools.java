package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.Varbits;
import net.runelite.api.WorldType;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;
import static net.runelite.client.plugins.projectx.agentserver.mcp.McpJson.tile;

/** Game and login state, the local player, skills, run state and cache definitions. */
final class GameTools
{
	/** VarPlayer 173: 1 when run is switched on. */
	static final int VARP_RUN = 173;
	/** VarPlayer 300: special attack energy in tenths of a percent. */
	static final int VARP_SPECIAL_ATTACK = 300;

	private GameTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("game");

		r.add("get_game_state",
			"One-call snapshot: game_state, logged_in, world and world types, tick, login_index on the login screen, "
				+ "and a player summary (name, tile, map_tile, in_instance, animation, moving, hp, prayer, run energy, "
				+ "combat level), plus scripts_paused. Start here.",
			Schema.empty(),
			a -> ToolResult.json(gameState()));

		r.add("get_local_player",
			"The local player in detail: name, combat_level, tile/map_tile/in_instance, animation, pose_animation, "
				+ "moving, idle, interacting target, hitpoints and prayer (current/max), run energy and run_enabled, "
				+ "special_attack, weight, skull, overhead prayer, world.",
			Schema.empty(),
			a -> ToolResult.json(localPlayer()));

		r.add("get_player_position",
			"Where the player is: tile (scene/instance coordinates, the same space as NPCs and objects), map_tile "
				+ "and region_id (original map; inside instances these differ from tile), local and scene coordinates, "
				+ "plane, in_instance, world_view_id.",
			Schema.empty(),
			a -> ToolResult.json(position()));

		r.add("get_player_stats",
			"Skills: real_level, boosted_level and xp for each skill (or one, with skill=Attack), total_level, "
				+ "total_xp and combat_level.",
			Schema.object().str("skill", "Only this skill, e.g. 'Hitpoints' (case-insensitive)").build(),
			a -> ToolResult.json(stats(a.str("skill"))));

		r.add("get_run_state",
			"Run: run_enabled, run_energy (0-100), stamina_active, weight (kg) and whether the player is moving.",
			Schema.empty(),
			a -> ToolResult.json(runState()));

		r.add("get_definition",
			"Cache definition of an item, NPC or object by id: name, actions and the main properties (items: "
				+ "stackable, noted/note link, members, prices; NPCs: combat level, size; objects: size, impostor).",
			Schema.object().enumStr("kind", "item, npc or object", "item", "npc", "object")
				.integer("id", "Definition id").required("kind", "id").build(),
			a -> ToolResult.json(definition(a.requireStr("kind"), a.requireInt("id"))));
	}

	static Map<String, Object> gameState()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Map<String, Object> m = new LinkedHashMap<>();
			GameState gs = c.getGameState();
			m.put("game_state", gs.name());
			m.put("logged_in", gs == GameState.LOGGED_IN);
			m.put("world", c.getWorld());
			m.put("world_types", worldTypes(c));
			m.put("members_world", c.getWorldType().contains(WorldType.MEMBERS));
			m.put("tick", c.getTickCount());
			if (gs == GameState.LOGIN_SCREEN)
			{
				m.put("login_index", c.getLoginIndex());
			}
			Player me = c.getLocalPlayer();
			if (gs == GameState.LOGGED_IN && me != null)
			{
				Map<String, Object> p = new LinkedHashMap<>();
				p.put("name", me.getName());
				putPosition(p, c, me);
				p.put("animation", me.getAnimation());
				p.put("moving", SceneJson.isMoving(me));
				p.put("interacting", SceneJson.actorRef(me.getInteracting(), me));
				p.put("hitpoints", c.getBoostedSkillLevel(Skill.HITPOINTS) + "/" + c.getRealSkillLevel(Skill.HITPOINTS));
				p.put("prayer", c.getBoostedSkillLevel(Skill.PRAYER) + "/" + c.getRealSkillLevel(Skill.PRAYER));
				p.put("run_energy", c.getEnergy() / 100);
				p.put("combat_level", me.getCombatLevel());
				m.put("player", p);
			}
			m.put("scripts_paused", ProjectX.pauseAllScripts.get());
			return m;
		});
	}

	private static List<String> worldTypes(Client c)
	{
		List<String> out = new ArrayList<>();
		for (WorldType t : c.getWorldType())
		{
			out.add(t.name());
		}
		return out;
	}

	private static void putPosition(Map<String, Object> m, Client c, Player me)
	{
		WorldPoint tile = me.getWorldLocation();
		m.put("tile", tile(tile));
		boolean instance = me.getWorldView() != null && me.getWorldView().isInstance();
		m.put("in_instance", instance);
		if (instance && me.getLocalLocation() != null)
		{
			m.put("map_tile", tile(WorldPoint.fromLocalInstance(c, me.getLocalLocation())));
		}
	}

	private static Map<String, Object> requireMe(Client c)
	{
		if (c.getGameState() != GameState.LOGGED_IN || c.getLocalPlayer() == null)
		{
			throw new ToolFailure("Not logged in");
		}
		return new LinkedHashMap<>();
	}

	static Map<String, Object> localPlayer()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Map<String, Object> m = requireMe(c);
			Player me = c.getLocalPlayer();
			m.put("name", me.getName());
			m.put("combat_level", me.getCombatLevel());
			putPosition(m, c, me);
			m.put("animation", me.getAnimation());
			m.put("pose_animation", me.getPoseAnimation());
			boolean moving = SceneJson.isMoving(me);
			m.put("moving", moving);
			Actor target = me.getInteracting();
			m.put("idle", me.getAnimation() == -1 && !moving && target == null);
			m.put("interacting", SceneJson.actorRef(target, me));
			m.put("hitpoints", map("current", c.getBoostedSkillLevel(Skill.HITPOINTS), "max", c.getRealSkillLevel(Skill.HITPOINTS)));
			m.put("prayer", map("current", c.getBoostedSkillLevel(Skill.PRAYER), "max", c.getRealSkillLevel(Skill.PRAYER)));
			m.put("run_energy", c.getEnergy() / 100);
			m.put("run_enabled", c.getVarpValue(VARP_RUN) == 1);
			m.put("special_attack", c.getVarpValue(VARP_SPECIAL_ATTACK) / 10);
			m.put("weight", c.getWeight());
			m.put("skull", me.getSkullIcon());
			m.put("overhead", me.getOverheadIcon() != null ? me.getOverheadIcon().name() : null);
			m.put("world", c.getWorld());
			return m;
		});
	}

	private static Map<String, Object> position()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Map<String, Object> m = requireMe(c);
			Player me = c.getLocalPlayer();
			putPosition(m, c, me);
			WorldPoint map = m.containsKey("map_tile") ? WorldPoint.fromLocalInstance(c, me.getLocalLocation()) : me.getWorldLocation();
			m.put("region_id", map.getRegionID());
			m.put("region_x", map.getRegionX());
			m.put("region_y", map.getRegionY());
			LocalPoint lp = me.getLocalLocation();
			if (lp != null)
			{
				m.put("local", map("x", lp.getX(), "y", lp.getY()));
				m.put("scene", map("x", lp.getSceneX(), "y", lp.getSceneY()));
			}
			m.put("plane", me.getWorldLocation().getPlane());
			m.put("world_view_id", me.getWorldView() != null ? me.getWorldView().getId() : null);
			m.put("top_level_world_view", me.getWorldView() == null || me.getWorldView().isTopLevel());
			return m;
		});
	}

	private static Map<String, Object> stats(String only)
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			List<Map<String, Object>> items = new ArrayList<>();
			int total = 0;
			long totalXp = 0;
			for (Skill s : Skill.values())
			{
				if (s == Skill.OVERALL)
				{
					continue;
				}
				int real = c.getRealSkillLevel(s);
				total += real;
				totalXp += c.getSkillExperience(s);
				if (only != null && !only.isEmpty() && !s.getName().equalsIgnoreCase(only))
				{
					continue;
				}
				Map<String, Object> e = new LinkedHashMap<>();
				e.put("skill", s.getName());
				e.put("real_level", real);
				e.put("boosted_level", c.getBoostedSkillLevel(s));
				e.put("xp", c.getSkillExperience(s));
				items.add(e);
			}
			if (only != null && !only.isEmpty() && items.isEmpty())
			{
				throw new ToolFailure("Unknown skill: " + only);
			}
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("items", items);
			m.put("total_level", total);
			m.put("total_xp", totalXp);
			Player me = c.getLocalPlayer();
			m.put("combat_level", me != null ? me.getCombatLevel() : null);
			return m;
		});
	}

	private static Map<String, Object> runState()
	{
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Map<String, Object> m = requireMe(c);
			m.put("run_enabled", c.getVarpValue(VARP_RUN) == 1);
			m.put("run_energy", c.getEnergy() / 100);
			m.put("stamina_active", c.getVarbitValue(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) != 0);
			m.put("weight", c.getWeight());
			m.put("moving", SceneJson.isMoving(c.getLocalPlayer()));
			return m;
		});
	}

	private static Map<String, Object> definition(String kind, int id)
	{
		if (id < 0)
		{
			throw McpError.invalidParams("id must be >= 0");
		}
		return onClient(() ->
		{
			Client c = ProjectX.getClient();
			Map<String, Object> m = new LinkedHashMap<>();
			m.put("kind", kind);
			m.put("id", id);
			switch (kind)
			{
				case "item":
				{
					ItemComposition d = c.getItemDefinition(id);
					if (d == null)
					{
						throw new ToolFailure("No item " + id);
					}
					m.put("name", d.getName());
					m.put("members", d.isMembers());
					m.put("stackable", d.isStackable());
					m.put("noted", d.getNote() != -1);
					m.put("linked_note_id", d.getLinkedNoteId());
					m.put("placeholder_id", d.getPlaceholderId());
					m.put("store_price", d.getPrice());
					m.put("ha_price", d.getHaPrice());
					m.put("inventory_actions", SceneJson.actions(d.getInventoryActions()));
					return m;
				}
				case "npc":
				{
					NPCComposition d = c.getNpcDefinition(id);
					if (d == null)
					{
						throw new ToolFailure("No NPC " + id);
					}
					m.put("name", d.getName());
					m.put("combat_level", d.getCombatLevel());
					m.put("size", d.getSize());
					m.put("actions", SceneJson.actions(d.getActions()));
					m.put("transforms", d.getConfigs() != null);
					return m;
				}
				case "object":
				{
					ObjectComposition d = c.getObjectDefinition(id);
					if (d == null)
					{
						throw new ToolFailure("No object " + id);
					}
					m.put("name", d.getName());
					m.put("actions", SceneJson.actions(d.getActions()));
					m.put("size_x", d.getSizeX());
					m.put("size_y", d.getSizeY());
					m.put("has_impostor", d.getImpostorIds() != null);
					if (d.getImpostorIds() != null)
					{
						ObjectComposition imp = d.getImpostor();
						if (imp != null)
						{
							m.put("impostor", map("id", imp.getId(), "name", imp.getName(), "actions", SceneJson.actions(imp.getActions())));
						}
						m.put("varbit_id", d.getVarbitId());
						m.put("varp_id", d.getVarPlayerId());
					}
					return m;
				}
				default:
					throw McpError.invalidParams("kind must be item, npc or object");
			}
		});
	}

	static Map<String, Object> map(Object... kv)
	{
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2)
		{
			m.put(String.valueOf(kv[i]), kv[i + 1]);
		}
		return m;
	}
}
