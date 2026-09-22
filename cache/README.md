# cache

What it does: Included build that packages cache tooling/resources required by the RuneLite client (mirrors upstream RuneLite `cache` module).

Public entry points
- Library/tooling jar only; no runtime main.

How to build/test
- From root: `./gradlew -p cache build`
- Included in `./gradlew buildAll`.

Key invariants/constraints
- Must stay version-aligned with `runelite-client` to avoid cache format mismatches.
- Build outputs are consumed during shaded client assembly; avoid breaking task names or published coordinates.

Links
- Architecture: `../docs/ARCHITECTURE.md`
- ADR 0002 (composite build): `../docs/decisions/adr-0002-composite-build-structure.md`
