# Rs2Dialogue Class Documentation

> **Old name.** Since plugin API 2.7.0 scripts use the short `projectx.bot` names; this `Rs2Dialogue` page documents the older, still working, deprecated name. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).

## [Back](../development.md)
## Overview
The `Rs2Dialogue` class provides methods to handle in-game dialogue interactions, allowing for checking if a dialogue is present and automating responses.

## Methods

### `clickContinue`
- **Signature**: `public static void clickContinue()`
- **Description**: Simulates pressing the spacebar to continue through dialogues when the "Click here to continue" prompt is visible.

### `hasSelectAnOption`
- **Signature**: `public static boolean hasSelectAnOption()`
- **Description**: Checks if the "Select an Option" dialogue is currently on the screen, indicating a choice needs to be made by the player.

### `isInDialogue`
- **Signature**: `public static boolean isInDialogue()`
- **Description**: Determines if any dialogue is active by checking for common dialogue prompts such as "Click here to continue" or "please wait...".

## Additional Details
These methods are useful for automating routine interactions with game dialogues, facilitating smoother gameplay and interaction handling in scripts.
