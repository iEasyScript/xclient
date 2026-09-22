# runelite-gradle-plugin

What it does: Houses custom Gradle plugins used by the client build for assembling resources, indexing, and jar signing.

Public entry points
- Plugin IDs: `net.runelite.runelite-gradle-plugin.assemble`, `net.runelite.runelite-gradle-plugin.index`, `net.runelite.runelite-gradle-plugin.jarsign`.
- Consumed by `runelite-client/build.gradle.kts`.

How to build/test
- From root: `./gradlew -p runelite-gradle-plugin build`
- Included in `./gradlew buildAll`.

Key invariants/constraints
- Keep plugin IDs and task contracts stable; `runelite-client` relies on them for resource overlay, index generation, and shaded packaging.
- Changes should remain compatible with composite build import in IDEs.

Links
- Architecture: `../docs/ARCHITECTURE.md`
- ADR 0002 (composite build): `../docs/decisions/adr-0002-composite-build-structure.md`
