# Contributing to Project X

Thanks for your interest in contributing. Please read this before opening a pull request.

## Scope

- This repository accepts **client changes only**: the engine, the `projectx.bot` scripting API,
  tooling and docs. Scripts do not belong here; build them from the
  [script template](https://github.com/iEasyScript/xclient-script-template) instead.
- Developer docs: [`docs/SCRIPTING.md`](../docs/SCRIPTING.md) (script API),
  [`docs/development.md`](../docs/development.md) (build and test).

## Flow

1. Fork the repository and create a branch in your fork.
2. Make a small, focused change. Follow the existing code style (Checkstyle, Lombok).
3. Run the checks below and make sure they pass.
4. Open a pull request against `main`. A maintainer reviews and merges it.

```
./gradlew :client:compileJava
./gradlew :client:runUnitTests
```

## Rules that apply to every change

- Never block or sleep on the client thread. The client-thread guardrail test must stay green.
- Scripts reach the game through the `projectx.bot` package; do not instantiate caches or queryables directly.
- Do not commit secrets, tokens or account details.

## Releases

Only maintainers release: CI must pass on `main`, then the version in `gradle.properties` is
bumped and a tag named after it (for example `1.0.79`) is pushed. The Release workflow builds
and attaches `projectx-<version>.jar`.
