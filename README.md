<p align="center">
  <img src="https://raw.githubusercontent.com/iEasyScript/xclient/main/.github/crest.png" width="140" alt="Project X">
</p>

<h1 align="center">Project X</h1>

<p align="center">
  Old School RuneScape automation client &mdash; the engine, API and script framework.<br>
  <a href="https://xclient.dev">xclient.dev</a>
</p>

---

Project X is a RuneLite fork with an always-on Project X plugin for learning, building and
running automation scripts. This repository is the **client**: the engine and the API that
scripts are written against. It is open source; scripts sold on the marketplace are not.

This README is intentionally short. Durable details live in the docs below so high-level
context does not rot when implementation details move.

## Writing scripts

Scripts are RuneLite plugins built against this client.

- Start from the template: [xclient-script-template](https://github.com/iEasyScript/xclient-script-template)
- The rules that matter (threading, caches, anti-ban): [`plugins/projectx/AGENTS.md`](runelite-client/src/main/java/net/runelite/client/plugins/projectx/AGENTS.md)
- Query game state: [`api/QUERYABLE_API.md`](runelite-client/src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md)
- Drive a running client while developing: [`docs/PROJECTX_CLI.md`](docs/PROJECTX_CLI.md)

Build the jar your plugin compiles against with `./gradlew :client:assemble`, or take it from
[Releases](https://github.com/iEasyScript/xclient/releases).

## Start Here
- Install or run a release: `docs/installation.md`
- Set up a development environment: `docs/development.md`
- Understand the runtime: `docs/ARCHITECTURE.md`
- Build scripts safely: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/AGENTS.md`
- Use entity caches/queryables: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md`
- Drive a running client: `docs/PROJECTX_CLI.md`

## Common Commands
- Compile client: `./gradlew :client:compileJava`
- Run unit tests: `./gradlew :client:runUnitTests`
- Build all projects: `./gradlew buildAll`
- Build shaded jar: `./gradlew :client:assemble`

## Code Map
- ProjectX plugin and scripts: `runelite-client/src/main/java/net/runelite/client/plugins/projectx`
- Reusable helpers: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/util`
- Queryable caches: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/api`
- Runtime agent tooling: `runelite-client/src/main/java/net/runelite/client/plugins/projectx/agentserver`

## Discord
[![Discord Banner 1](https://discord.com/api/guilds/1521062459446657125/widget.png?style=banner1)]([https://discord.gg/zaGrfqFEWE](https://discord.gg/UUwfkXFcub))

If you have any questions, please join our [Discord]([https://discord.gg/zaGrfqFEWE](https://discord.gg/UUwfkXFcub)) server. 

## Credits and licence

Project X is built on [RuneLite](https://github.com/runelite/runelite) and released under the
BSD 2-Clause licence &mdash; see [LICENSE](LICENSE). Copyright notices are retained throughout
the source, as that licence requires.

Not affiliated with, endorsed by, or sponsored by Jagex/Runescape Ltd.
