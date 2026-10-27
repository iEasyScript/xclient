# Development

Local setup, build commands, and script authoring entry points.

## Prerequisites
- JDK 17+ for development. The code targets Java 11 bytecode where RuneLite does.
- Git and the included Gradle wrapper (`./gradlew`); no system Gradle needed.
- IntelliJ IDEA is recommended. Open the root `build.gradle.kts` as a Gradle project.

## Project Layout
- Core plugin: `runelite-client/src/main/java/net/runelite/client/plugins/projectx`
- Helpers/utilities: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/util`
- Queryable API: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/api`
- Config UI: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/ui/ProjectXConfigPanel`
- Runtime agent server: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/agentserver`
- Composite build members: `cache`, `runelite-api`, `runelite-gradle-plugin`, `runelite-jshell`, and `:client`

## Build & Run
- Quick compile: `./gradlew :client:compileJava`
- Run client: `./gradlew :client:run`
- Full build, including included builds: `./gradlew buildAll`
- Clean everything: `./gradlew cleanAll`
- Unit tests: `./gradlew :client:runUnitTests`
- Live/integration tests: `./gradlew :client:runIntegrationTest` with a running client where required
- Shaded jar: `./gradlew :client:assemble`

## IDE Setup (IntelliJ)
1. Open the root `build.gradle.kts`.
2. Set Project SDK and Gradle JVM to JDK 17+.
3. Let IntelliJ import included builds.
4. Create a Gradle run configuration for `:client:run` when launching from the IDE.

## Developing Scripts
- Write scripts against the `projectx.bot` package (`Inventory`, `Bank`, `Player`, `Walker`, `Objects.query()`, `Npcs.query()`, ...). Guide: [SCRIPTING.md](SCRIPTING.md); old `Rs2*` name map: [api-migration.tsv](api-migration.tsv). Set `minClientVersion = "2.7.0"` for scripts that use it.
- Standalone scripts: start from the [script template](https://github.com/iEasyScript/xclient-script-template), build a jar and copy it to `%USERPROFILE%\.runelite\projectx-plugins\dev\` (client 1.0.77+, restart the client; the My Scripts Local tab shows it since 1.0.78). The plain `projectx-plugins` folder is store-only.
- Scripts bundled in this repo live in `runelite-client/src/main/java/net/runelite/client/plugins/projectx`; reusable helpers belong in `projectx/util`, config UI in `projectx/ui/ProjectXConfigPanel`.
- Never instantiate caches or queryables directly. Scripts use `Objects.query()` and friends; client internals use `ProjectX.getRs2XxxCache().query()` or `.getStream()`.
- When a filter resolves live names or widget text, prefer the `*OnClientThread` terminal helpers.
- Use `runelite-client/src/main/java/net/runelite/client/plugins/projectx/AGENTS.md` for script/threading rules.
- Use `runelite-client/src/main/java/net/runelite/client/plugins/projectx/statemachine/AGENTS.md` for scripts with three or more phases.

## Releasing
Releases are tag-driven. Make sure CI passes on `main`, set `projectx.version` (and `projectx.pluginapi.version` when the script API changed) in `gradle.properties`, then tag the commit with the version (for example `1.0.79`). The Release workflow builds the shaded jar and attaches `projectx-<version>.jar`; the launcher downloads `releases/latest`.

## Guardrails
- Client-thread guardrail: `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.projectx.threadsafety.ClientThreadGuardrailTest`
- Regenerate client-thread baseline only after reviewing the diff: `./gradlew :client:regenerateClientThreadGuardrailBaseline`
- Queryable terminal guardrail: `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.projectx.threadsafety.QueryableTerminalGuardrailTest`
- Offline API lookup: `./projectx-cli ct <method>`

## Additional References
- Installation steps and launcher notes: `docs/installation.md`
- API guide and examples: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/api`
- Example scripts: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/example/`
