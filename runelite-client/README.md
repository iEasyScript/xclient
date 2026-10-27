# runelite-client

What it does: Main RuneLite client fork that embeds the hidden, always-on ProjectX plugin, overlays, and config panels. Builds shaded artifacts for end users.

Public entry points
- `net.runelite.client.RuneLite` (application main class)
- `net.runelite.client.plugins.projectx.ProjectXPlugin` (plugin descriptor/boot)
- Gradle tasks: `:client:runDebug`, `:client:assemble` (`shadowJar` + `projectxReleaseJar`)

How to run/test
- Fast compile: `./gradlew :client:compileJava`
- Run with JDWP: `./gradlew :client:runDebug`
- Assemble shaded jars: `./gradlew :client:assemble`
- Default `test` tasks are disabled; use `./gradlew :client:runUnitTests` (or `:client:runTests`, `runDebugTests` for debugger).

Key invariants/constraints
- Never block/sleep on the client thread; use script/executor threads and `ClientThread.runOnClientThreadOptional` for client access.
- Scripts use the `projectx.bot` API (`Objects.query()`, `Npcs.query()`, `Inventory`, ...; see `../docs/SCRIPTING.md`); client internals use the caches via `ProjectX.getRs2XxxCache().query()`/`.getStream()`. Do not instantiate caches/queryables manually.
- Preserve hidden/always-on plugin descriptor and ProjectX config panel wiring.
- Version and commit values are injected during `processResources`; keep `gradle.properties` values in sync when packaging.

Links
- Architecture: `../docs/ARCHITECTURE.md`
- ADRs: `../docs/decisions/`
- Script API guide: `../docs/SCRIPTING.md`
- Cache API guide: `src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md`
