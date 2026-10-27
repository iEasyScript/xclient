# Installation

## Players

Use the **Project X launcher** from https://xclient.dev. It downloads the latest release of this
client (`releases/latest`), keeps it up to date and starts it.

## Run a release jar by hand

Every release attaches one jar, `projectx-<version>.jar`, to https://github.com/iEasyScript/xclient/releases.
Install Java 17+ and run it:

```bash
java -jar projectx-<version>.jar
```

Use a release, not a local development build, unless you are intentionally testing changes.

## Jagex Account Flow

1. Log in with the Jagex Launcher once. This creates the account token used by RuneLite-compatible clients.
2. Close the launcher/client after a successful login.
3. Open the Project X jar. It should prompt with the Jagex account login flow.

Video walkthroughs:
- Jagex account setup: https://www.youtube.com/watch?v=ga-lg1oAnhM
- Java/client launch: https://www.youtube.com/watch?v=EbtdZnxq5iw

## Jagex Launcher Replacement

Replace the official `RuneLite.jar` with the Project X jar and keep the filename expected by the launcher. Then start the Jagex Launcher and select RuneLite.

## Linux

- Install Java 17+ from your package manager, then run the jar with `java -jar`.
- For Jagex accounts, Bolt can launch a custom RuneLite jar: https://github.com/Adamcake/Bolt
- You can also extract the RuneLite AppImage and replace `RuneLite.jar` with the Project X jar.

## Scripts

Store scripts are installed and updated from the My Scripts window (Store tab). Scripts you build
yourself go in `%USERPROFILE%\.runelite\projectx-plugins\dev\` (client 1.0.77 and later) and show
under the Local tab (1.0.78 and later). See [SCRIPTING.md](SCRIPTING.md) and
https://xclient.dev/osrs/developer/docs.

**Are you stuck? Join our [Discord](https://discord.gg/UUwfkXFcub) server.**
