# Architecture

## Components & Interaction
- **RuneLite client (`runelite-client`)**: Main application with ProjectX hidden plugin (`ProjectXPlugin`) always enabled; entrypoint `net.runelite.client.RuneLite`. Builds shaded distribution via `shadowJar` + `projectxReleaseJar`.
- **ProjectX runtime**: `ProjectX` singleton exposes caches, utilities, and script lifecycle helpers; `Script` base class drives scheduled script loops; `BlockingEventManager` preempts scripts when required UI/game states are detected.
- **Queryable caches**: Guice-injected caches (`Rs2NpcCache`, `Rs2PlayerCache`, `Rs2TileItemCache`, `Rs2TileObjectCache`, `Rs2BoatCache`, `Rs2PlayerStateCache`) updated per tick and accessed via `ProjectX.getRs2XxxCache().query()` or `.getStream()`. World-view aware for boats.
- **Utilities (`projectx/util`)**: Facades over RuneLite APIs for player, inventory, banking, walking, etc.; expected to run on script threads, not the client thread.
- **Included builds**: `runelite-api` (shared API artifacts), `runelite-gradle-plugin` (assemble/index/jarsign tasks), `cache` (cache tooling), `runelite-jshell` (JShell support). Root Gradle orchestrates via composite build.
- **Telemetry/API**: `ProjectXApi`, `ProjectXVersionChecker`, and related clients can call `https://microbot.cloud/api`; users can disable this with `-Dprojectx.disableTelemetry=true` or the ProjectX config toggle.
- **Agent Server**: Optional local control surface under `agentserver`; defaults to TCP on `127.0.0.1:8081`, token-gates requests, and can run in UDS or stealth-bind modes.

## Key Data Flows
- User starts client → `ProjectXClientLoader` fetches jav_config/world info → RuneLite client loads → `ProjectXPlugin` starts, registers overlays/config panels and initializes caches.
- Script loop (`Script.run()` implementations) executes on scheduled executors → queries caches via Queryable API → performs interactions through utilities (`Rs2Inventory`, `Rs2Walker`, etc.) → waits with `sleepUntil` helpers.
- Blocking events (`BlockingEventManager`) continuously validate (e.g., welcome screen, bank popups) → if triggered, they run on a dedicated executor and block script progression until resolved.
- Telemetry flow: session/version/fact/plugin telemetry is skipped when telemetry is disabled; failures are logged at debug level.

## Runtime Boundaries
- **Threads**: Client thread (never block/sleep); script/executor threads (automation logic, sleeps allowed); blocking-event executor (resolves UI blockers). Use `ClientThread.runOnClientThreadOptional` for safe client access.
- **Services vs libraries**: ProjectX plugin runs inside client process; optional external calls are limited to configured telemetry/API clients; caches are in-memory per client instance.
- **Distribution**: Shaded jar (`*-shaded.jar` and `projectx-<version>.jar`) produced in `runelite-client/build/libs`.

## Configuration & Environments
- Gradle properties: `gradle.properties` holds `projectx.version`, `projectx.commit.sha`, optional repo credentials (`projectx.repo.*`), and `glslang.path` (populated by CI script).
- Runtime config: `ProjectXClientLoader` consumes `RuneLiteProperties` for jav_config URL; falls back to world-supplied hosts on failure. `ProjectXVersionChecker` logs current version/commit on startup unless telemetry is disabled.
- Logging: Game chat logging configurable via `ProjectXConfig` (patterns/levels, projectx-only filter); logback appender wired at plugin startup.

## Non-Obvious Behaviors
- Caches refresh at most once per game tick; repeated queries within the same tick reuse cached lists.
- `BlockingEventManager` uses exponential backoff when no events validate to reduce overhead; events are re-queued if execution fails.
- Default Gradle `test` tasks are disabled in `runelite-client/build.gradle.kts`; use explicit tasks such as `:client:runUnitTests`, `:client:runTests`, `:client:runDebugTests`, `:client:runIntegrationTest`, or `:client:runClientThreadScanner`.
- World hopping short-circuits if the player is interacting or already hopping; confirmation widget auto-click handled in `ProjectX.hopToWorld`.

## References
- Queryable API guide: `../runelite-client/src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md`
- Development setup: `development.md`
- Decision records: `decisions/`
