# runelite-api

What it does: Provides the RuneLite public API artifacts consumed by `runelite-client` and plugins (interfaces, events, data models).

Public entry points
- Library jar only; no runtime main.
- Published via included-build coordinates `net.runelite:runelite-api`.

How to build/test
- From repo root: `./gradlew -p runelite-api build`
- Part of aggregate tasks: `./gradlew buildAll`
- Tests mirror upstream RuneLite defaults.

Key invariants/constraints
- API surface must stay compatible with the client; changes here ripple to `runelite-client` and plugin code.
- Avoid ProjectX-specific types creeping into the shared API unless intentionally upstreamed.

Links
- Architecture: `../docs/ARCHITECTURE.md`
- ADR 0002 (composite build): `../docs/decisions/adr-0002-composite-build-structure.md`
