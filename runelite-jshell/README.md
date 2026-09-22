# runelite-jshell

What it does: Provides JShell support artifacts used alongside the RuneLite client for interactive evaluation and debugging.

Public entry points
- Library jar only; consumed by `runelite-client` as project dependency `:jshell`.

How to build/test
- From root: `./gradlew -p runelite-jshell build`
- Included in `./gradlew buildAll`.

Key invariants/constraints
- Keep alignment with RuneLite client versions to avoid classpath mismatches.
- No ProjectX-specific logic should leak here; it must stay a generic JShell helper module.

Links
- Architecture: `../docs/ARCHITECTURE.md`
- ADR 0002 (composite build): `../docs/decisions/adr-0002-composite-build-structure.md`
