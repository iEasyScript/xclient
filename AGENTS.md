# ProjectX

RuneLite fork (base 1.13.4) with a hidden always-on plugin hosting automation scripts. Composite Gradle build, Java 11 target (JDK 17+ to develop). Client version and script API version live in `gradle.properties` (`projectx.version`, `projectx.pluginapi.version`; currently 1.0.79 / 2.7.0).

## Build / validate
- Compile: `./gradlew :client:compileJava`
- Full: `./gradlew buildAll`
- Shaded jar: `./gradlew :client:assemble`
- Tests (opt-in): `:client:runUnitTests`, `:client:runTests`, `:client:runIntegrationTest` (needs running game)

## Non-negotiable rules
- Scripts and docs teach the `projectx.bot` API (`Inventory`, `Bank`, `Player`, `Walker`, `Widgets`, `Dialogue`, `Prayers`, `Objects/Npcs/GroundItems/Players.query()`; models `Npc`, `SceneObject`, `FloorItem`, `PlayerModel` with `interact(action)`). Guide `docs/SCRIPTING.md`, old-to-new map `docs/api-migration.tsv`. The `Rs2*` names and `ProjectX.getRs2XxxCache()` are deprecated aliases: mention them only in old-name tables, and keep them working.
- Never instantiate caches/queryables directly. Client internals may use `ProjectX.getRs2XxxCache().query()` / `.getStream()`. See `runelite-client/src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md`.
- Never click on the client thread; a click from there blocks the game. Dispatch interaction from the script thread.
- The guardrail tests (`ClientThreadGuardrailTest`, `QueryableTerminalGuardrailTest`) must stay green. Regenerate the baseline only after reviewing the diff.
- Never write the name of the third-party project this client was ported from in code, comments or docs.
- Never block/sleep on the client thread.
- Never use static sleeps to wait for game state — use `sleepUntil(condition, timeoutMs)`.
- Keep `ProjectXPlugin` hidden/always-on; don't break its config panel wiring.
- Respect existing Checkstyle/Lombok patterns; don't weaken security (telemetry tokens, HTTP clients).
- Minimal logging; no PII or session identifiers.

## Review priority
- **P0:** client crashes, client-thread blocking, login/world-hop breakage, cache invariant corruption, credential/token exposure.
- **P1:** script loop timing, overlay correctness, plugin discovery/config, shaded-jar packaging, build reproducibility.

## Runtime tooling
- `./projectx-cli` (JSON output) — see `docs/PROJECTX_CLI.md`, HTTP API `docs/AGENT_SERVER.md`, full tool list `docs/AGENT_SCRIPT_TOOLS.md`.
- Agent Server plugin runs on port 8081 by default.
- Offline client-thread lookup: `./projectx-cli ct <method>`.
- Test mode: `-Dprojectx.test.mode=true -Dprojectx.test.script=<PluginName>` → results in `~/.runelite/test-results/`. Protocol: `docs/AGENTIC_TESTING_LOOP.md`.

## Trying a script
Build the script jar and copy it to `%USERPROFILE%\.runelite\projectx-plugins\dev\`, then restart the client (1.0.77+; the My Scripts window lists it under the Local tab since 1.0.78). The plain `projectx-plugins` folder is store-only, and the dev folder refuses jars that reuse a store script's name, package or hash. Scripts using the `bot` API set `minClientVersion = "2.7.0"` in their `@PluginDescriptor`.

## Releasing
CI must pass first. Bump `projectx.version` (and `projectx.pluginapi.version` when the script API changes), then push a tag named after the version. The Release workflow builds and attaches `projectx-<version>.jar`; the launcher downloads `releases/latest`.

## In-game settings
Use the settings search bar — tab indices shift on updates. Verify changes via `./projectx-cli varbit <id>`.

## Before touching `projectx/util/`
Read `docs/entity-guides/README.md`. Add a gotcha there when you fix an entity-assumption bug.

## Docs maintenance
- Keep root `README.md`, `docs/README.md`, and `docs/INDEX.md` short routing pages.
- Put volatile command details, API examples, endpoint lists, generated inventories, and screenshots in the narrowest owning doc.
- Prefer links to owner docs over copying the same guidance into multiple high-level files.

## Deeper guides
- Script authoring & threading: `runelite-client/.../projectx/AGENTS.md`
- State machines (use for 3+ phase scripts): `.../projectx/statemachine/AGENTS.md`
- Architecture: `docs/ARCHITECTURE.md`, `docs/decisions/`
- Script API: `docs/SCRIPTING.md`, `docs/api-migration.tsv`
- Setup: `docs/development.md`, `docs/installation.md`
- Developer portal: https://xclient.dev/osrs/developer/docs
