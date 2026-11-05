package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpJson.tile;

/**
 * Serialises game entities. Every method here reads live game state and must be called on the
 * client thread (inside {@code McpContext.onClient}).
 *
 * <p>Coordinates: {@code tile} is the entity's world point as the game reports it, which inside an
 * instance is the instance copy (the same space NPCs, objects and the local player share). Inside an
 * instance {@code map_tile} adds the original map location. {@code distance} is measured in tiles
 * (Chebyshev) between {@code tile}s, so it is right inside instances too.
 */
final class SceneJson
{
	private SceneJson()
	{
	}

	static WorldPoint myTile(Client c)
	{
		Player me = c.getLocalPlayer();
		return me != null ? me.getWorldLocation() : null;
	}

	/** Adds tile, map_tile (instances only) and distance from {@code from}. */
	static void putWhere(Map<String, Object> m, Client c, WorldPoint tile, LocalPoint local, WorldView wv, WorldPoint from)
	{
		m.put("tile", tile(tile));
		if (wv != null && wv.isInstance() && local != null)
		{
			try
			{
				m.put("map_tile", tile(WorldPoint.fromLocalInstance(c, local)));
			}
			catch (RuntimeException ignored)
			{
				// a point outside the top-level scene (a boat, a sub-world view) has no map location
			}
		}
		Integer d = distance(tile, from);
		m.put("distance", d);
	}

	static Integer distance(WorldPoint a, WorldPoint b)
	{
		if (a == null || b == null || a.getPlane() != b.getPlane())
		{
			return null;
		}
		return a.distanceTo(b);
	}

	static String clean(String s)
	{
		return s == null ? null : Text.removeTags(s).replace(' ', ' ').trim();
	}

	static List<String> actions(String[] raw)
	{
		List<String> out = new ArrayList<>();
		if (raw != null)
		{
			for (String a : raw)
			{
				if (a != null && !a.isEmpty())
				{
					out.add(clean(a));
				}
			}
		}
		return out;
	}

	static NPCComposition npcComposition(NPC npc)
	{
		NPCComposition comp = npc.getTransformedComposition();
		return comp != null ? comp : npc.getComposition();
	}

	static ObjectComposition objectComposition(Client c, int id)
	{
		ObjectComposition comp = c.getObjectDefinition(id);
		if (comp != null && comp.getImpostorIds() != null)
		{
			ObjectComposition impostor = comp.getImpostor();
			if (impostor != null)
			{
				return impostor;
			}
		}
		return comp;
	}

	static Map<String, Object> actorRef(Actor a, Player me)
	{
		if (a == null)
		{
			return null;
		}
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("type", a instanceof NPC ? "npc" : a instanceof Player ? "player" : "actor");
		m.put("name", clean(a.getName()));
		if (a instanceof NPC)
		{
			m.put("index", ((NPC) a).getIndex());
			m.put("id", ((NPC) a).getId());
		}
		if (a == me)
		{
			m.put("is_me", true);
		}
		return m;
	}

	static boolean isMoving(Actor a)
	{
		return a.getPoseAnimation() != a.getIdlePoseAnimation();
	}

	static Map<String, Object> npc(Client c, NPC npc, WorldPoint from, boolean verbose)
	{
		Player me = c.getLocalPlayer();
		NPCComposition comp = npcComposition(npc);
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("index", npc.getIndex());
		m.put("id", comp != null ? comp.getId() : npc.getId());
		if (comp != null && comp.getId() != npc.getId())
		{
			m.put("base_id", npc.getId());
		}
		m.put("name", clean(comp != null ? comp.getName() : npc.getName()));
		m.put("combat_level", npc.getCombatLevel());
		putWhere(m, c, npc.getWorldLocation(), npc.getLocalLocation(), npc.getWorldView(), from);
		m.put("animation", npc.getAnimation());
		m.put("moving", isMoving(npc));
		Actor target = npc.getInteracting();
		m.put("interacting", actorRef(target, me));
		m.put("interacting_with_me", target != null && target == me);
		int scale = npc.getHealthScale();
		if (scale > 0)
		{
			m.put("health_percent", Math.round(npc.getHealthRatio() * 100.0 / scale));
		}
		m.put("dead", npc.isDead());
		m.put("actions", comp != null ? actions(comp.getActions()) : new ArrayList<>());
		if (verbose)
		{
			m.put("pose_animation", npc.getPoseAnimation());
			m.put("idle_pose_animation", npc.getIdlePoseAnimation());
			m.put("orientation", npc.getOrientation());
			m.put("current_orientation", npc.getCurrentOrientation());
			m.put("graphic", npc.getGraphic());
			m.put("overhead_text", npc.getOverheadText());
			m.put("size", comp != null ? comp.getSize() : 1);
			m.put("health_ratio", npc.getHealthRatio());
			m.put("health_scale", scale);
		}
		return m;
	}

	static Map<String, Object> player(Client c, Player p, WorldPoint from, boolean verbose)
	{
		Player me = c.getLocalPlayer();
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("name", clean(p.getName()));
		m.put("combat_level", p.getCombatLevel());
		putWhere(m, c, p.getWorldLocation(), p.getLocalLocation(), p.getWorldView(), from);
		m.put("animation", p.getAnimation());
		m.put("moving", isMoving(p));
		Actor target = p.getInteracting();
		m.put("interacting", actorRef(target, me));
		m.put("interacting_with_me", target != null && target == me);
		m.put("skull", p.getSkullIcon());
		m.put("overhead", p.getOverheadIcon() != null ? p.getOverheadIcon().name() : null);
		m.put("team", p.getTeam());
		if (verbose)
		{
			m.put("pose_animation", p.getPoseAnimation());
			m.put("orientation", p.getOrientation());
			m.put("graphic", p.getGraphic());
			m.put("overhead_text", p.getOverheadText());
			m.put("friend", p.isFriend());
			m.put("clan_member", p.isClanMember());
		}
		return m;
	}
}
