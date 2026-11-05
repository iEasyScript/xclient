package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import net.runelite.api.Client;
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
import net.runelite.client.plugins.projectx.util.camera.Rs2Camera;
import net.runelite.client.plugins.projectx.util.keyboard.Rs2Keyboard;

import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.runelite.client.plugins.projectx.agentserver.mcp.McpContext.onClient;

/** Keyboard and camera. */
final class InputTools
{
	private static final int MAX_HOLD_MS = 5_000;

	private InputTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("input");

		r.add("key_press",
			"Press a key: a name (enter, escape/esc, backspace, space, tab, up, down, left, right, f1-f12, home, end, "
				+ "page_up, page_down, delete, shift, ctrl, alt) or one character ('1', 'a'). hold_ms holds it down "
				+ "(max 5000), e.g. arrow keys to turn the camera.",
			Schema.object().str("key", "Key name or a single character").integer("hold_ms", "Hold for this long (default 0 = tap)", 0, MAX_HOLD_MS)
				.required("key").build(),
			a -> ToolResult.json(keyPress(a)));

		r.add("type_text",
			"Type text into the game (chat box, number prompts, search boxes). press_enter=true presses Enter after.",
			Schema.object().str("text", "Text to type").bool("press_enter", "Press Enter afterwards (default false)")
				.required("text").build(),
			a ->
			{
				String text = a.requireStr("text");
				Rs2Keyboard.typeString(text);
				boolean enter = a.bool("press_enter", false);
				if (enter)
				{
					Rs2Keyboard.enter();
				}
				return ToolResult.json(GameTools.map("typed", text.length(), "pressed_enter", enter));
			});

		r.add("get_camera",
			"Camera state: yaw (0-2047, 0 = north, 512 = west, 1024 = south, 1536 = east), pitch and zoom.",
			Schema.empty(),
			a -> ToolResult.json(camera()));

		r.add("set_camera",
			"Move the camera: yaw (0-2047), pitch, zoom, and/or face a tile (face_x, face_y). Only the given values "
				+ "change. Uses the arrow keys like a player, so it takes a moment.",
			Schema.object().integer("yaw", "Target yaw 0-2047", 0, 2048).integer("pitch", "Target pitch")
				.integer("zoom", "Target zoom").integer("face_x", "Turn to face this tile x").integer("face_y", "Turn to face this tile y")
				.build(),
			a -> ToolResult.json(setCamera(a)));
	}

	static int keyCode(String key)
	{
		String k = key.trim();
		if (k.length() == 1)
		{
			int code = KeyEvent.getExtendedKeyCodeForChar(k.charAt(0));
			if (code == KeyEvent.VK_UNDEFINED)
			{
				throw McpError.invalidParams("Unknown key: " + key);
			}
			return code;
		}
		String name = k.toUpperCase().replace('-', '_').replace(' ', '_');
		switch (name)
		{
			case "ESC":
				name = "ESCAPE";
				break;
			case "BACKSPACE":
				name = "BACK_SPACE";
				break;
			case "DEL":
				name = "DELETE";
				break;
			case "CTRL":
			case "CONTROL":
				name = "CONTROL";
				break;
			case "RETURN":
				name = "ENTER";
				break;
			case "PAGEUP":
				name = "PAGE_UP";
				break;
			case "PAGEDOWN":
				name = "PAGE_DOWN";
				break;
			default:
				break;
		}
		try
		{
			return KeyEvent.class.getField("VK_" + name).getInt(null);
		}
		catch (ReflectiveOperationException e)
		{
			throw McpError.invalidParams("Unknown key: " + key);
		}
	}

	private static Map<String, Object> keyPress(ToolArgs a) throws InterruptedException
	{
		String key = a.requireStr("key");
		int code = keyCode(key);
		int hold = a.integer("hold_ms", 0, 0, MAX_HOLD_MS);
		if (hold > 0)
		{
			Rs2Keyboard.keyHold(code);
			try
			{
				Thread.sleep(hold);
			}
			finally
			{
				Rs2Keyboard.keyRelease(code);
			}
		}
		else if (code == KeyEvent.VK_ENTER)
		{
			Rs2Keyboard.enter();
		}
		else if (key.trim().length() == 1)
		{
			Rs2Keyboard.keyPress(key.trim().charAt(0));
		}
		else
		{
			Rs2Keyboard.keyPress(code);
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("key", key);
		out.put("key_code", code);
		out.put("held_ms", hold);
		return out;
	}

	private static Map<String, Object> camera()
	{
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("yaw", Rs2Camera.getYaw());
		out.put("pitch", Rs2Camera.getPitch());
		out.put("zoom", onClient(Rs2Camera::getZoom));
		return out;
	}

	private static Map<String, Object> setCamera(ToolArgs a)
	{
		Integer yaw = a.intOrNull("yaw");
		Integer pitch = a.intOrNull("pitch");
		Integer zoom = a.intOrNull("zoom");
		Integer fx = a.intOrNull("face_x");
		Integer fy = a.intOrNull("face_y");
		if (yaw == null && pitch == null && zoom == null && (fx == null || fy == null))
		{
			throw McpError.invalidParams("Give yaw, pitch, zoom, or face_x and face_y");
		}
		if (fx != null && fy != null)
		{
			LocalPoint lp = onClient(() ->
			{
				Client c = ProjectX.getClient();
				int plane = c.getLocalPlayer() != null ? c.getLocalPlayer().getWorldLocation().getPlane() : 0;
				return LocalPoint.fromWorld(c.getTopLevelWorldView(), new WorldPoint(fx, fy, plane));
			});
			if (lp == null)
			{
				throw new ToolFailure("That tile is not in the loaded scene");
			}
			Rs2Camera.turnTo(lp);
		}
		if (yaw != null)
		{
			Rs2Camera.setYaw(yaw);
		}
		if (pitch != null)
		{
			Rs2Camera.setPitch(pitch);
		}
		if (zoom != null)
		{
			Rs2Camera.setZoom(zoom);
		}
		return camera();
	}
}
