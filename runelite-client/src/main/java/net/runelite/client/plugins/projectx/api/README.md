# ProjectX Queryable API

This package owns the cache and query layer for live game entities. It is how scripts find NPCs, players, ground items, scene objects, boats and player state.

Scripts reach it through the short entry points in `net.runelite.client.plugins.projectx.bot` (plugin API 2.7.0+): `Npcs.query()`, `Objects.query()`, `GroundItems.query()` and `Players.query()`, which return `Npc`, `SceneObject`, `FloorItem` and `PlayerModel`. Guide: [docs/SCRIPTING.md](../../../../../../../../../../docs/SCRIPTING.md). Do not instantiate caches or queryables directly.

## Quick start

```java
import net.runelite.client.plugins.projectx.bot.*;

Npc banker = Npcs.query("Banker").nearestOnClientThread();

if (banker != null) {
    banker.interact("Bank");
}
```

`Npcs.query("Banker")` is short for `Npcs.query().nameEquals("Banker")`. Every terminal can be called from a script thread; the `*OnClientThread` terminals (`nearestOnClientThread()`, `firstOnClientThread()`, `toListOnClientThread()`) do a whole name search in one client-thread hop, so prefer them for name searches that run every loop.

| Start with | Returns | Interact with |
|---|---|---|
| `Npcs.query()` | `Npc` | `npc.interact("Attack")` |
| `Objects.query()` | `SceneObject` | `rock.interact("Mine")` |
| `GroundItems.query()` | `FloorItem` | `item.pickup()` or `item.interact("Take")` |
| `Players.query()` | `PlayerModel` | `Players.local()` is you |

## References

- Full guide: [QUERYABLE_API.md](QUERYABLE_API.md)
- Script and threading rules: [../AGENTS.md](../AGENTS.md)
- Entity utility gotchas: [../../../../../../../../../../docs/entity-guides/README.md](../../../../../../../../../../docs/entity-guides/README.md)
- Old name to new name map: [../../../../../../../../../../docs/api-migration.tsv](../../../../../../../../../../docs/api-migration.tsv)

## Directory structure

```
api/
├── README.md
├── QUERYABLE_API.md
├── AbstractEntityQueryable.java   shared filters and terminals
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

The per-type folders hold the caches and models that the `bot` entry points wrap (their classes still carry the older `Rs2*` names, which client internals use).

## Examples

The `*ApiExample.java` files in each subdirectory are complete examples:

- `npc/NpcApiExample.java` - NPC queries
- `player/PlayerApiExample.java` - player queries
- `tileitem/TileItemApiExample.java` - ground items
- `tileobject/TileObjectApiExample.java` - scene objects

## Old names

`ProjectX.getRs2NpcCache().query()` is `Npcs.query()`, `Rs2NpcModel` is `Npc`, `Rs2TileObjectModel` is `SceneObject`, `Rs2TileItemModel` is `FloorItem`, `Rs2PlayerModel` is `PlayerModel`. The full table is at the end of [QUERYABLE_API.md](QUERYABLE_API.md#old-names).
