# ADR 0002: Composite Build Structure

- Status: Accepted (2026-02-08)

## Context
The project mirrors upstream RuneLite by using included builds (`cache`, `runelite-api`, `runelite-gradle-plugin`, `runelite-jshell`) plus the `:client` project. Flattening or vendoring these modules would simplify the tree but risk divergence from upstream tooling and artifacts.

## Decision
Keep the composite Gradle setup defined in `settings.gradle.kts`, treating included builds as first-class modules wired via `common.settings.gradle.kts` for shared configuration.

## Consequences
- Contributors work inside the existing multi-project layout; IDEs must import included builds.
- Shared tasks like `buildAll`/`cleanAll` orchestrate across modules; single-module builds remain available for quick iterations (e.g., `:client:compileJava`).
- Dependency version alignment stays consistent with upstream RuneLite release practices.
