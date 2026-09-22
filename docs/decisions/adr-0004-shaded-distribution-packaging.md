# ADR 0004: Shaded Distribution Packaging

- Status: Accepted (2026-02-08)

## Context
End users install ProjectX as a single jar. The client depends on multiple modules and native LWJGL artifacts, so an assembled, reproducible output is required. Upstream RuneLite uses shaded packaging; ProjectX extends this with versioned release naming.

## Decision
Produce distribution jars via the `shadowJar` task in `runelite-client`, generating both `*-shaded.jar` and `projectx-<projectx.version>.jar` (`projectxReleaseJar`). Keep this as the primary delivery artifact for releases and local testing.

## Consequences
- Build pipelines and docs reference `runelite-client/build/libs/projectx-<version>.jar` as the canonical output.
- Any changes to packaging (signing, classifiers, contents) must update Gradle tasks and release instructions.
- Developers can rely on `./gradlew :client:assemble` to reproduce end-user artifacts without additional tooling.
