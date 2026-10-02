# Rs2Antiban Class Documentation

## [Back](development.md)

## Overview
The `Rs2Antiban` class provides a comprehensive anti-ban system that simulates human-like behavior during various in-game activities. This system includes features such as mouse fatigue, random intervals, micro-breaks, action cooldowns, and contextually aware mouse movements, all aimed at reducing the risk of detection.

## Methods

### `actionCooldown`
- **Signature**: `public static void actionCooldown()`
- **Description**: Handles the execution of an action cooldown based on anti-ban behaviors. Controls the flow for activating the cooldown either with certainty or based on a chance. Includes logic to adjust behaviors such as non-linear intervals, behavioral variability, and random mouse movements.

### `activateAntiban`
- **Signature**: `public static void activateAntiban()`
- **Description**: Activates the antiban system.

### `checkForCookingEvent`
- **Signature**: `public static boolean checkForCookingEvent(ChatMessage event)`
- **Description**: Checks if a chat message corresponds to a cooking event.

### `deactivateAntiban`
- **Signature**: `public static void deactivateAntiban()`
- **Description**: Deactivates the antiban system.

### `isIdle`
- **Signature**: `public static boolean isIdle()`
- **Description**: Checks if the player is currently idle.

### `isIdleTooLong`
- **Signature**: `public static boolean isIdleTooLong(int timeoutTicks)`
- **Description**: Checks if the player has been idle for too long based on a specified timeout in ticks.

### `isMining`
- **Signature**: `public static boolean isMining()`
- **Description**: Checks if the player is currently performing a mining animation.

### `isWoodcutting`
- **Signature**: `public static boolean isWoodcutting()`
- **Description**: Checks if the player is currently performing a woodcutting animation.

### `moveMouseOffScreen`
- **Signature**: `public static void moveMouseOffScreen()`
- **Description**: Moves the mouse off the screen with a 100% chance to trigger. Used to simulate a user taking a break.

### `moveMouseOffScreen`
- **Signature**: `public static void moveMouseOffScreen(double chance)`
- **Description**: Moves the mouse off the screen based on a specified chance percentage.

### `moveMouseRandomly`
- **Signature**: `public static void moveMouseRandomly()`
- **Description**: Moves the mouse randomly based on the settings. Used to simulate natural mouse behavior.

### `renderAntibanOverlayComponents`
- **Signature**: `public static void renderAntibanOverlayComponents(PanelComponent panelComponent)`
- **Description**: Renders an overlay component that displays various anti-ban settings and information within a panel. Populates a `PanelComponent` with details regarding the current anti-ban system's state.

### `resetAntibanSettings`
- **Signature**: `public static void resetAntibanSettings()`
- **Description**: Resets all antiban settings to their default values.

### `resetAntibanSettings`
- **Signature**: `public static void resetAntibanSettings(boolean forceReset)`
- **Description**: Resets all antiban settings to their defaults. Without `forceReset`, does nothing while the user has "Override every script" on. Either way it leaves the override itself alone.

### `setActivity`
- **Signature**: `public static void setActivity(@NotNull Activity activity)`
- **Description**: Sets the current activity and adjusts antiban settings based on the activity type. With attention span on, the play style starts at the one the activity's intensity calls for.

### `setActivityIntensity`
- **Signature**: `public static void setActivityIntensity(ActivityIntensity activityIntensity)`
- **Description**: Sets the intensity level of the current activity, and turns dynamic intensity off. Use this to pin an intensity from a script.

### `updateActivityIntensity`
- **Signature**: `public static void updateActivityIntensity(ActivityIntensity activityIntensity)`
- **Description**: Changes the intensity without touching dynamic intensity. Used by the antiban system's own updates.

### `takeMicroBreakByChance`
- **Signature**: `public static boolean takeMicroBreakByChance()`
- **Description**: Attempts to trigger a micro-break based on a random chance, as configured in settings. Simulates human-like pauses.

## Your settings and a script's settings

Scripts write `Rs2AntibanSettings` fields directly. The user's own settings are kept separately, and change only through the panel (`Rs2AntibanSettings.userChange`). That separation means:

- The panel shows whether a script's values are live, and a script's values are never saved as the user's.
- The user's settings come back when a script plugin stops (`Rs2AntibanSettings.restoreUserSettings`).
- With "Override every script" on, the user's settings are put back every game tick and before every `actionCooldown`, `takeMicroBreakByChance` and `setActivity`. Scripts still set the activity.
