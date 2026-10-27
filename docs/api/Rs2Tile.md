## [Back](../development.md)

> **Old name.** Since plugin API 2.7.0 scripts use the short `projectx.bot` names; this `Rs2Tile` page documents the older, still working, deprecated name. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).

### `isWalkable(Tile tile)`
- **Parameters**:
    - `Tile`: `tile` - Represents a tile in the game.
- **Returns**:
  - true if the tile is walkable

### `isWalkable(LocalPoint localPoint)`
- **Parameters**:
    - `LocalPoint`: `localPoint` - A two-dimensional point in the local coordinate space.
- **Returns**:
    - true if the localPoint is walkable