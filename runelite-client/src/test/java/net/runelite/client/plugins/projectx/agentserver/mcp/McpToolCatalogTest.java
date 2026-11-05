package net.runelite.client.plugins.projectx.agentserver.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.runelite.client.plugins.projectx.agentserver.mcp.tools.McpTools;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The real tool catalogue: every tool has a valid name, a description and an object schema whose
 * required arguments are declared, and the tools the docs promise are all there.
 */
public class McpToolCatalogTest
{
	private static McpToolRegistry registry()
	{
		McpContext ctx = new McpContext(null, Collections.emptyList(), new ChatLog(), 200, () -> "test");
		return McpTools.registerAll(new McpToolRegistry(), ctx);
	}

	@Test
	public void everyToolHasNameDescriptionAndSchema()
	{
		McpProtocol protocol = new McpProtocol(registry(), "projectx-osrs", "test");
		McpProtocol.Reply r = protocol.handlePost("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}", null, null);
		JsonArray tools = new JsonParser().parse(r.body).getAsJsonObject().getAsJsonObject("result").getAsJsonArray("tools");
		assertTrue("expected a full catalogue, got " + tools.size(), tools.size() >= 80);
		Set<String> names = new HashSet<>();
		for (JsonElement e : tools)
		{
			JsonObject t = e.getAsJsonObject();
			String name = t.get("name").getAsString();
			assertTrue("duplicate " + name, names.add(name));
			assertTrue(name, name.matches("[a-z][a-z0-9_]*"));
			String description = t.get("description").getAsString();
			assertTrue(name + " description too short", description.length() >= 20);
			JsonObject schema = t.getAsJsonObject("inputSchema");
			assertEquals(name, "object", schema.get("type").getAsString());
			assertTrue(name, schema.has("properties") && schema.get("properties").isJsonObject());
			JsonObject props = schema.getAsJsonObject("properties");
			for (java.util.Map.Entry<String, JsonElement> p : props.entrySet())
			{
				JsonObject prop = p.getValue().getAsJsonObject();
				assertTrue(name + "." + p.getKey() + " has no type", prop.has("type"));
				assertTrue(name + "." + p.getKey() + " has no description", prop.has("description"));
			}
			if (schema.has("required"))
			{
				for (JsonElement req : schema.getAsJsonArray("required"))
				{
					assertTrue(name + " requires undeclared " + req.getAsString(), props.has(req.getAsString()));
				}
			}
		}
	}

	@Test
	public void documentedToolsArePresent()
	{
		McpToolRegistry r = registry();
		List<String> expected = Arrays.asList(
			"get_game_state", "get_local_player", "get_player_position", "get_player_stats", "get_run_state", "get_definition",
			"get_inventory", "get_equipment", "get_bank", "list_item_containers", "get_item_container", "interact_item",
			"drop_item", "use_item_on", "bank_open", "bank_close", "bank_deposit", "bank_withdraw",
			"list_npcs", "find_closest_npc", "interact_npc", "list_objects", "find_closest_object", "interact_object",
			"list_ground_items", "find_closest_ground_item", "interact_ground_item", "list_players", "list_projectiles",
			"list_spot_anims", "get_entity_info",
			"walk_to", "walk_click", "stop_walking", "is_reachable",
			"list_open_interfaces", "get_widget_tree", "find_widgets", "click_widget", "invoke_widget_action",
			"get_menu_entries", "invoke_menu_action", "diff_open_interfaces",
			"get_dialogue", "continue_dialogue", "select_dialogue_option",
			"key_press", "type_text", "get_camera", "set_camera",
			"get_varp", "get_varbit", "get_var_client", "list_varps", "list_varbits_for_varp",
			"wait_for_varbit_change", "wait_for_varp_change", "wait_for_widget", "wait_for_interface_open",
			"wait_for_idle", "wait_for_chat_message",
			"get_chat_messages", "read_logs", "get_session_log_path",
			"list_scripts", "start_script", "stop_script", "get_script_status", "get_script_health", "get_script_results",
			"get_state_machine_snapshot", "pause_scripts", "resume_scripts", "reload_scripts",
			"get_plugin_setting", "set_plugin_setting", "list_plugin_settings", "get_client_settings", "set_client_setting",
			"level_up_notifications", "quest_helper_status", "quest_helper_start", "quest_helper_stop", "quest_helper_list",
			"list_deployed_scripts", "deploy_script", "reload_deployed_script", "undeploy_script",
			"get_login_state", "login", "logout", "list_profiles", "switch_profile", "get_break_state", "get_grand_exchange",
			"list_tools", "get_client_info", "screenshot", "search_game_settings");
		for (String name : expected)
		{
			assertTrue("missing tool " + name, r.get(name) != null);
		}
		assertEquals("update documentedToolsArePresent and docs/MCP.md when adding tools", expected.size(), r.size());
	}

	@Test
	public void listToolsGroupsByCategory()
	{
		McpToolRegistry r = registry();
		McpProtocol protocol = new McpProtocol(r, "projectx-osrs", "test");
		McpProtocol.Reply reply = protocol.handlePost("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":"
			+ "{\"name\":\"list_tools\",\"arguments\":{\"category\":\"world\"}}}", null, null);
		JsonObject result = new JsonParser().parse(reply.body).getAsJsonObject().getAsJsonObject("result");
		assertFalse(result.get("isError").getAsBoolean());
		JsonObject listing = new JsonParser().parse(result.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString()).getAsJsonObject();
		JsonObject categories = listing.getAsJsonObject("categories");
		assertEquals(1, categories.size());
		assertTrue(categories.has("world"));
	}
}
