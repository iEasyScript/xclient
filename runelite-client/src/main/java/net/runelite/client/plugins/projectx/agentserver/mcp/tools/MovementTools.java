package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.handler.WalkHandler;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;
import net.runelite.client.plugins.projectx.util.tile.Rs2Tile;
import net.runelite.client.plugins.projectx.util.walker.Rs2Walker;

import java.util.LinkedHashMap;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;
import static net.runelite.client.plugins.projectx.agentserver.mcp.McpJson.tile;

/** The web walker, single walk clicks, stopping and reachability. */
final class MovementTools
{
	private MovementTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("movement");

		r.add("walk_to",
			"Walk to a world-map tile with the web walker (shortest path: doors, stairs, shortcuts, transports). "
				+ "Without wait it returns at once and the walk carries on in the background; with wait=true it "
				+ "returns on arrival or after timeout_s (default 30, max 300) with distance_to_destination. "
				+ "distance = how close counts as arrived (default 0). Outside instances use normal map coordinates; "
				+ "inside an instance use walk_click with 'tile' coordinates instead.",
			Schema.object().integer("x", "Target x").integer("y", "Target y").integer("plane", "Target plane (default 0)")
				.bool("wait", "Block until arrived or timed out (default false)")
				.integer("timeout_s", "Seconds to wait when wait=true (default 30, max 300)", 1, 300)
				.integer("distance", "Arrival radius in tiles (default 0, max 20)", 0, 20)
				.required("x", "y").build(),
			a -> ToolResult.json(ctx.rest("POST", "/walk", ItemTools.body(
				"x", a.requireInt("x"), "y", a.requireInt("y"), "plane", a.integer("plane", 0),
				"wait", a.bool("wait", false), "timeout", a.integer("timeout_s", 30, 1, 300),
				"distance", a.integer("distance", 0, 0, 20)))));

		r.add("walk_click",
			"One walk click on a nearby tile, no path finding: on the game scene (default) or on the minimap "
				+ "(minimap=true). Takes 'tile' coordinates, so it works inside instances. Returns once clicked.",
			Schema.object().integer("x", "Tile x").integer("y", "Tile y")
				.integer("plane", "Plane (default: the player's)").bool("minimap", "Click the minimap instead of the scene")
				.required("x", "y").build(),
			a -> ToolResult.json(walkClick(a)));

		r.add("stop_walking", "Stop the current walk_to walk (and clear the walker's route).", Schema.empty(),
			a -> ToolResult.json(GameTools.map("was_walking", WalkHandler.cancelActiveWalk())));

		r.add("is_reachable",
			"Whether the player can walk to a tile from where it stands now (local collision map, current scene only), "
				+ "plus the straight-line distance. For objects, which are solid, test a tile next to them.",
			Schema.object().integer("x", "Tile x").integer("y", "Tile y").integer("plane", "Plane (default: the player's)")
				.required("x", "y").build(),
			a -> ToolResult.json(reachable(a)));
	}

	private static WorldPoint target(ToolArgs a)
	{
		int x = a.requireInt("x");
		int y = a.requireInt("y");
		Integer plane = a.intOrNull("plane");
		if (plane != null)
		{
			return new WorldPoint(x, y, plane);
		}
		Integer myPlane = onClient(() ->
		{
			Client c = ProjectX.getClient();
			return c.getLocalPlayer() != null ? c.getLocalPlayer().getWorldLocation().getPlane() : 0;
		});
		return new WorldPoint(x, y, myPlane == null ? 0 : myPlane);
	}

	private static Map<String, Object> walkClick(ToolArgs a)
	{
		WorldPoint wp = target(a);
		boolean minimap = a.bool("minimap", false);
		boolean ok = minimap ? Rs2Walker.walkMiniMap(wp) : Rs2Walker.walkFastCanvas(wp);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", ok);
		out.put("target", tile(wp));
		out.put("via", minimap ? "minimap" : "scene");
		if (!ok)
		{
			throw new ToolFailure("Could not click that tile (off screen or off the minimap)",
				new com.google.gson.Gson().toJsonTree(out));
		}
		return out;
	}

	private static Map<String, Object> reachable(ToolArgs a)
	{
		WorldPoint wp = target(a);
		boolean reachable = Rs2Tile.isTileReachable(wp);
		Integer distance = onClient(() -> SceneJson.distance(wp, SceneJson.myTile(ProjectX.getClient())));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("tile", tile(wp));
		out.put("reachable", reachable);
		out.put("distance", distance);
		return out;
	}
}
