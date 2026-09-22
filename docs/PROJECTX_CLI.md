# ProjectX CLI

`projectx-cli` is a command-line tool that lets AI agents (and developers) interact with a running ProjectX client in real time. It communicates with the embedded Agent Server plugin over HTTP on localhost, providing access to the game's widget system, inventory, NPCs, objects, banking, walking, dialogues, and more.

The CLI is located at the repository root: `./projectx-cli`

## Prerequisites

1. The ProjectX client must be running
2. The **Agent Server** plugin must be enabled in the plugin list
3. `curl` must be available on the system (standard on Linux/macOS)
4. A POSIX shell (`bash`) — the CLI is a bash script. On Windows, run it from **WSL** or **Git Bash**. macOS users with the default `/bin/bash` 3.2 are supported (the script avoids Bash 4+ features like namerefs).
5. Auth is token-gated. The plugin writes `~/.runelite/.agent-token`; the CLI reads it automatically.

## Quick Start

```bash
# Check if the server is reachable and the player is logged in
./projectx-cli state

# See what's in the inventory
./projectx-cli inventory

# Find nearby NPCs
./projectx-cli npcs --name Guard --distance 15

# Attack the nearest guard
./projectx-cli npcs interact "Guard" "Attack"

# Eat a lobster
./projectx-cli inventory interact "Lobster" "Eat"

# Walk somewhere
./projectx-cli walk 3222 3218
```

## Configuration

The CLI reads three environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `PROJECTX_HOST` | `127.0.0.1` | Agent Server host |
| `PROJECTX_PORT` | `8081` | Agent Server port (must match plugin config) |
| `PROJECTX_TIMEOUT` | `30` | Request timeout in seconds |
| `PROJECTX_TOKEN` | unset | Override the `X-Agent-Token` value |
| `PROJECTX_TOKEN_FILE` | `~/.runelite/.agent-token` | Token file path |

Example with custom port:

```bash
PROJECTX_PORT=9090 ./projectx-cli state
```

## Command Reference

### Game State

```bash
# Full player state: health, position, animation, combat level
./projectx-cli state

# All skill levels, boosted levels, and XP
./projectx-cli skills

# Single skill
./projectx-cli skills --name Attack
```

### Widget Inspection

Widgets are the game's UI elements (interfaces, buttons, toggles, text). Use these commands to explore and interact with any game interface at runtime.

```bash
# List all currently visible widget interfaces
./projectx-cli widgets list
./projectx-cli widgets list --limit 50 --offset 10

# Search for widgets by keywords (results ranked by relevance)
./projectx-cli widgets search "notification,level-up,pop-up"
./projectx-cli widgets search "bank" --limit 5

# Describe the widget tree under a specific widget
./projectx-cli widgets describe 134 0
./projectx-cli widgets describe 134 0 --depth 3

# Click a widget by group/child ID
./projectx-cli widgets click 134 42

# Click a widget by text content
./projectx-cli widgets click --text "Toggle"
```

**Typical widget workflow:**

1. Search for the widget you need: `widgets search "notifications"`
2. Drill into the match: `widgets describe <groupId> <childId> --depth 3`
3. Find the interactable child and click it: `widgets click <groupId> <childId>`

### Inventory

```bash
# List all items with id, name, quantity, slot, actions
./projectx-cli inventory

# Use/eat/equip an item
./projectx-cli inventory interact "Lobster" "Eat"
./projectx-cli inventory interact "Bronze sword" "Wield"

# Drop a single item
./projectx-cli inventory drop "Logs"

# Drop all of an item
./projectx-cli inventory drop "Logs" --all
```

### NPCs

```bash
# Query nearby NPCs (all within 20 tiles by default)
./projectx-cli npcs

# Filter by name and distance
./projectx-cli npcs --name "Banker" --distance 10
./projectx-cli npcs --name "Guard" --distance 15 --limit 5

# Interact with the nearest matching NPC
./projectx-cli npcs interact "Banker" "Bank"
./projectx-cli npcs interact "Guard" "Attack"
```

### Objects

Game objects include trees, rocks, doors, bank booths, furnaces, and all other world objects.

```bash
# Query nearby objects
./projectx-cli objects
./projectx-cli objects --name "Oak tree" --distance 10

# Interact with the nearest matching object
./projectx-cli objects interact "Oak tree" "Chop down"
./projectx-cli objects interact "Bank booth" "Bank"
```

### Ground Items

```bash
# See what's on the ground nearby
./projectx-cli ground-items
./projectx-cli ground-items --name "Dragon bones" --distance 15

# Pick up the nearest matching item
./projectx-cli ground-items pickup "Dragon bones"
./projectx-cli ground-items pickup "Coins"
```

### Walking

```bash
# Walk to coordinates (x, y, optional plane)
./projectx-cli walk 3222 3218
./projectx-cli walk 3222 3218 1    # plane 1 (upstairs)
```

The walk command is **non-blocking by default** — it starts the walk and returns immediately. Add `--wait` to block until the player arrives (or the timeout elapses, default 30s). The response includes both the destination and the player's final position.

```bash
# Blocking walk — waits until arrival or 60s timeout
./projectx-cli walk 3222 3218 --wait --timeout 60
```

### Banking

```bash
# Check if the bank is open (includes item list when open)
./projectx-cli bank

# Open the nearest bank (waits up to 5 seconds)
./projectx-cli bank open

# Close the bank
./projectx-cli bank close

# Deposit all items in inventory
./projectx-cli bank deposit-all

# Deposit a specific item
./projectx-cli bank deposit "Logs"

# Withdraw items (name + quantity)
./projectx-cli bank withdraw "Pure essence" 28
./projectx-cli bank withdraw "Coins" 1000
```

### Dialogue

```bash
# Check current dialogue state (options, text, continue button)
./projectx-cli dialogue

# Click the continue button
./projectx-cli dialogue continue

# Select a dialogue option by text
./projectx-cli dialogue select "Buy sword"
./projectx-cli dialogue select "Yes"
```

### Login

```bash
# Check login status (game state, profile, world, error detection)
./projectx-cli login

# Trigger login using active profile
./projectx-cli login now

# Login to a specific world
./projectx-cli login now --world 360

# Login and wait until logged in (blocks until success or timeout)
./projectx-cli login wait --timeout 60
./projectx-cli login wait --timeout 60 --world 450
```

When on the login screen, the status response includes `loginIndex` and `loginError` for detecting issues like non-member accounts on members worlds (`loginIndex: 34`), banned accounts (`loginIndex: 14`), or invalid credentials (`loginIndex: 4`).

### Profiles

```bash
# List all profiles (shows name, member status, world selection, active indicator)
./projectx-cli profile

# Switch the active profile (used by the next login)
./projectx-cli profile switch "player@email.com"
```

After switching, `./projectx-cli login now` will use the newly selected profile. Profile switching is safe while logged in — the current session is unaffected; only the next login uses the new profile.

### Client-Thread Lookup (offline)

Look up whether a specific RuneLite API method needs to be called on the client thread. This subcommand is **fully offline** — it does not require the game client or Agent Server to be running. It reads `docs/client-thread-lookup.tsv`, which is regenerated by `./gradlew :client:runClientThreadScanner`.

```bash
# Exact match (default) — single method by name
./projectx-cli ct getItems

# Class.method shorthand auto-splits into class filter + method name
./projectx-cli ct Player.getName

# Substring matching — useful for exploring an API surface
./projectx-cli ct getItem --like

# Restrict matches to a specific class (substring)
./projectx-cli ct getName --class Player

# Filter by evidence kind (only show methods with the strongest signal)
./projectx-cli ct getName --evidence ASSERT

# Machine-readable output for tooling / scripts
./projectx-cli ct getDynamicChildren --json
```

Each match prints the fully-qualified method, the evidence types backing the verdict, and a confidence rating:

```
net.runelite.api.ItemContainer.getItems(): Item[]
  Evidence: ASSERT,LAMBDA,SUBSCRIBE
  Callers : 20
  Verdict : REQUIRED (asserted)
```

**Evidence kinds:**

| Tag | Meaning |
|---|---|
| `ASSERT` | A method in this repo wraps the call with `assert client.isClientThread()` (highest confidence — throws `AssertionError` off-thread under `-ea`) |
| `SUBSCRIBE` | A `@Subscribe`-annotated event handler calls this method; RuneLite's event bus dispatches handlers on the client thread |
| `LAMBDA` | A lambda body that was passed to `clientThread.invoke*()` calls this method, transitively |

**Verdicts:**

- **REQUIRED (asserted)** — has `ASSERT` evidence; calling off-thread will fail loudly under assertions
- **Strongly indicated (N signals)** — two or more independent evidence types
- **Likely (single signal)** — only one evidence type; treat as needing the client thread but with lower certainty

**Caveat:** absence from the lookup file does **not** prove a method is safe off-thread. Many RuneLite API methods are silently unsafe with no surrounding wrapper anywhere in this repo, so the scanner cannot detect them. When in doubt, marshal via `ProjectX.getClientThread().runOnClientThreadOptional(...)`.

A typical workflow: a contributor unsure whether their script utility needs to wrap a call in `clientThread.invoke()` runs `./projectx-cli ct <methodName>` and gets an answer in ~10 ms without booting the client. The full inferred-API table lives in `docs/client-thread-manifest.md` if you prefer browsing.

### Script Lifecycle

```bash
# List all projectx plugins with active/enabled status
./projectx-cli scripts

# Start a plugin by class name
./projectx-cli scripts start --class "net.runelite.client.plugins.projectx.example.ExamplePlugin"

# Start a plugin by display name
./projectx-cli scripts start --name "Example"

# Stop a plugin
./projectx-cli scripts stop --class "net.runelite.client.plugins.projectx.example.ExamplePlugin"

# Check plugin status (runtime, state)
./projectx-cli scripts status --class "net.runelite.client.plugins.projectx.example.ExamplePlugin"

# Submit test results
./projectx-cli scripts results post --class "com.hub.MyPlugin" --data '{"passed":true,"kills":10}'

# Retrieve test results
./projectx-cli scripts results --class "com.hub.MyPlugin"
```

These endpoints are designed for the Microbot-Hub automated testing loop: the Hub spawns a client, logs in, starts a script, polls status, and collects results. See `docs/AGENT_SERVER.md` for the full HTTP API.

## Response Format

All responses are JSON. Successful queries return structured data:

```json
{
  "count": 3,
  "total": 3,
  "npcs": [
    {"id": 3010, "name": "Guard", "combatLevel": 21, "distance": 5, "position": {"x": 3215, "y": 3219, "plane": 0}}
  ]
}
```

Action responses indicate success/failure with a reason:

```json
{"success": true, "action": "Attack", "npc": {"name": "Guard", "id": 3010}}
```

```json
{"success": false, "reason": "NPC not found"}
```

Connection errors output:

```json
{"error": "Connection refused - is the Agent Server plugin enabled?"}
```

## Agent Workflow Examples

### Example: Mine copper ore and bank it

```bash
# 1. Check current state
./projectx-cli state

# 2. Walk to mining area
./projectx-cli walk 3285 3365

# 3. Find copper rocks
./projectx-cli objects --name "Copper rocks" --distance 10

# 4. Mine
./projectx-cli objects interact "Copper rocks" "Mine"

# 5. Check if inventory is full
./projectx-cli inventory

# 6. Walk to bank
./projectx-cli walk 3269 3167

# 7. Open bank and deposit
./projectx-cli bank open
./projectx-cli bank deposit-all

# 8. Close bank and repeat
./projectx-cli bank close
```

### Example: Navigate a settings interface

```bash
# 1. Search for the settings widget
./projectx-cli widgets search "settings"

# 2. Click to open settings
./projectx-cli widgets click --text "Settings"

# 3. Search for the specific toggle
./projectx-cli widgets search "notification,pop-up"

# 4. Describe the widget tree around the match
./projectx-cli widgets describe 134 42 --depth 2

# 5. Click the toggle
./projectx-cli widgets click 134 42
```

### Example: Talk to an NPC

```bash
# 1. Find the NPC
./projectx-cli npcs --name "Shop keeper"

# 2. Talk to them
./projectx-cli npcs interact "Shop keeper" "Talk-to"

# 3. Read the dialogue
./projectx-cli dialogue

# 4. Click through dialogue
./projectx-cli dialogue continue

# 5. Select an option
./projectx-cli dialogue select "What do you have for sale?"
```

## Extending the CLI

The CLI is a bash wrapper around `curl` calls to the Agent Server HTTP API. New commands map 1:1 to server endpoints. To add a new command:

1. Add a handler in `agentserver/handler/` (extend `AgentHandler`)
2. Register it in `AgentServerPlugin.startUp()`
3. Add a `case` block in `projectx-cli`

See `docs/AGENT_SERVER.md` for the full HTTP API reference, handler architecture, and server internals.
