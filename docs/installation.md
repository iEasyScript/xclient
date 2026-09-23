# Installation

## Download A Release

Download the latest shaded release jar from https://github.com/iEasyScript/xclient/releases.

Nightly builds are development builds. Use a release build unless you are intentionally testing new changes.

## Java

Install Java 17+ and run the shaded jar:

```bash
java -jar client-<version>-SNAPSHOT-shaded.jar
```

## Jagex Account Flow

1. Log in with the Jagex Launcher once. This creates the account token used by RuneLite-compatible clients.
2. Close the launcher/client after a successful login.
3. Open the ProjectX shaded jar. It should prompt with the Jagex account login flow.

Video walkthroughs:
- Jagex account setup: https://www.youtube.com/watch?v=ga-lg1oAnhM
- Java/client launch: https://www.youtube.com/watch?v=EbtdZnxq5iw

## Jagex Launcher Replacement

Replace the official `RuneLite.jar` with the ProjectX jar and keep the filename expected by the launcher. Then start the Jagex Launcher and select RuneLite.

## Linux

- Install Java 17+ from your package manager, then run the shaded jar with `java -jar`.
- For Jagex accounts, Bolt can launch a custom RuneLite jar: https://github.com/Adamcake/Bolt
- You can also extract the RuneLite AppImage and replace `RuneLite.jar` with the ProjectX jar.

**Are you stuck? Join our [Discord](https://discord.gg/zaGrfqFEWE) server.**
