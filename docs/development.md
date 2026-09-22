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
- Place new scripts inside the projectx plugin folder: `runelite-client/src/main/java/net/runelite/client/plugins/projectx`.
- Reusable helpers belong in `projectx/util`.
- Config UI goes in `projectx/ui/ProjectXConfigPanel`.
- Never instantiate caches or queryables directly. Use `ProjectX.getRs2XxxCache().query()` or `.getStream()`.
- When a filter resolves live names or widget text, prefer the `*OnClientThread` terminal helpers.
- Use `runelite-client/src/main/java/net/runelite/client/plugins/projectx/AGENTS.md` for script/threading rules.
- Use `runelite-client/src/main/java/net/runelite/client/plugins/projectx/statemachine/AGENTS.md` for scripts with three or more phases.

## Guardrails
- Client-thread guardrail: `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.projectx.threadsafety.ClientThreadGuardrailTest`
- Regenerate client-thread baseline only after reviewing the diff: `./gradlew :client:regenerateClientThreadGuardrailBaseline`
- Queryable terminal guardrail: `./gradlew :client:runUnitTests --tests net.runelite.client.plugins.projectx.threadsafety.QueryableTerminalGuardrailTest`
- Offline API lookup: `./projectx-cli ct <method>`

## Additional References
- Installation steps and launcher notes: `docs/installation.md`
- API guide and examples: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/api`
- Example scripts: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/example/`
