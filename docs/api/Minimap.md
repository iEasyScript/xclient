# Minimap Class Documentation

> Also known as `Rs2MiniMap`, the older name, which still works but is deprecated. Scripts use the short `projectx.bot` names since plugin API 2.7.0. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).


## [Back](../development.md)
## Overview
The `Minimap` class provides methods for translating in-game coordinates into mini-map points in the ProjectX client. This functionality is essential for navigation and pathfinding tasks that require mini-map interaction.

## Methods

### `localToMinimap(LocalPoint localPoint)`
- **Description**: Converts a `LocalPoint` to its corresponding point on the mini-map.
- **Parameters**:
    - `localPoint`: `LocalPoint` - The local point within the current region.
- **Returns**: `Point` - The mini-map coordinates of the specified local point. Returns `null` if the input is `null` or if the point cannot be converted.

