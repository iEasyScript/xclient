# MCP Server

The **MCP Server** plugin (called "Agent Server" in older clients) lets AI agents such as Claude Code
read and drive a running Project X OSRS client through the
[Model Context Protocol](https://modelcontextprotocol.io). It offers 99 tools: game state,
the local player, inventory/equipment/bank, NPCs, objects, ground items, players and projectiles,
walking, widgets and menus, dialogue, keyboard and camera, varps/varbits, waits, chat, the client's own
session log, script control and settings, login, breaks, the Grand Exchange and screenshots.

The same plugin still serves the older REST API used by `projectx-cli` on the same port; see
[AGENT_SERVER.md](AGENT_SERVER.md). That API is legacy: it keeps working, but new tooling should use MCP.

## Enable it

1. Start the client.
2. In the Project X plugin list, turn on **MCP Server**.
3. The client log shows `MCP server bound to 127.0.0.1:8081 (MCP endpoint http://127.0.0.1:8081/mcp)`.

Settings (plugin panel; the config group is still `agentServer`, so older settings carry over):

| Setting | Default | Notes |
|---------|---------|-------|
| Port | `8081` | Bound to `127.0.0.1` only. Another client already on the port: this one skips starting. |
| Max Results | `200` | Default `limit` for list tools. |
| Auth token | generated | See below. Clear it to generate a new one. |
| Stealth bind | off | When on, the socket only opens while a script runs, so MCP clients cannot connect before that. |
| Bind mode | `TCP` | Keep `TCP` for MCP clients. `UDS` binds a Unix socket at `~/.runelite/.agent.sock` instead. |

## Endpoint and transport

- **URL:** `http://127.0.0.1:8081/mcp` (the configured port).
- **Transport:** MCP Streamable HTTP, protocol `2025-06-18` (also accepts `2025-03-26`).
  - `POST /mcp` takes a JSON-RPC 2.0 message (or a batch) and answers with `application/json`.
    Notifications and client responses get `202 Accepted` with no body.
  - `GET /mcp` returns `405`: the server never opens a server-initiated stream.
  - `DELETE /mcp` with `Mcp-Session-Id` ends a session.
  - `initialize` returns an `Mcp-Session-Id` header; clients send it back. An id from before a
    plugin restart is accepted, so a client does not have to reconnect after one.
  - An `MCP-Protocol-Version` header with an unsupported version gets `400`.
- **Methods:** `initialize`, `notifications/initialized`, `ping`, `tools/list`, `tools/call`.
  Capabilities: `tools` only. Unknown methods get JSON-RPC `-32601`; unknown tools and bad
  arguments `-32602`; malformed JSON `-32700` (HTTP 400).
- **Results:** a `text` content block holding JSON (keys in `snake_case`); `screenshot` adds an
  `image` block (base64 PNG). A tool that ran but could not do the job (nothing found, not logged in,
  click not sent) returns `isError: true` with an `error` message, so the agent can read why.
- Implemented in plain Java on the JDK HTTP server (no extra libraries), in
  `runelite-client/.../projectx/agentserver/mcp/`.

## Auth

Every request needs the token, in either header:

- `Authorization: Bearer <token>` (MCP clients)
- `X-Agent-Token: <token>` (`projectx-cli`, REST)

The token is generated on first start, kept in the plugin settings (so it stays the same across
restarts) and written to `~/.runelite/.agent-token` while the plugin runs (the file is removed on
shutdown).

Requests with a missing or wrong token, an `Origin` header (browsers), or a non-loopback `Host`
header all get the same plain `404 Not Found` as an unknown path. That is deliberate: a port
scanner cannot tell the server is there without the token. If an MCP client reports a 404 on
connect, check the token first.

## Register it in Claude Code

With the client running and the plugin on, from a terminal:

```bash
claude mcp add --transport http projectx-osrs http://127.0.0.1:8081/mcp \
  --header "Authorization: Bearer $(cat ~/.runelite/.agent-token)"
```

Or put it in the project's `.mcp.json`, keeping the token out of the file:

```json
{
  "mcpServers": {
    "projectx-osrs": {
      "type": "http",
      "url": "http://127.0.0.1:8081/mcp",
      "headers": {
        "Authorization": "Bearer ${PROJECTX_MCP_TOKEN}"
      }
    }
  }
}
```

and export the token before starting Claude Code:

```bash
export PROJECTX_MCP_TOKEN="$(cat ~/.runelite/.agent-token)"     # bash / Git Bash
$env:PROJECTX_MCP_TOKEN = Get-Content "$HOME\.runelite\.agent-token"   # PowerShell
```

`./projectx-cli mcp` prints the URL and this snippet. Long tool calls (`walk_to` with `wait`, `login`,
`reload_scripts`) can take up to a few minutes; raise the client's tool timeout if it is lower
(Claude Code: `MCP_TOOL_TIMEOUT`, in milliseconds).

## How the tools behave

- **Coordinates.** `tile` is the tile as the game reports it for NPCs, objects, ground items and the
  local player; inside an instance that is the instance copy. Inside an instance `map_tile` adds the
  original map location. `distance` is the tile distance between `tile`s, so it is right inside
  instances too: compare with `distance`, not by hand.
- **Names.** `name` filters are case-insensitive substrings unless `exact=true`. When several match,
  `interact_*` and `find_closest_*` prefer exact-name matches, then the nearest.
- **Threading.** Reads run on the client thread in one short call. Clicks, typing and waits run on
  the MCP request thread, never on the client thread. Every wait has a timeout (max 120 s; `walk_to`
  up to 300 s).
- **Actions return when the click is sent.** Follow them with a `wait_for_*` tool, `get_dialogue`
  or `get_local_player` to see the result.
- **Chat** is recorded while the plugin runs (last 500 messages); messages from before it started
  are not available.
- **Reuse.** Tools that match a REST route call the same handler in-process, so both APIs behave
  the same.

## Tools

`list_tools` returns this list from the running client, grouped by category.

### game

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_game_state` | - | One-call snapshot: game_state, logged_in, world and world types, tick, login_index on the login screen, and a player summary (name, tile, map_tile, in_instance, animation, moving, hp, prayer, run energy, combat level), plus scripts_paused. Start here. |
| `get_local_player` | - | The local player in detail: name, combat_level, tile/map_tile/in_instance, animation, pose_animation, moving, idle, interacting target, hitpoints and prayer (current/max), run energy and run_enabled, special_attack, weight, skull, overhead prayer, world. |
| `get_player_position` | - | Where the player is: tile (scene/instance coordinates, the same space as NPCs and objects), map_tile and region_id (original map; inside instances these differ from tile), local and scene coordinates, plane, in_instance, world_view_id. |
| `get_player_stats` | skill | Skills: real_level, boosted_level and xp for each skill (or one, with skill=Attack), total_level, total_xp and combat_level. |
| `get_run_state` | - | Run: run_enabled, run_energy (0-100), stamina_active, weight (kg) and whether the player is moving. |
| `get_definition` | **kind**, **id** | Cache definition of an item, NPC or object by id: name, actions and the main properties (items: stackable, noted/note link, members, prices; NPCs: combat level, size; objects: size, impostor). |

### items

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_inventory` | - | The 28-slot inventory: count, free_slots, full, and items[] {id, name, quantity, slot, stackable, noted, actions}. |
| `get_equipment` | - | Worn equipment: items[] {slot, slot_name (HEAD, WEAPON, ...), id, name, quantity}; empty slots are left out. |
| `get_bank` | name, limit | Bank contents when the bank is open: open, count and items[] {id, name, quantity}. Filter with name (substring) and limit. When closed only open=false is returned; open it with bank_open. |
| `list_item_containers` | - | Item containers the client currently holds (inventory, equipment, bank, GE collection, trade, ...): id, name when known, size and used slots. Use get_item_container to read one. |
| `get_item_container` | **id** | Read any item container by id (see list_item_containers): slot, id, name, quantity for each non-empty slot. |
| `interact_item` | name, id, **action** | Click an option on an inventory item, e.g. 'Eat', 'Wield', 'Drink', 'Bury', 'Empty'. Target by name or id. |
| `drop_item` | **name**, all | Drop one of an inventory item by name, or every one with all=true. |
| `use_item_on` | item_name, item_id, **target_type**, target_name, target_id, max_distance | Use an inventory item on another inventory item, an NPC or an object (e.g. tinderbox on logs, bones on an altar). Item by item_name/item_id; target by target_type plus target_name/target_id. |
| `bank_open` | - | Open the nearest bank (booth, chest or banker) and wait up to 5 s for it. |
| `bank_close` | - | Close the bank window if it is open. |
| `bank_deposit` | name, all | Deposit every item with this name, or the whole inventory with all=true and no name. |
| `bank_withdraw` | name, id, quantity | Withdraw an item by name or id. quantity 0 or less withdraws all. Note: a request for more than the bank stack holds can silently do nothing. |

### world

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `list_npcs` | name, exact, id, max_distance, x, y, limit, interacting_with_me, verbose | List NPCs near the player, nearest first. Each item: index, id, name, combat_level, tile, map_tile (instances only), distance, animation, moving, interacting, interacting_with_me, health_percent, dead, actions. verbose adds pose/orientation/size/graphic. Related: find_closest_npc, interact_npc, get_entity_info. |
| `find_closest_npc` | name, exact, id, max_distance, x, y, reachable | The nearest NPC matching name/id (exact-name matches win over substring matches). Returns found=false when none. reachable=true skips NPCs the player cannot walk to. |
| `interact_npc` | name, exact, id, max_distance, x, y, index, **action** | Click an option on the nearest matching NPC (e.g. action 'Talk-to', 'Attack', 'Bank'). Target by index (from list_npcs), or name and/or id. Returns once the click is sent; check the result with get_local_player, get_dialogue or a wait_for_* tool. |
| `list_objects` | name, exact, id, max_distance, x, y, limit, action, include_unnamed | List game objects (scenery, doors, trees, banks, ...) near the player, nearest first. Each item: id, name, type (GAME/WALL/GROUND/DECORATIVE), tile, map_tile (instances), distance, actions. Unnamed objects are skipped unless include_unnamed=true. Related: find_closest_object, interact_object. |
| `find_closest_object` | name, exact, id, max_distance, x, y, action, reachable | The nearest object matching name/id/action. reachable=true only returns an object the player can walk up to. |
| `interact_object` | name, exact, id, max_distance, x, y, **action** | Click an option on the nearest matching object (e.g. 'Open', 'Chop down', 'Bank'). Use x/y to pick one tile. Returns once the click is sent. |
| `list_ground_items` | name, exact, id, max_distance, x, y, limit | List items on the ground near the player, nearest first. Each item: id, name, quantity, tile, map_tile, distance, noted, stackable, owned, ha_price. |
| `find_closest_ground_item` | name, exact, id, max_distance, x, y | The nearest ground item matching name/id. |
| `interact_ground_item` | name, exact, id, max_distance, x, y, action | Pick up (action 'Take', the default) or use another option on the nearest matching ground item. |
| `list_players` | name, max_distance, limit, verbose | List other players near the local player, nearest first: name, combat_level, tile, distance, animation, interacting, skull, overhead, team. |
| `list_projectiles` | id, max_distance | Projectiles in flight: id, source and target (actor or tile), current tile, distance, remaining_cycles (client cycles, 20 ms each). Use for dodge/prayer logic. |
| `list_spot_anims` | id, max_distance, include_actors | Graphics objects on the ground (spot anims such as falling rocks or AoE markers): id, tile, distance, start_cycle, finished. Pass include_actors=true to also list spot anims on NPCs and players. |
| `get_entity_info` | type, index, name | Everything about one NPC or player: all list fields plus verbose ones, its definition (size, actions, combat level) and active spot anims. Target an NPC by index or name, a player by name. |

### movement

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `walk_to` | **x**, **y**, plane, wait, timeout_s, distance | Walk to a world-map tile with the web walker (shortest path: doors, stairs, shortcuts, transports). Without wait it returns at once and the walk carries on in the background; with wait=true it returns on arrival or after timeout_s (default 30, max 300) with distance_to_destination. distance = how close counts as arrived (default 0). Outside instances use normal map coordinates; inside an instance use walk_click with 'tile' coordinates instead. |
| `walk_click` | **x**, **y**, plane, minimap | One walk click on a nearby tile, no path finding: on the game scene (default) or on the minimap (minimap=true). Takes 'tile' coordinates, so it works inside instances. Returns once clicked. |
| `stop_walking` | - | Stop the current walk_to walk (and clear the walker's route). |
| `is_reachable` | **x**, **y**, plane | Whether the player can walk to a tile from where it stands now (local collision map, current scene only), plus the straight-line distance. For objects, which are solid, test a tile next to them. |

### interfaces

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `list_open_interfaces` | limit, offset | Visible top-level widgets (root layers) with group_id, child_id, type, text, actions and child counts, plus open_group_ids: every interface group currently open. Paged by limit/offset. |
| `get_widget_tree` | **group_id**, child_id, depth | A widget and its children down to depth levels (default 3, max 15): group_id, child_id, index, type, text, name, actions, hidden, sprite_id, item_id, child_count, path. depth=0 describes just the widget. |
| `find_widgets` | text, action, group_id, limit | Search visible widgets by text and/or action (case-insensitive substrings), best match first. Each result has group_id, child_id, index, text, actions, hidden and path. |
| `click_widget` | group_id, child_id, text | Click a widget by group_id + child_id, or the first visible widget whose text matches. Refuses hidden widgets. For a specific menu option on a widget use invoke_widget_action. |
| `invoke_widget_action` | **group_id**, **child_id**, param0, action, identifier | Fire a widget menu option (CC_OP) directly: group_id, child_id, param0 (dynamic child index or slot, -1 for none), action text and identifier (option number, 1-based). E.g. bank Withdraw-All on a slot. |
| `get_menu_entries` | - | The current right-click menu entries, top option first: option, target, type, identifier, param0, param1, item_id; plus menu_open. Copy an entry into invoke_menu_action to replay it. |
| `invoke_menu_action` | **option**, target, **type**, identifier, param0, param1, item_id, world_view_id | Send a raw menu action, as if that menu entry had been clicked. type is a MenuAction name (e.g. NPC_FIRST_OPTION, GAME_OBJECT_FIRST_OPTION, CC_OP, WALK) or its numeric id. Values usually come from get_menu_entries. Low level: prefer interact_* and click_widget. |
| `diff_open_interfaces` | **baseline** | Compare the open interface groups with a baseline list: opened, closed, persisted and current. Take the baseline from list_open_interfaces.open_group_ids before an action. |

### dialogue

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_dialogue` | - | Dialogue state: in_dialogue, has_continue, has_options, dialogue_text, question and options[] (when a choice is shown). |
| `continue_dialogue` | - | Press 'Click here to continue' in the open dialogue. |
| `select_dialogue_option` | option, index | Choose a dialogue option by text (substring) or by its 1-based index (sent as the number key). |

### input

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `key_press` | **key**, hold_ms | Press a key: a name (enter, escape/esc, backspace, space, tab, up, down, left, right, f1-f12, home, end, page_up, page_down, delete, shift, ctrl, alt) or one character ('1', 'a'). hold_ms holds it down (max 5000), e.g. arrow keys to turn the camera. |
| `type_text` | **text**, press_enter | Type text into the game (chat box, number prompts, search boxes). press_enter=true presses Enter after. |
| `get_camera` | - | Camera state: yaw (0-2047, 0 = north, 512 = west, 1024 = south, 1536 = east), pitch and zoom. |
| `set_camera` | yaw, pitch, zoom, face_x, face_y | Move the camera: yaw (0-2047), pitch, zoom, and/or face a tile (face_x, face_y). Only the given values change. Uses the arrow keys like a player, so it takes a moment. |

### variables

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_varp` | **id** | Read a player variable (varp / VarPlayer) by id. |
| `get_varbit` | **id** | Read a varbit by id, with where it lives: value, base_varp, lsb, msb and the raw varp value. |
| `get_var_client` | **id**, type | Read a client variable (VarClientInt, or VarClientStr with type=string). |
| `list_varps` | from, to, non_zero_only, limit | Varp values in an id range (default only non-zero ones). Handy to diff before/after an action. |
| `list_varbits_for_varp` | **varp** | Every varbit stored in one varp: varbit_id, lsb, msb and its current value. The first call builds an index from the cache (a second or two). |

### waits

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `wait_for_varbit_change` | **id**, baseline, timeout_ms, poll_ms | Wait until a varbit differs from baseline (default: its value when the call starts). Returns changed, timed_out, elapsed_ms, baseline, value. |
| `wait_for_varp_change` | **id**, baseline, timeout_ms, poll_ms | Wait until a varp differs from baseline (default: its value when the call starts). |
| `wait_for_widget` | **group_id**, child_id, state, timeout_ms | Wait until a widget is visible (default) or hidden. Returns matched, timed_out, elapsed_ms. |
| `wait_for_interface_open` | group_id, exclude_ids, timeout_ms | Wait until an interface group opens: a given group_id, or any group not in exclude_ids (default: the groups open when the call starts). Returns opened (new group ids), timed_out, elapsed_ms. |
| `wait_for_idle` | timeout_ms, stable_ms, ignore_interacting | Wait until the player stands still: no animation, not moving and (unless ignore_interacting) no target, for stable_ms in a row. Use after an action to let it finish. |
| `wait_for_chat_message` | **pattern**, type, timeout_ms, lookback_ms | Wait for a chat message matching a regex (case-insensitive), e.g. 'You catch\|You fail'. Only messages after the call starts count, unless lookback_ms reaches back. Returns matched, message, timed_out. |

### chat

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_chat_messages` | limit, type, pattern, after_seq | Recent chat messages (game, public, private, clan, ...) received since the MCP server started, oldest first: seq, time, type, name, message. Filter by type (e.g. GAMEMESSAGE), regex pattern, or after_seq (only newer than a seq you saw). |

### logs

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `read_logs` | lines, level, pattern, file | Tail of this client's session log (~/.runelite/logs/projectx/session_*.log). Entries are whole log records (a stack trace stays with its line). Filter by minimum level (DEBUG/INFO/WARN/ERROR) and a regex; lines = max entries returned (default 100, max 2000). file picks an older session by name. |
| `get_session_log_path` | - | Where this client logs: the current session file, the folder, and the most recent session files with sizes. |

### scripts

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `list_scripts` | name, running_only | Installed Project X scripts and built-in plugins: name, class_name, version, active (running), enabled. Filter with name (substring) or running_only. Also returns scripts_paused. |
| `start_script` | class_name, name | Start a script by class_name (fully qualified, from list_scripts) or by name (substring of the display name; the first match wins, so prefer class_name). |
| `stop_script` | class_name, name | Stop a running script by class_name or name. |
| `get_script_status` | class_name, name | One script's status (RUNNING/STOPPED, start time) plus its loop heartbeat: loop_count, last_heartbeat_at and stalled_ms (time since its loop last ran; large = stuck). |
| `get_script_health` | class_name, name | Loop heartbeats of every running script (or one by class_name/name): loop_count, last_heartbeat_at, stalled_ms. |
| `get_script_results` | class_name, name | Results a script submitted through ScriptResultStore (test mode and agent runs). |
| `get_state_machine_snapshot` | script | Live state of StateMachineScript scripts: with script (name substring) the full snapshot (current state, last transition and reason, loop count); without it a summary of every registered one. |
| `pause_scripts` | - | Pause every running script (the same flag breaks use). Scripts stop acting at their next loop. |
| `resume_scripts` | - | Resume scripts after pause_scripts. |
| `reload_scripts` | timeout_s | The side panel's 'Refresh scripts': checks the store for updates and new purchases, then reloads any changed local dev jar from ~/.runelite/projectx-plugins/dev/. Waits up to timeout_s (default 60) and returns the dev jars found. |

### settings

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_plugin_setting` | **group**, **key** | Read one plugin setting (any config group, e.g. a script's group) as stored: group and key. |
| `set_plugin_setting` | **group**, **key**, value | Write one plugin setting (string, number or boolean; null clears it). Takes effect like a change in the settings panel. Returns the stored value. |
| `list_plugin_settings` | **group** | Every stored setting in a config group (key and value). Values of keys that look secret (password, pin, token...) are masked; read one with get_plugin_setting if you need it. |
| `get_client_settings` | - | Project X client options the agent can toggle: disable_level_up_interface, disable_world_switcher_confirmation. |
| `set_client_setting` | **key**, **value** | Set a Project X client option: key disableLevelUpInterface or disableWorldSwitcherConfirmation, boolean value. |
| `level_up_notifications` | enable | Read (no enable argument) or change the in-game level-up notification setting, through the game's settings. |

### quest_helper

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `quest_helper_status` | - | Quest Helper: active/enabled, the selected quest, whether it is completed and the current step text. |
| `quest_helper_start` | **name** | Select a quest in Quest Helper by its display name (see quest_helper_list). |
| `quest_helper_stop` | - | Close the selected Quest Helper quest. |
| `quest_helper_list` | name, with_state | Quests Quest Helper knows (enum and display_name); with_state=true adds each quest's state for this account. |

### deploy

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `list_deployed_scripts` | - | Scripts compiled and loaded at runtime with deploy_script. |
| `deploy_script` | **name**, **source_path** | Compile a script from a source folder on this machine, load it and start it, without restarting the client. |
| `reload_deployed_script` | **name** | Recompile and restart a script loaded with deploy_script. |
| `undeploy_script` | **name** | Stop and unload a script loaded with deploy_script. |

### account

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `get_login_state` | - | Login status: logged_in, game_state, login_attempt_active, login_cooldown_ms, login_duration_ms, login_index and login_error on the login screen, active_profile (name, members, world) and current_world. |
| `login` | world, wait, timeout_s | Log in with the active profile (see list_profiles / switch_profile), optionally to a world. With wait (default true) it returns once logged in and stable, on a fatal login error, or after timeout_s (default 30, max 120). Respects the too-many-attempts cooldown. |
| `logout` | wait, timeout_s | Log out. With wait (default true) returns when the login screen shows or after timeout_s (default 30). |
| `list_profiles` | - | Login profiles (never passwords): name, is_member, selected_world, active. |
| `switch_profile` | **name** | Make a profile active (exact name, case-insensitive); the next login uses it. |
| `get_break_state` | - | Break handlers: for v1 and v2, whether the plugin is enabled, its state and whether a break is active; v1 also break_in_s, break_duration_s, total_breaks and lock_state. Plus scripts_paused. |
| `get_grand_exchange` | - | Grand Exchange: open (the GE window is showing) and the 8 offer slots: slot, state (EMPTY, BUYING, BOUGHT, SELLING, SOLD, CANCELLED_*), item_id, item_name, quantity_sold, total_quantity, price, spent. |

### meta

| Tool | Arguments | What it does |
|------|-----------|--------------|
| `list_tools` | category | Every tool of this server grouped by category, with the first sentence of each description. Pass category to see one group. |
| `get_client_info` | - | Versions and endpoints: Project X client version and commit, script API version, launcher version, Java and OS, the MCP endpoint and protocol, and this session's log file. |
| `screenshot` | max_width, save, label, dir | A screenshot of the game canvas as a PNG image. max_width scales it down (keeps the aspect ratio). save=true writes it to disk instead (default ~/.runelite/test-results/screenshots) and returns the path. |
| `search_game_settings` | **term** | Open the in-game Settings, type term into its search box and return the widgets found (to toggle one, click it with click_widget). Same as 'projectx-cli settings search'. |

99 tools. Arguments in **bold** are required.

## Not included (RS3 client only)

The RS3 client's MCP also has memory reading, packet logs, projection and CS2 debugging tools. They
depend on that client's engine and have no counterpart here.

## Troubleshooting

- **Connection refused:** the plugin is off, the client is not running, stealth bind is on and no
  script runs, or another client took the port first (see the client log).
- **404 on every request:** wrong or missing token, or the client sends an `Origin` header.
- **"The game thread did not answer in time":** the game was loading or stalled; retry.
- **Session log:** `get_session_log_path` / `read_logs`, or `~/.runelite/logs/projectx/`.
