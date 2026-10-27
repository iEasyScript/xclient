# ProjectX Queryable API

This package owns the cache/query layer for live game entities. It is the preferred way for scripts and utilities to find NPCs, players, ground items, tile objects, boats, and player state.

Scripts reach it through the short entry points in `projectx.bot` (`Objects.query()`, `Npcs.query()`, `GroundItems.query()`, `Players.query()`; plugin API 2.7.0+); see [docs/SCRIPTING.md](../../../../../../../../../../docs/SCRIPTING.md). Client code may use the singleton caches exposed by `ProjectX`. Do not instantiate caches or queryables directly.

## Quick Start

```java
Npc banker = Npcs.query("Banker").nearestOnClientThread();

if (banker != null) {
    banker.interact("Bank");
}
```

Use `*OnClientThread` terminal methods when filters or sorting need live RuneLite data such as names, widgets, world locations, or actions.

## References

- Full guide: [QUERYABLE_API.md](QUERYABLE_API.md)
- Script/threading rules: [../AGENTS.md](../AGENTS.md)
- Entity utility gotchas: [../../../../../../../../../../docs/entity-guides/README.md](../../../../../../../../../../docs/entity-guides/README.md)

## Directory Structure

```
api/
├── README.md
├── QUERYABLE_API.md
├── AbstractEntityQueryable.java
├── IEntity.java
├── IEntityQueryable.java
├── actor/
├── boat/
├── npc/
├── player/
├── playerstate/
├── tileitem/
└── tileobject/
```

## Examples

Check the `*ApiExample.java` files in each subdirectory for complete examples:

- `player/PlayerApiExample.java` - Player query examples
- `tileitem/TileItemApiExample.java` - Ground item examples
- `tileobject/TileObjectApiExample.java` - Tile object examples
