# Scripting guide

Scripts talk to the game through one package with short, plain names:

```java
import net.runelite.client.plugins.projectx.bot.*;

SceneObject rock = Objects.query("Copper rocks").nearestReachable();
if (rock != null) {
    rock.interact("Mine");
}

Npcs.query("Cow").interact("Attack");

if (Inventory.isFull()) {
    Bank.depositAll();
}
```

Available from client plugin API **2.7.0**. The older `Rs2*` names still work (see
[Old names](#old-names)).

## Your first script: a copper miner

A script is two classes: a **plugin** (shows up in the plugin list, starts and stops the script)
and a **script** (the loop, which runs off the game's thread).

```java
package net.runelite.client.plugins.projectx.copperminer;

import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@PluginDescriptor(
        name = PluginDescriptor.Default + "Copper Miner",
        description = "Mines copper and drops the ore",
        tags = {"mining", "copper"},
        enabledByDefault = false
)
public class CopperMinerPlugin extends Plugin {
    @Inject
    private CopperMinerScript script;

    @Override
    protected void startUp() {
        script.run();
    }

    @Override
    protected void shutDown() {
        script.shutdown();
    }
}
```

```java
package net.runelite.client.plugins.projectx.copperminer;

import net.runelite.client.plugins.projectx.ProjectX;
import net.runelite.client.plugins.projectx.Script;
import net.runelite.client.plugins.projectx.bot.*;

import java.util.concurrent.TimeUnit;

public class CopperMinerScript extends Script {

    public boolean run() {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!ProjectX.isLoggedIn() || !super.run()) return;

                if (Inventory.isFull()) {
                    Inventory.dropAll("Copper ore");
                    return;
                }

                if (Player.isAnimating()) return;     // still mining

                SceneObject rock = Objects.query("Copper rocks").nearestReachable();
                if (rock == null) {
                    ProjectX.status = "No copper rock nearby";
                    return;
                }

                if (rock.interact("Mine")) {
                    sleepUntil(Player::isAnimating, 3000);
                }
            } catch (Exception e) {
                ProjectX.log("Copper miner: " + e.getMessage());
            }
        }, 0, 600, TimeUnit.MILLISECONDS);
        return true;
    }
}
```

Rules worth knowing from day one:

- **Wait with `sleepUntil(condition, timeoutMs)`**, not a fixed `sleep`, when you are waiting for
  the game to change.
- **Check for `null`.** A search finds nothing when nothing matches; `nearest()` then returns null.
  `Npcs.query("Cow").interact("Attack")` does the search and the click in one go and simply returns
  false when there is no cow.
- **Don't keep entities between loops.** Search again each time; the scene changes every tick.
- Object names are the in-game names: the rock is "Copper rocks", not "Copper rock".

More detail on threading, configs and overlays: `runelite-client/.../projectx/AGENTS.md`.

## Entry points

All in `net.runelite.client.plugins.projectx.bot`.

| Class | What it is for |
|---|---|
| `Objects` | Rocks, trees, doors, banks, ladders: `Objects.query(...)` |
| `Npcs` | NPCs: `Npcs.query(...)` |
| `GroundItems` | Items on the ground: `GroundItems.query(...)` |
| `Players` | Other players (and you): `Players.query(...)`, `Players.local()` |
| `Inventory` | The items you carry |
| `Bank`, `DepositBox` | Banking |
| `Equipment` | What you wear |
| `Player` | Your own character: health, run energy, animation, eating |
| `Walker`, `PathApi`, `Minimap` | Walking and route finding |
| `Widgets`, `Dialogue`, `Tabs` | Interfaces, chat dialogues, side-panel tabs |
| `Prayers` | Prayers, plus the prayer constants (`Prayers.PIETY`) |
| `Magic`, `Spells`, `CombatSpells`, `Spellbooks`, `Staffs`, `Tomes`, `Thralls` | Magic |
| `Combat`, `Pvp`, `Slayer`, `Cannon`, `NpcData` | Combat |
| `GrandExchange`, `Shop`, `Items` | Trading and item lookups (ids, prices) |
| `Keyboard`, `Camera` | Input and camera |
| `Rand` | Random numbers and human-like delays (`Rand.between(600, 900)`) |
| `Antiban`, `AntibanSettings` | The built-in antiban behaviour |
| `Loadout` | Load a saved inventory setup: `new Loadout("Mining", mainScheduledFuture)` |
| `Foods`, `Potions`, `EnsouledHeads` | Item data |
| `GemBag`, `LogBasket`, `RunePouch`, `ExplorersRing` | Containers and special items |
| `Tiles`, `LocalPoints`, `Worlds`, `Boats`, `BoatNavigator` | Tiles, coordinates, worlds, sailing |
| `UiHelper`, `TextSanitizer`, `GameSettings`, `Discord`, `LootEngine`, `BankIds`, `Fletching`, `Reflection` | Smaller helpers |

Each helper has exactly the methods of its old `Rs2*` class (it *is* that class under a new name),
so your IDE's autocomplete on `Inventory.` shows everything `Rs2Inventory.` had.

## Searching: `query()`

Every search has three parts: where to look, filters, and a terminal that gives the answer.

```java
SceneObject tree = Objects.query("Oak tree").nearestReachable();         // by exact name
Npc banker       = Npcs.query().nameEquals("Banker").nearest();          // same, long form
FloorItem coins  = GroundItems.query().withId(ItemID.COINS).within(10).first();
List<Npc> goblins = Npcs.query().withNameContains("goblin").list();
int nearby       = Players.query().within(15).count();
boolean clicked  = Objects.query().withIds(ROCK_IDS).interact("Mine");  // search + click
```

**Filters** (chain as many as you like): `nameEquals(name)`, `withName(name)`, `withNames(a, b)`,
`withNameContains(text)`, `withId(id)`, `withIds(ids...)`, `within(tiles)`,
`within(point, tiles)`, `where(entity -> ...)`, `fromWorldView()`.
Names compare ignoring case.

**Terminals** (end the search): `nearest()`, `nearest(maxTiles)`, `nearestReachable()`,
`first()`, `firstReachable()`, `list()`, `count()`, `interact(action)`.

**Name searches off the game thread.** A name filter reads each entity's name from the game, one
hop per entity. If you search by name a lot, the `...OnClientThread()` terminals
(`nearestOnClientThread()`, `firstOnClientThread()`, `toListOnClientThread()`) do the whole search
in one hop and are much cheaper.

`Npcs.stream()`, `Objects.stream()`, `GroundItems.stream()` and `Players.stream()` give everything
in the scene this tick as a Java `Stream`, if you prefer streams.

## What a search returns

| Search | Returns | Useful methods |
|---|---|---|
| `Objects.query()` | `SceneObject` | `interact(action)`, `getId()`, `getName()`, `getWorldLocation()`, `isReachable()` |
| `Npcs.query()` | `Npc` | `interact(action)`, `getHealthPercentage()`, `isInteractingWithPlayer()`, `isMoving()` |
| `GroundItems.query()` | `FloorItem` | `pickup()`, `interact("Take")`, `getQuantity()`, `getTotalGeValue()`, `isOwned()` |
| `Players.query()` | `PlayerModel` | `getName()`, `getCombatLevel()`, `isFriend()`, `getWorldLocation()` |

`interact(action)` is the same as `click(action)`, which also still works. It returns `true` when
the click was sent.

Names were chosen so they don't collide with RuneLite's own types: `SceneObject` (not
`GameObject`), `Npc` (RuneLite has `NPC`), `FloorItem` (the ground items plugin has `GroundItem`),
`PlayerModel` (RuneLite has `Player`).

## Constants: prayers, spells, food

Java enums cannot be renamed without breaking scripts that are already installed, so their
constants are also offered under short names:

```java
Prayers.toggle(Prayers.PROTECT_MELEE, true);
Magic.cast(Spells.HIGH_LEVEL_ALCHEMY);
for (Rs2Food food : Foods.values()) { ... }
```

`Prayers.PIETY` *is* `Rs2PrayerEnum.PIETY`. A variable or parameter that holds one still has the
old enum type (`Rs2PrayerEnum`, `Rs2Spells`, `Rs2Food`, ...).

## Name clashes to know about

- **`Player`** is the helper for your character. RuneLite also has `net.runelite.api.Player`. If a
  file needs both, write RuneLite's one out in full: `net.runelite.api.Player p = ...`.
- **`Objects`** shares its name with `java.util.Objects`. Don't import `java.util.Objects`;
  `bot.Objects` has the same `equals`, `hash`, `requireNonNull`, `isNull`, `nonNull`, ... methods.
- With `import net.runelite.api.*;` or `import java.util.*;` in the same file, import `Player` or
  `Objects` by name (`import net.runelite.client.plugins.projectx.bot.Player;`) so Java knows which
  one you mean.

## Old names

Every old name keeps working, forever: scripts compiled against them run unchanged. They are
marked deprecated so new code picks the short names. To move code over, rename; the methods are
the same. The full machine-readable map (used by the migration codemod) is
[`api-migration.tsv`](api-migration.tsv).

Searches move from the cache getters to the entry points:

| Old | New |
|---|---|
| `ProjectX.getRs2TileObjectCache().query()` | `Objects.query()` |
| `ProjectX.getRs2NpcCache().query()` | `Npcs.query()` |
| `ProjectX.getRs2TileItemCache().query()` | `GroundItems.query()` |
| `ProjectX.getRs2PlayerCache().query()` | `Players.query()` |
| `ProjectX.getRs2XxxCache().getStream()` | `Objects.stream()`, `Npcs.stream()`, ... |
| `....query().withName("X")` | `....query("X")` |
| `ProjectX.getRs2ItemManager().getItemId(name)` | `Items.getItemId(name)` |
| `Rs2BoatCache.getLocalBoat()` | `Boats.local()` |
| `model.click(action)` | `model.interact(action)` |
| `query.toList()` | `query.list()` |

Classes:

| Old name | New name |
|---|---|
| `Rs2Inventory` | `Inventory` |
| `Rs2Player` | `Player` |
| `Rs2Bank` | `Bank` |
| `Rs2Walker` | `Walker` |
| `Rs2Widget` | `Widgets` |
| `Rs2Dialogue` | `Dialogue` |
| `Rs2Equipment` | `Equipment` |
| `Rs2Random` | `Rand` |
| `Rs2AntibanSettings` | `AntibanSettings` |
| `Rs2Antiban` | `Antiban` |
| `Rs2GrandExchange` | `GrandExchange` |
| `Rs2Keyboard` | `Keyboard` |
| `Rs2Magic` | `Magic` |
| `Rs2Combat` | `Combat` |
| `Rs2Camera` | `Camera` |
| `Rs2Tab` | `Tabs` |
| `Rs2Shop` | `Shop` |
| `Rs2DepositBox` | `DepositBox` |
| `Rs2Potion` | `Potions` |
| `Rs2Slayer` | `Slayer` |
| `Rs2UiHelper` | `UiHelper` |
| `Rs2Pvp` | `Pvp` |
| `Rs2NpcManager` | `NpcData` |
| `Rs2Gembag` | `GemBag` |
| `Rs2ExplorersRing` | `ExplorersRing` |
| `Rs2Discord` | `Discord` |
| `Rs2RunePouch` | `RunePouch` |
| `Rs2LootEngine` | `LootEngine` |
| `Rs2Settings` | `GameSettings` |
| `Rs2Cannon` | `Cannon` |
| `Rs2WorldUtil` | `Worlds` |
| `Rs2LogBasket` | `LogBasket` |
| `Rs2Fletching` | `Fletching` |
| `Rs2MiniMap` | `Minimap` |
| `Rs2Reflection` | `Reflection` |
| `Rs2PathApi` | `PathApi` |
| `Rs2LocalPoint` | `LocalPoints` |
| `Rs2ItemManager` | `Items` |
| `Rs2BankID` | `BankIds` |
| `Rs2TextSanitizer` | `TextSanitizer` |
| `Rs2BoatNavigator` | `BoatNavigator` |
| `Rs2GameObject` | `Objects` |
| `Rs2Npc` | `Npcs` |
| `Rs2GroundItem` | `GroundItems` |
| `Rs2InventorySetup` | `Loadout` |
| `Rs2Prayer` | `Prayers` |
| `Rs2Tile` | `Tiles` |
| `Rs2NpcModel` (`api.npc.models`) | `Npc` |
| `Rs2TileObjectModel` (`api.tileobject.models`) | `SceneObject` |
| `Rs2TileItemModel` | `FloorItem` |
| `Rs2PlayerModel` (`api.player.models`) | `PlayerModel` |
| `Rs2NpcQueryable` | `NpcQuery` |
| `Rs2TileObjectQueryable` | `ObjectQuery` |
| `Rs2TileItemQueryable` | `GroundItemQuery` |
| `Rs2PlayerQueryable` | `PlayerQuery` |
| `Rs2PrayerEnum.X` | `Prayers.X` (the type stays `Rs2PrayerEnum`) |
| `Rs2Spells.X` | `Spells.X` (the type stays `Rs2Spells`) |
| `Rs2CombatSpells.X` | `CombatSpells.X` (the type stays `Rs2CombatSpells`) |
| `Rs2Food.X` | `Foods.X` (the type stays `Rs2Food`) |
| `Rs2Spellbook.X` | `Spellbooks.X` (the type stays `Rs2Spellbook`) |
| `Rs2Staff.X` | `Staffs.X` (the type stays `Rs2Staff`) |
| `Rs2Thrall.X` | `Thralls.X` (the type stays `Rs2Thrall`) |
| `Rs2Tome.X` | `Tomes.X` (the type stays `Rs2Tome`) |
| `Rs2EnsouledHead.X` | `EnsouledHeads.X` (the type stays `Rs2EnsouledHead`) |

Still under their old names: `Rs2ItemModel` (the inventory/bank item; use `var` for locals),
the older `util.npc.Rs2NpcModel` / `util.player.Rs2PlayerModel` / `util.tileobject.Rs2TileObjectModel`
returned by the old static helpers, `Rs2WorldPoint`, `Rs2WorldArea`, `Rs2NpcStats` and
`Rs2BoatModel`.

### How the old names stay working

- A helper such as `Inventory` is declared `public final class Inventory extends Rs2Inventory`, so
  every static method is the old one; nothing is copied.
- `Npc`, `SceneObject`, `FloorItem` and `PlayerModel` extend the old models, and the caches now build
  the new types. A new model can be passed to any method that takes the old one.
- Old methods keep their exact signatures, so jars compiled against them link as before.
