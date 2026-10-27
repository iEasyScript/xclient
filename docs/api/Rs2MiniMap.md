# Rs2MiniMap Class Documentation

> **Old name.** Since plugin API 2.7.0 scripts use the short `projectx.bot` names; this `Rs2MiniMap` page documents the older, still working, deprecated name. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).

## [Back](../development.md)
## Overview
The `Rs2MiniMap` class provides methods for translating in-game coordinates into mini-map points in the ProjectX client. This functionality is essential for navigation and pathfinding tasks that require mini-map interaction.

## Methods

### `localToMinimap(LocalPoint localPoint)`
- **Description**: Converts a `LocalPoint` to its corresponding point on the mini-map.
- **Parameters**:
    - `localPoint`: `LocalPoint` - The local point within the current region.
- **Returns**: `Point` - The mini-map coordinates of the specified local point. Returns `null` if the input is `null` or if the point cannot be converted.

