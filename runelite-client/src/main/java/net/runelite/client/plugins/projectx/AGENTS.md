# ProjectX Plugin & Script Development

Automation plugins live here. Every plugin = **Plugin class** (`@PluginDescriptor`, RuneLite lifecycle) + **Script class** (runs the loop on a background thread) + **Config** + optional **Overlay**.

Config UI uses the custom `ProjectXConfigPanel` (`plugins/projectx/ui`), not RuneLite's default.

## Threading — hard rules
- Game state reads/writes must happen on the client thread: wrap with `ProjectX.getClientThread().runOnClientThreadOptional(...)` or `.invoke(...)`.
- Script loops run on a background executor; **never** block the client thread.
- Waits = `sleepUntil(condition, timeoutMs)`. No fixed `sleep(ms)` to wait on game state.
- Widget methods like `isHidden()`, `getBounds()`, menu entry access → client thread only.

## Cache / Queryable API (mandatory)
- Scripts: use the short names in `projectx.bot` — `Objects.query("Oak tree")`, `Npcs.query()`, `GroundItems.query()`, `Players.query()`, `Inventory`, `Bank`, … (guide: `docs/SCRIPTING.md`, old-to-new map: `docs/api-migration.tsv`; needs `minClientVersion = "2.7.0"`). The `Rs2*` names and `ProjectX.getRs2XxxCache()` are deprecated aliases kept for compiled scripts; do not teach them in new code or docs.
- Client internals: singletons via `ProjectX.getRs2XxxCache()` (NPCs, objects, ground items, inventory, bank, varbits, players, widgets, …).
- Query as `Npcs.query("Banker").where(npc -> !npc.isInteracting()).nearest()` (filters, then `nearest()` / `nearestReachable()` / `first()` / `list()` / `count()` / `interact(action)`), or `Npcs.stream()` — not direct cache instantiation, not raw client iteration. Use the `*OnClientThread` terminals for name searches that run every loop.
- Caches are event-driven; treat returned entities as snapshots, re-query before acting on stale refs.
- Full reference: `runelite-client/.../projectx/api/QUERYABLE_API.md`.

## Interaction utilities
`Npcs`, `Objects`, `GroundItems`, `Inventory`, `Bank`, `Walker`, `Widgets`, `Dialogue`, `Player`, `Prayers`, `Combat`, etc. (package `projectx.bot`; models expose `interact(action)`). Their `Rs2*` equivalents in `util/` are the implementations and the deprecated old names. Prefer these over raw RuneLite APIs — they handle client-thread hopping, retries, and menu-entry correctness.

Check `docs/entity-guides/README.md` before modifying anything under `util/` — each entity type has documented footguns.

**Direct input** (client 1.0.46+): `ProjectX.withDirectInput(() -> ...)` sends the clicks inside it straight to the game with no mouse movement — for actions that must not wait on the mouse, such as prayer flicking (`Prayers.flickQuickPrayer()` / `flick(prayer)`, client 1.0.47+, called once per tick from `onGameTick`). Per thread, restored afterwards. Easier to detect than a mouse click: offer it as a setting. See `docs/api/DirectInput.md`.

## Scripts
- Extend `Script` (or `StateMachineScript<S>` for 3+ phase scripts — see `statemachine/AGENTS.md`).
- Implement `run(TConfig)`; return `true` to signal successful start.
- Always `shutdown()` cleanly: cancel schedulers, clear state, unregister listeners.
- Use `ProjectX.status` for user-visible status; keep logging sparse and at appropriate levels.

## Plugins
- `@PluginDescriptor(name, description, tags, enabledByDefault = false, ...)`.
- Inject dependencies via Guice constructor injection.
- `startUp()` / `shutDown()` must be idempotent and fast — do not launch scripts synchronously there.
- Communicate between plugins via the event bus, not static state.

## Anti-ban & timing
- Use randomized sleeps from `Rand` (old name `Rs2Random`) / `Global.sleep*Gaussian*`.
- Don't hammer menu actions; honor tick timing (`Player.waitForAnimation`, `sleepUntilOnClientThread`).
- Respect blocking events (`BlockingEventManager`) — login, level-up, death, random events pause the script automatically when handlers are registered.

## Error handling
- Wrap script bodies in try/catch; log and stop cleanly rather than propagating into the client thread.
- Never swallow `InterruptedException` — restore the flag and exit the loop.

## Memory / performance
- Don't cache entity refs across ticks; cache by id/name or re-query.
- Unregister event subscribers in `shutDown()`.
- Avoid heavy work inside overlays (`render`) and events fired on the client thread.

## Testing a script
Build its jar and put it in `%USERPROFILE%\.runelite\projectx-plugins\dev\` (client 1.0.77+, restart; Local tab of My Scripts since 1.0.78). Drive it with `./projectx-cli` and read results per `docs/AGENTIC_TESTING_LOOP.md`. Never write the name of the third-party project this client was ported from.

## Commits / versioning
Follow `.github/git-commit-instructions.md`. Bump the plugin version in the relevant `@PluginDescriptor` when behavior changes.

## See also
- Repo-root `AGENTS.md` — non-negotiable rules, build/test, CLI.
- `docs/SCRIPTING.md` — the script API guide; `docs/api-migration.tsv` — old to new names.
- `statemachine/AGENTS.md` — state machine framework.
- `util/settings/AGENTS.md` — widget/settings debugging.
- `docs/AGENT_SCRIPT_TOOLS.md` — runtime tooling for authoring/debugging.
- `docs/entity-guides/` — per-entity footguns.
