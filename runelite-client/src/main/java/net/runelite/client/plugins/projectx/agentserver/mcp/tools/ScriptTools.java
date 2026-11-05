package net.runelite.client.plugins.projectx.agentserver.mcp.tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpContext;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpError;
import net.runelite.client.plugins.projectx.agentserver.mcp.McpToolRegistry;
import net.runelite.client.plugins.projectx.agentserver.mcp.Schema;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolArgs;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolFailure;
import net.runelite.client.plugins.projectx.agentserver.mcp.ToolResult;
import net.runelite.client.plugins.projectx.externalplugins.ProjectXPluginManager;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Scripts (plugins): list, start, stop, status, settings, refresh, pause; plus quest helper and runtime deploys. */
final class ScriptTools
{
	/** Setting keys whose values list_plugin_settings masks (get_plugin_setting still reads them). */
	private static final Pattern SECRET_KEY = Pattern.compile("(?i).*(password|passwd|pin|secret|token|apikey|api_key).*");

	private ScriptTools()
	{
	}

	static void register(McpToolRegistry r, McpContext ctx)
	{
		r.category("scripts");

		r.add("list_scripts",
			"Installed Project X scripts and built-in plugins: name, class_name, version, active (running), enabled. "
				+ "Filter with name (substring) or running_only. Also returns scripts_paused.",
			Schema.object().str("name", "Name substring").bool("running_only", "Only running ones").build(),
			a -> ToolResult.json(listScripts(ctx, a)));

		r.add("start_script",
			"Start a script by class_name (fully qualified, from list_scripts) or by name (substring of the display "
				+ "name; the first match wins, so prefer class_name).",
			target(), a -> ToolResult.json(ctx.rest("POST", "/scripts/start", targetBody(a))));

		r.add("stop_script", "Stop a running script by class_name or name.",
			target(), a -> ToolResult.json(ctx.rest("POST", "/scripts/stop", targetBody(a))));

		r.add("get_script_status",
			"One script's status (RUNNING/STOPPED, start time) plus its loop heartbeat: loop_count, last_heartbeat_at "
				+ "and stalled_ms (time since its loop last ran; large = stuck).",
			target(), a ->
			{
				targetBody(a);
				String q = McpContext.query("className", a.str("class_name"), "name", a.str("name"));
				JsonElement status = ctx.rest("GET", "/scripts/status" + q, null);
				JsonElement health = ctx.rest("GET", "/scripts/health" + q, null);
				if (status.isJsonObject())
				{
					status.getAsJsonObject().add("health", health);
				}
				return ToolResult.json(status);
			});

		r.add("get_script_health",
			"Loop heartbeats of every running script (or one by class_name/name): loop_count, last_heartbeat_at, stalled_ms.",
			Schema.object().str("class_name", "Fully qualified class").str("name", "Display name substring").build(),
			a -> ToolResult.json(ctx.rest("GET", "/scripts/health" + McpContext.query("className", a.str("class_name"), "name", a.str("name")), null)));

		r.add("get_script_results",
			"Results a script submitted through ScriptResultStore (test mode and agent runs).",
			target(), a ->
			{
				targetBody(a);
				return ToolResult.json(ctx.rest("GET", "/scripts/results" + McpContext.query("className", a.str("class_name"), "name", a.str("name")), null));
			});

		r.add("get_state_machine_snapshot",
			"Live state of StateMachineScript scripts: with script (name substring) the full snapshot (current state, "
				+ "last transition and reason, loop count); without it a summary of every registered one.",
			Schema.object().str("script", "Script name (substring)").build(),
			a -> ToolResult.json(ctx.rest("GET", "/debug/snapshot" + McpContext.query("script", a.str("script")), null)));

		r.add("pause_scripts",
			"Pause every running script (the same flag breaks use). Scripts stop acting at their next loop.",
			Schema.empty(),
			a ->
			{
				ProjectX.pauseAllScripts.set(true);
				return ToolResult.json(GameTools.map("scripts_paused", true));
			});

		r.add("resume_scripts", "Resume scripts after pause_scripts.", Schema.empty(),
			a ->
			{
				ProjectX.pauseAllScripts.set(false);
				return ToolResult.json(GameTools.map("scripts_paused", false));
			});

		r.add("reload_scripts",
			"The side panel's 'Refresh scripts': checks the store for updates and new purchases, then reloads any "
				+ "changed local dev jar from ~/.runelite/projectx-plugins/dev/. Waits up to timeout_s (default 60) and "
				+ "returns the dev jars found.",
			Schema.object().integer("timeout_s", "Max wait in seconds (default 60, max 120)", 1, 120).build(),
			a -> ToolResult.json(reload(a.integer("timeout_s", 60, 1, 120))));

		r.category("settings");

		r.add("get_plugin_setting",
			"Read one plugin setting (any config group, e.g. a script's group) as stored: group and key.",
			Schema.object().str("group", "Config group").str("key", "Setting key").required("group", "key").build(),
			a -> ToolResult.json(ctx.rest("GET", "/settings/plugin" + McpContext.query("group", a.requireStr("group"), "key", a.requireStr("key")), null)));

		r.add("set_plugin_setting",
			"Write one plugin setting (string, number or boolean; null clears it). Takes effect like a change in the "
				+ "settings panel. Returns the stored value.",
			Schema.object().str("group", "Config group").str("key", "Setting key").scalar("value", "New value; null to unset")
				.required("group", "key").build(),
			a ->
			{
				Map<String, Object> body = new LinkedHashMap<>();
				body.put("group", a.requireStr("group"));
				body.put("key", a.requireStr("key"));
				JsonElement v = a.get("value");
				body.put("value", v == null || v.isJsonNull() ? null : v.getAsString());
				return ToolResult.json(ctx.rest("POST", "/settings/plugin", body));
			});

		r.add("list_plugin_settings",
			"Every stored setting in a config group (key and value). Values of keys that look secret (password, pin, "
				+ "token...) are masked; read one with get_plugin_setting if you need it.",
			Schema.object().str("group", "Config group").required("group").build(),
			a -> ToolResult.json(listSettings(a.requireStr("group"))));

		r.add("get_client_settings",
			"Project X client options the agent can toggle: disable_level_up_interface, disable_world_switcher_confirmation.",
			Schema.empty(), a -> ToolResult.json(ctx.rest("GET", "/settings/config", null)));

		r.add("set_client_setting",
			"Set a Project X client option: key disableLevelUpInterface or disableWorldSwitcherConfirmation, boolean value.",
			Schema.object().enumStr("key", "Option", "disableLevelUpInterface", "disableWorldSwitcherConfirmation")
				.bool("value", "On or off").required("key", "value").build(),
			a -> ToolResult.json(ctx.rest("POST", "/settings/config", ItemTools.body("key", a.requireStr("key"), "value", a.bool("value", false)))));

		r.add("level_up_notifications",
			"Read (no enable argument) or change the in-game level-up notification setting, through the game's settings.",
			Schema.object().bool("enable", "true to enable, false to disable; omit to read").build(),
			a -> ToolResult.json(a.has("enable")
				? ctx.rest("POST", "/settings/level-up", ItemTools.body("enable", a.bool("enable", true)))
				: ctx.rest("GET", "/settings/level-up", null)));

		r.category("quest_helper");

		r.add("quest_helper_status",
			"Quest Helper: active/enabled, the selected quest, whether it is completed and the current step text.",
			Schema.empty(), a -> ToolResult.json(ctx.rest("GET", "/quest-helper/status", null)));

		r.add("quest_helper_start", "Select a quest in Quest Helper by its display name (see quest_helper_list).",
			Schema.object().str("name", "Quest name").required("name").build(),
			a -> ToolResult.json(ctx.rest("POST", "/quest-helper/start", ItemTools.body("name", a.requireStr("name")))));

		r.add("quest_helper_stop", "Close the selected Quest Helper quest.", Schema.empty(),
			a -> ToolResult.json(ctx.rest("POST", "/quest-helper/stop", null)));

		r.add("quest_helper_list",
			"Quests Quest Helper knows (enum and display_name); with_state=true adds each quest's state for this account.",
			Schema.object().str("name", "Name substring").bool("with_state", "Add NOT_STARTED/IN_PROGRESS/FINISHED").build(),
			a -> ToolResult.json(filterList(ctx.rest("GET", a.bool("with_state", false) ? "/quest-helper/states" : "/quest-helper/list", null),
				"quests", "display_name", a.str("name"))));

		r.category("deploy");

		r.add("list_deployed_scripts", "Scripts compiled and loaded at runtime with deploy_script.",
			Schema.empty(), a -> ToolResult.json(ctx.rest("GET", "/scripts/deploy", null)));

		r.add("deploy_script",
			"Compile a script from a source folder on this machine, load it and start it, without restarting the client.",
			Schema.object().str("name", "Deployment name").str("source_path", "Absolute path of the source folder")
				.required("name", "source_path").build(),
			a -> ToolResult.json(ctx.rest("POST", "/scripts/deploy", ItemTools.body("name", a.requireStr("name"), "sourcePath", a.requireStr("source_path")))));

		r.add("reload_deployed_script", "Recompile and restart a script loaded with deploy_script.",
			Schema.object().str("name", "Deployment name").required("name").build(),
			a -> ToolResult.json(ctx.rest("POST", "/scripts/deploy/reload", ItemTools.body("name", a.requireStr("name")))));

		r.add("undeploy_script", "Stop and unload a script loaded with deploy_script.",
			Schema.object().str("name", "Deployment name").required("name").build(),
			a -> ToolResult.json(ctx.rest("POST", "/scripts/deploy/undeploy", ItemTools.body("name", a.requireStr("name")))));
	}

	private static com.google.gson.JsonObject target()
	{
		return Schema.object().str("class_name", "Fully qualified plugin class (from list_scripts)")
			.str("name", "Display name substring").build();
	}

	private static Map<String, Object> targetBody(ToolArgs a)
	{
		if (!a.has("class_name") && !a.has("name"))
		{
			throw McpError.invalidParams("Give class_name or name");
		}
		return ItemTools.body("className", a.str("class_name"), "name", a.str("name"));
	}

	private static JsonElement listScripts(McpContext ctx, ToolArgs a) throws Exception
	{
		JsonElement json = ctx.rest("GET", "/scripts", null);
		String name = a.str("name");
		boolean runningOnly = a.bool("running_only", false);
		JsonArray out = new JsonArray();
		if (json.isJsonObject() && json.getAsJsonObject().has("scripts"))
		{
			for (JsonElement e : json.getAsJsonObject().getAsJsonArray("scripts"))
			{
				JsonObject s = e.getAsJsonObject();
				if (runningOnly && !(s.has("active") && s.get("active").getAsBoolean()))
				{
					continue;
				}
				if (name != null && !name.isEmpty() && !s.get("name").getAsString().toLowerCase().contains(name.toLowerCase()))
				{
					continue;
				}
				Plugin p = ProjectX.getPlugin(s.get("class_name").getAsString());
				PluginDescriptor d = p != null ? p.getClass().getAnnotation(PluginDescriptor.class) : null;
				if (d != null)
				{
					s.addProperty("version", d.version());
				}
				out.add(s);
			}
		}
		JsonObject o = new JsonObject();
		o.addProperty("count", out.size());
		o.addProperty("scripts_paused", ProjectX.pauseAllScripts.get());
		o.add("scripts", out);
		return o;
	}

	private static Map<String, Object> listSettings(String group)
	{
		ConfigManager cm = ProjectX.getConfigManager();
		if (cm == null)
		{
			throw new ToolFailure("Settings are not available yet");
		}
		String prefix = group + ".";
		List<Map<String, Object>> items = new ArrayList<>();
		for (String full : cm.getConfigurationKeys(prefix))
		{
			String key = full.substring(prefix.length());
			boolean secret = SECRET_KEY.matcher(key).matches();
			String value = cm.getConfiguration(group, key);
			items.add(GameTools.map("key", key, "value", secret && value != null && !value.isEmpty() ? "(hidden)" : value));
		}
		items.sort((x, y) -> String.valueOf(x.get("key")).compareTo(String.valueOf(y.get("key"))));
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("group", group);
		out.put("count", items.size());
		out.put("items", items);
		return out;
	}

	private static Map<String, Object> reload(int timeoutS) throws InterruptedException
	{
		ProjectXPluginManager pm;
		try
		{
			pm = ProjectX.getInjector().getInstance(ProjectXPluginManager.class);
		}
		catch (RuntimeException e)
		{
			throw new ToolFailure("Script manager not available: " + e.getMessage());
		}
		long start = System.currentTimeMillis();
		CountDownLatch done = new CountDownLatch(1);
		pm.syncNow(done::countDown);
		boolean finished = done.await(timeoutS, TimeUnit.SECONDS);
		File devDir = new File(new File(RuneLite.RUNELITE_DIR, "projectx-plugins"), "dev");
		File[] jars = devDir.listFiles((d, n) -> n.endsWith(".jar"));
		List<Map<String, Object>> dev = new ArrayList<>();
		if (jars != null)
		{
			for (File f : jars)
			{
				dev.add(GameTools.map("name", f.getName(), "size", f.length(), "modified", Instant.ofEpochMilli(f.lastModified()).toString()));
			}
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("finished", finished);
		out.put("elapsed_ms", System.currentTimeMillis() - start);
		out.put("dev_folder", devDir.getAbsolutePath());
		out.put("dev_jars", dev);
		out.put("hint", "Use list_scripts to see what is loaded; the session log says which dev jars were reloaded.");
		return out;
	}

	private static JsonElement filterList(JsonElement json, String arrayKey, String field, String needle)
	{
		if (needle == null || needle.isEmpty() || !json.isJsonObject() || !json.getAsJsonObject().has(arrayKey))
		{
			return json;
		}
		JsonArray out = new JsonArray();
		for (JsonElement e : json.getAsJsonObject().getAsJsonArray(arrayKey))
		{
			JsonObject o = e.getAsJsonObject();
			if (o.has(field) && o.get(field).getAsString().toLowerCase().contains(needle.toLowerCase()))
			{
				out.add(o);
			}
		}
		JsonObject res = json.getAsJsonObject();
		res.add(arrayKey, out);
		res.addProperty("count", out.size());
		return res;
	}
}
