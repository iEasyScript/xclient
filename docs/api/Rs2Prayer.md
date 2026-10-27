# Rs2Prayer Class Documentation

> **Old name.** Since plugin API 2.7.0 scripts use the short `projectx.bot` names; this `Rs2Prayer` page documents the older, still working, deprecated name. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).

## [Back](../development.md)
## Overview
The `Rs2Prayer` class offers functionalities to manage and interact with prayer abilities in the game. It includes methods to toggle prayers on or off, check if prayers are active, and determine if the player is out of prayer points.

## Methods

### `toggle(Rs2PrayerEnum name)`
- **Parameters**:
    - `name`: `Rs2PrayerEnum` - The enum entry representing the prayer to toggle.
- **Description**: Toggles the specified prayer without checking its current state.

### `toggle(Rs2PrayerEnum name, boolean on)`
- **Parameters**:
    - `name`: `Rs2PrayerEnum` - The enum entry representing the prayer to toggle.
    - `on`: `boolean` - Desired state of the prayer; `true` to activate, `false` to deactivate.
- **Description**: Toggles the specified prayer to the desired state. Checks the current state to avoid unnecessary toggling.

### `isPrayerActive(Rs2PrayerEnum name)`
- **Parameters**:
    - `name`: `Rs2PrayerEnum` - The prayer to check.
- **Returns**: `boolean` - True if the specified prayer is currently active.
- **Description**: Checks if the specified prayer is active.

### `isQuickPrayerEnabled`
- **Returns**: `boolean` - True if quick prayers are currently enabled.
- **Description**: Checks if quick prayers are enabled.

### `isOutOfPrayer`
- **Returns**: `boolean` - True if the player has no prayer points left.
- **Description**: Checks if the player is out of prayer points.

### `flickQuickPrayer()`
- **Returns**: `boolean` - False if no quick prayers are set up or there are no prayer points left.
- **Description**: One tick of 1-tick flicking with quick prayers: off and straight back on (just
  on, if they are off), both clicks sent at once. Client 1.0.47+. See [1-tick flicking](#1-tick-flicking).

### `flick(Rs2PrayerEnum prayer)`
- **Parameters**:
    - `prayer`: `Rs2PrayerEnum` - The prayer to flick, e.g. `PROTECT_MELEE`.
- **Returns**: `boolean` - False if there are no prayer points left.
- **Description**: One tick of 1-tick flicking with a single prayer. Flicking a different
  protection prayer from the one that is up swaps them. Client 1.0.47+.

## Usage Examples

### 1-tick flicking
A prayer turned off and back on within the same game tick is up for that tick and does not
drain. Do it every tick and the prayer is always up for free. Two rules:

1. **Once per tick, at the start of it.** Call from an `onGameTick` subscriber.
2. **With [direct input](DirectInput.md),** so both clicks reach the game on the same tick.
   Through the mouse they can straddle two ticks, and the prayer drops or drains.

```java
@Subscribe
public void onGameTick(GameTick tick) {
    if (!underAttack()) {
        return;
    }
    // Quick prayers if any are set up, otherwise the protection prayer for the attacker
    ProjectX.withDirectInput(() -> {
        if (!Rs2Prayer.flickQuickPrayer()) {
            Rs2Prayer.flick(Rs2PrayerEnum.PROTECT_MELEE);
        }
    });
}
```
Turn the prayer off once the fight is over: `Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false)`.

### Toggling a Prayer
```java
Rs2Prayer.toggle(Rs2PrayerEnum.PIETY, true); // Activates Piety
Rs2Prayer.toggle(Rs2PrayerEnum.PIETY, false); // Deactivates Piety
