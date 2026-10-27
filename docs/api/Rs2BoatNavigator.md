# Rs2BoatNavigator (sailing)

> **Old name.** Since plugin API 2.7.0 scripts use the short `projectx.bot` names; this `Rs2BoatNavigator` page documents the older, still working, deprecated name. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).

## [Back](../development.md)

## Overview

Sails the player's boat to a place, the way `Rs2Walker` walks the player there. Available from
client **1.0.49** (plugin API **2.6.30**); set `minClientVersion` to `"2.6.30"`.

- **Long trips** follow the sea lanes: every stored port-to-port route (`PortPaths`) is a
  known-safe line of water, and together they form a network searched for the shortest way.
  Routes to ports the player lacks the Sailing level for are left out.
- **The ends of a trip**, and anything within a scene, are planned on the loaded scene: the boat
  keeps a tile clear of the shore and goes round headlands and islands instead of into them.
- **Steering** uses the game's 16 headings, aimed at a planned tile up to 18 tiles ahead. A boat
  that stops making progress is turned towards the most open water and tried again (up to four
  times) before the trip is reported unreachable.

### Walking from a boat

You often don't need this class. `Rs2Walker.walkTo` checks for a boat first:

- If the destination is **at sea**, it sails there with `sailTo`.
- If it is **on land**, it goes ashore first, then walks. When the boat is moored it takes the
  gangplank. Otherwise it sails to the port nearest the destination and moors there.

So quest steps and scripts that call `walkTo` work the same on a boat as on land.

## Methods

### `sailTo(WorldPoint target, int distance)`
- **Description**: Sails to `target` and lowers the sails there. Blocks until the boat arrives,
  gets stuck for good, or the thread is interrupted. Takes the helm if needed.
- **Returns**: `WalkerState`. `ARRIVED` within `distance` (at least 4 tiles; a boat turns wide).
  `UNREACHABLE` when stuck. `EXIT` when not on a boat, logged out or interrupted.

### `step(WorldPoint target, int distance)`
- **Description**: One pass of the same trip, returning at once: takes the helm, steers, checks
  for being stuck. Call it once per loop pass from a script that runs its own loop. Sails are
  left up on arrival; call `stopSailing()` or `leaveHelm(boat)` when you're done.
- **Returns**: `WalkerState`, with `MOVING` while under way.

### `goAshore(WorldPoint destination)`
- **Description**: Gets off the boat for a walk to `destination`: straight down the gangplank
  when moored, otherwise by sailing to the nearest port first.

### `getBoat()` / `isAboard()`
- **Description**: The boat the player is on (`Rs2BoatModel`), or null / false.

### `getBoatLocation()`
- **Description**: Where the boat is in the main world. While on a boat,
  `Rs2Player.getWorldLocation()` reads coordinates in the boat's own world, so use this instead.

### `isMainWorld(WorldPoint point)`
- **Description**: True for main-world coordinates (as opposed to a boat's or an instance's).

### `takeHelm(Rs2BoatModel boat)` / `leaveHelm(Rs2BoatModel boat)`
- **Description**: Take the helm (Navigate). Lower the sails and let go of the helm.

### `stopSailing()` / `reset()`
- **Description**: Lower the sails. Forget the trip under way so the next call plans afresh.

## Example

```java
// In a script loop: sail to the Pandemonium, then go ashore.
WorldPoint pandemonium = PortLocation.PANDEMONIUM.getNavigationLocation();
WalkerState state = Rs2BoatNavigator.step(pandemonium, 4);
if (state == WalkerState.ARRIVED) {
    Rs2BoatNavigator.goAshore(pandemonium);
}
```

## Limits

- Planning round the shore reads the scene's collision data: sea is a tile no one can stand on
  and nothing is built on. Where a scene doesn't read that way (instances such as the Barracuda
  Trials), the boat steers straight at the next waypoint.
- Destinations far from every sea lane, or past the loaded scene, are steered towards in a
  straight line until the scene round them loads.
