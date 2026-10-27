# Queryable API

How scripts find things in the game world: NPCs, objects, ground items and players. It is the long
form of the "Searching" section of [docs/SCRIPTING.md](../../../../../../../../../../docs/SCRIPTING.md).
Available from client plugin API **2.7.0** (set `minClientVersion = "2.7.0"`).

Every example below compiles against the client. Imports are left out to keep them short (plus the usual
`java.util` ones such as `List`, `Comparator`, `Predicate` and `Collectors`); the ones that matter are:

```java
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.projectx.bot.*;
```

## Contents

1. [Getting started](#getting-started)
2. [What you can search](#what-you-can-search)
3. [Filters](#filters)
4. [Terminals](#terminals)
5. [What a search returns: the models](#what-a-search-returns-the-models)
6. [Threading](#threading)
7. [Common patterns](#common-patterns)
8. [Advanced](#advanced)
9. [Performance](#performance)
10. [Troubleshooting](#troubleshooting)
11. [Old names](#old-names)

---

## Getting started

A search has three parts: **where to look**, **filters** that narrow it down, and a **terminal** that
gives the answer.

```java
Npc banker = Npcs.query("Banker")                  // where to look: NPCs named "Banker"
        .where(npc -> !npc.isInteracting())        // filter: not busy with someone else
        .nearest();                                // terminal: the closest one, or null

if (banker != null) {
    banker.interact("Bank");
}
```

`Npcs.query("Banker")` is the short form of `Npcs.query().nameEquals("Banker")`. Names compare
ignoring case, and must otherwise match exactly.

When you only want to click the nearest match, skip the variable. `interact(action)` is a terminal
too; it returns `true` when a click was sent and `false` when nothing matched:

```java
boolean attacked = Npcs.query("Cow").interact("Attack");
boolean mined = Objects.query("Copper rocks").interact("Mine");
```

Two rules that avoid most bugs:

- **Check for `null`.** `nearest()`, `first()` and friends return `null` when nothing matches.
- **A query is single use.** It reads the scene once, through one terminal. To ask a second
  question, start a new `query()`. Do not keep the entities either; search again each loop, because
  the scene changes every tick.

## What you can search

| Start with | Query type | Returns | Notes |
|---|---|---|---|
| `Npcs.query()` | `NpcQuery` | `Npc` | Monsters, bankers, shopkeepers |
| `Objects.query()` | `ObjectQuery` | `SceneObject` | Rocks, trees, doors, bank booths, ladders, altars |
| `GroundItems.query()` | `GroundItemQuery` | `FloorItem` | Items lying on the ground |
| `Players.query()` | `PlayerQuery` | `PlayerModel` | Other players, and you |

Each also takes a name: `Npcs.query("Guard")`, `Objects.query("Oak tree")`,
`GroundItems.query("Bones")`, `Players.query("Zezima")`.

`Npcs.stream()`, `Objects.stream()`, `GroundItems.stream()` and `Players.stream()` return everything
in the scene this tick as a Java `Stream`, for when you would rather use streams. Your own
character is `Players.local()`.

```java
List<Npc> all = Npcs.stream().filter(n -> n.getCombatLevel() > 50).collect(Collectors.toList());
WorldPoint here = Players.local().getWorldLocation();
```

All four query types have exactly the same filters and terminals, listed next. Scenery from the
boat system (`Boats.local()`) is not a query; see [SCRIPTING.md](../../../../../../../../../../docs/SCRIPTING.md).

## Filters

Filters return the query, so you chain as many as you like. They are applied in the order you write
them, which matters for [performance](#performance).

| Filter | Keeps |
|---|---|
| `nameEquals(String name)` | entities whose name is `name`, ignoring case (`withName` is the same) |
| `withNames(String... names)` | entities named any of these |
| `withNameContains(String text)` | entities whose name contains `text`, ignoring case |
| `withId(int id)` | entities with this id |
| `withIds(int... ids)` | entities with any of these ids |
| `within(int tiles)` | entities within `tiles` of your character |
| `within(WorldPoint anchor, int tiles)` | entities within `tiles` of `anchor` |
| `where(Predicate<E> test)` | entities for which `test` is true (`E` is `Npc`, `SceneObject`, ...) |
| `fromWorldView()` | entities in the same world view as your character (matters on boats) |

A `null` name, an empty `withNames()` / `withIds()` or an empty `withNameContains("")` keeps
nothing, so the query comes back empty rather than matching everything.

```java
List<Npc> guards = Npcs.query()
        .withNames("Guard", "Knight of Ardougne")
        .within(10)
        .where(npc -> npc.getHealthRatio() != 0)
        .list();
```

## Terminals

A terminal ends the query and gives the answer.

| Terminal | Returns | Meaning |
|---|---|---|
| `first()` | model or `null` | first match in scene order |
| `firstReachable()` | model or `null` | first match your character can walk to |
| `nearest()` | model or `null` | closest match to your character |
| `nearest(int maxTiles)` | model or `null` | closest match no further than `maxTiles` |
| `nearest(WorldPoint anchor, int maxTiles)` | model or `null` | closest match to `anchor` |
| `nearestReachable()` | model or `null` | closest match you can walk to |
| `nearestReachable(int maxTiles)` | model or `null` | same, no further than `maxTiles` |
| `list()` | `List<E>` | every match (`toList()` is the same) |
| `count()` | `int` | how many match |
| `interact(String action)` | `boolean` | click the nearest reachable match with `action` |

The `interact` family, all returning `true` if a click was sent:

```java
Objects.query("Bank booth").interact();                   // left-click option
Objects.query("Bank booth").interact("Bank");             // pick this option
Objects.query("Oak tree").interact("Chop down", 12);      // only within 12 tiles
Objects.query().interact(1276, "Chop down");              // by object id
GroundItems.query().interact(995, "Take", 8);             // by item id, within 8 tiles
```

The full list is `interact()`, `interact(action)`, `interact(action, maxTiles)`, `interact(id)`,
`interact(id, action)` and `interact(id, action, maxTiles)`. Those with an `id` add a `withId(id)`
filter first.

`nearestReachable()` is usually what a skilling script wants: the nearest rock that is not behind a
wall or a fence. If reachability cannot be worked out (for example your character has no world view
yet) it falls back to the nearest match within 10 tiles instead of returning nothing.

**Inside an instance** (a boss room, a minigame), model locations are in the instance's own
coordinates. An anchor you pass to `within(anchor, n)` or `nearest(anchor, n)` is usually a normal
world tile, and the API maps it onto the instance for you, so a hard-coded tile still works there.

The `*OnClientThread` terminals (`firstOnClientThread()`, `nearestOnClientThread()` and its
`(maxTiles)` and `(anchor, maxTiles)` forms, `toListOnClientThread()`) give the same answers; see
[Threading](#threading) for when to prefer them.

## What a search returns: the models

The models are named so they do not collide with RuneLite's own types: `Npc` (RuneLite has `NPC`),
`SceneObject` (not `GameObject`), `FloorItem` (the ground items plugin has `GroundItem`) and
`PlayerModel` (RuneLite has `Player`).

All four have these:

| Method | Meaning |
|---|---|
| `interact(String action)` | click with this menu option; `true` if the click was sent (`click(action)` is the same) |
| `interact()` | click the left-click option |
| `getId()` | object id, NPC id, item id or player index |
| `getName()` | name, or `null` |
| `getWorldLocation()` | `WorldPoint` |
| `getLocalLocation()` | `LocalPoint` |
| `isReachable()` | can your character walk there |

Beyond that, each model has its own:

**`Npc`** (also everything a RuneLite actor has, such as `getAnimation()`, `getInteracting()`,
`isDead()`, `getOverheadText()`):

```java
Npc npc = Npcs.query("Goblin").nearest();
if (npc != null) {
    int level = npc.getCombatLevel();
    double hp = npc.getHealthPercentage();          // 0 to 100
    boolean busy = npc.isInteracting();             // fighting anyone
    boolean fightingMe = npc.isInteractingWithPlayer();
    boolean walking = npc.isMoving();
    int tiles = npc.getDistanceFromPlayer();
    boolean near = npc.isWithinDistance(Players.local().getWorldLocation(), 5);
    boolean visible = npc.hasLineOfSight();
}
```

**`SceneObject`:**

```java
SceneObject door = Objects.query("Large door").nearest();
if (door != null) {
    int id = door.getId();
    net.runelite.api.ObjectComposition def = door.getObjectComposition();   // actions, impostors
    boolean hasOpen = door.isOpShown(0);                                     // is menu option 1 shown
}
```

**`FloorItem`:**

```java
FloorItem loot = GroundItems.query("Dragon bones").nearest();
if (loot != null) {
    int qty = loot.getQuantity();
    int geValue = loot.getTotalGeValue();       // quantity times Grand Exchange price
    boolean mine = loot.isOwned();              // visible only to you
    boolean tradeable = loot.isTradeable();
    boolean noted = loot.isNoted();
    boolean gone = loot.willDespawnWithin(10);  // ticks, not seconds
    loot.pickup();                              // same as interact("Take")
}
```

Also on `FloorItem`: `isStackable()`, `isMembers()`, `isLootAble()`, `isDespawned()`,
`isProfitableToHighAlch()`, `getTotalValue()`, `getVisibleTime()`, `getDespawnTime()`.

**`PlayerModel`** (and `Players.local()` is one too):

```java
for (PlayerModel other : Players.query().within(15).list()) {
    String name = other.getName();
    int combat = other.getCombatLevel();
    boolean friend = other.isFriend();
    boolean clan = other.isClanMember();
    int skull = other.getSkullIcon();
}
```

## Threading

Game state lives on RuneLite's client thread; your script loop runs on a separate thread. The query
API hides the hop, but it helps to know what it costs.

**Every terminal is safe to call from a script thread.** That is where scripts call them, and
`interact(...)` in particular must be called from the script thread, never from the client thread,
because the click may wait for the mouse.

What differs is how many hops a search makes:

- With the plain terminals (`nearest()`, `first()`, `list()`, `count()`, ...) the filters run on
  *your* thread, and each time one needs live data it hops to the client thread and waits. A name
  is live data (`getName()` is a hop per entity); an id, a location and a `within` check mostly
  are not.
- The `*OnClientThread` terminals do the whole search in a single hop: filters, sorting and all.
  With a name filter over a busy scene that is much cheaper, and every entity is read in the same
  game tick.
- `interact(...)` already works this way: it picks the target in one client-thread read, then clicks
  on your thread.

```java
// One hop for the whole search, then a click on the script thread.
Npc banker = Npcs.query("Banker").nearestOnClientThread();
if (banker != null) {
    banker.interact("Bank");
}

List<Npc> goblins = Npcs.query("Goblin").within(12).toListOnClientThread();
```

Rules for what goes inside `where(...)`:

- Keep it quick and side-effect free: no clicks, no `sleep`, no waiting. With an `*OnClientThread`
  terminal it runs on the client thread, where blocking freezes the game.
- Do not call `interact(...)`, `sleepUntil(...)` or anything else that clicks or waits from inside it.
- Only the terminals listed above have an `OnClientThread` form. `nearestReachable()`, `count()` and
  `firstReachable()` use the plain form.

If the client thread does not answer for 10 seconds (the game is stalled) a hop throws a
`RuntimeException`. Script loops should catch exceptions per iteration, as in the example in
[Common patterns](#a-whole-script-loop).

## Common patterns

### Nearest NPC that is not busy

```java
Npc cow = Npcs.query("Cow")
        .where(npc -> !npc.isInteracting())
        .nearestReachable();

if (cow != null) {
    cow.interact("Attack");
}
```

### Valuable loot

```java
FloorItem loot = GroundItems.query()
        .where(item -> item.getTotalGeValue() >= 5000)
        .where(FloorItem::isLootAble)
        .nearest(10);

if (loot != null) {
    loot.pickup();
}
```

### Every match

```java
List<Npc> guards = Npcs.query("Guard")
        .where(npc -> !npc.isInteracting())
        .within(15)
        .list();

for (Npc guard : guards) {
    ProjectX.log("Guard at " + guard.getWorldLocation());
}
```

### Any of several names

```java
Npc banker = Npcs.query()
        .withNames("Banker", "Bank clerk", "Bank assistant")
        .nearestReachable();
```

### Several filters

```java
Npc target = Npcs.query("Goblin")
        .within(10)
        .where(npc -> !npc.isInteracting())
        .where(npc -> npc.getHealthRatio() != 0)
        .where(npc -> npc.getAnimation() == -1)
        .nearest();
```

### Partial name

```java
SceneObject tree = Objects.query().withNameContains("tree").nearestReachable();
```

### Items about to disappear

```java
List<FloorItem> vanishing = GroundItems.query()
        .where(item -> item.willDespawnWithin(30))   // 30 ticks
        .where(item -> item.getTotalValue() > 1000)
        .list();
```

### Friends nearby

```java
List<PlayerModel> friends = Players.query()
        .where(PlayerModel::isFriend)
        .within(20)
        .list();
```

### By id

```java
SceneObject altar = Objects.query().withId(409).nearest();
```

### Wait for something to appear

```java
Npc banker = sleepUntilNotNull(() -> Npcs.query("Banker").nearest(), 5000, 600);
```

### Click, then wait for the result

```java
if (Npcs.query("Banker").interact("Bank")) {
    sleepUntil(Bank::isOpen, 5000);
}
```

### A whole script loop

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
                if (Player.isAnimating()) return;

                SceneObject rock = Objects.query("Copper rocks").nearestReachable();
                if (rock != null && rock.interact("Mine")) {
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

## Advanced

### Your own predicates

`where` takes any `Predicate`, so put reusable rules in a method or a field:

```java
Predicate<Npc> idle = npc -> !npc.isInteracting() && npc.getAnimation() == -1;
Predicate<Npc> weak = npc -> npc.getCombatLevel() <= 10;

Npc target = Npcs.query("Guard").where(idle).where(weak.negate()).nearest();
```

### Search around a point

`within(anchor, n)` and `nearest(anchor, n)` measure from the anchor instead of from you, which is
how you stay inside an area while fighting or gathering:

```java
WorldPoint camp = new WorldPoint(3222, 3218, 0);

Npc cow = Npcs.query("Cow").within(camp, 8).nearestReachable();
Npc closestToCamp = Npcs.query("Cow").nearest(camp, 8);
```

### Sorting and limiting

A query has no `sorted()` or `limit()`. Take the list, or use `stream()`:

```java
List<Npc> closestThree = Npcs.query("Guard").list().stream()
        .sorted(Comparator.comparingInt(Npc::getDistanceFromPlayer))
        .limit(3)
        .collect(Collectors.toList());

long lowHealth = Npcs.stream()
        .filter(npc -> "Goblin".equalsIgnoreCase(npc.getName()))
        .filter(npc -> npc.getHealthRatio() > 0 && npc.getHealthRatio() < 10)
        .count();
```

Prefer the query filters when you can; they are the ones the API optimises, and `stream()` hands you
every entity in the scene.

### Counting and branching

```java
int guards = Npcs.query("Guard").within(10).count();
boolean crowded = Players.query().within(7).count() > 3;
boolean nobodyAround = GroundItems.query("Bones").count() == 0;
```

### Null safety

```java
Npc npc = Npcs.query("Banker").nearest();
if (npc == null) {
    ProjectX.status = "No banker in sight";
    return;
}
npc.interact("Bank");
```

## Performance

- **Filter cheap things first.** Ids and distances are cheap; names and `where` predicates that read
  game data are not. `Objects.query().withId(1276).within(10).where(...)` lets the `where` see few
  entities. A search by `withId` / `withIds` beats one by name whenever you know the id.
- **Narrow the area.** `within(15)` or `nearest(15)` beats a scene-wide search.
- **Use the `*OnClientThread` terminals for name searches** that run every loop; see
  [Threading](#threading).
- **Search once per loop**, then use the result. Do not repeat the same query for each decision:

```java
Npc cow = Npcs.query("Cow").nearestReachable();    // once
if (cow != null && !cow.isInteracting()) {
    cow.interact("Attack");
}
```

- **Keep predicates simple.** A `where` runs for every entity that reaches it, so no loops over the
  whole scene inside it.
- **Do not hold entities between loops.** They are snapshots; an NPC that died or an object that
  depleted will not click.

## Troubleshooting

**The query returns `null` though it is on screen.**

- Is the name exactly the in-game name? The rock is `"Copper rocks"`, not `"Copper rock"`. Check with
  `withNameContains("copper")` to see what it is called.
- Widen the radius, or drop `within`.
- Drop filters one by one. If the search works without your `where`, the predicate is too strict.
- With `nearestReachable()`, the target may really be unreachable (behind a closed door). Try
  `nearest()` to tell the two apart.

```java
Objects.query("Copper rocks").nearest();            // found?
Objects.query("Copper rocks").nearestReachable();   // found, and walkable?
```

- The thing may not have spawned yet. Wait for it:

```java
SceneObject rock = sleepUntilNotNull(() -> Objects.query("Copper rocks").nearestReachable(), 5000);
```

**`IllegalStateException: stream has already been operated upon or closed`.** You used a query twice.
Build a new `query()` for each question.

**`interact` returns `false`.** Nothing matched, or nothing reachable matched. For an entity you hold
already, `model.interact(action)` returns `false` if the action is not on its menu: check the exact
spelling (`"Pick-up"` versus `"Take"`; `FloorItem.pickup()` handles the ground-item case).

**The click goes through but nothing happens.** Wait for the result before the next click:

```java
SceneObject rock = Objects.query("Copper rocks").nearestReachable();
if (rock != null && rock.interact("Mine")) {
    sleepUntil(Player::isAnimating, 3000);
}
```

**A hop times out.** The game thread was busy for 10 seconds, usually because something in a `where`
predicate blocks. Remove waits and clicks from predicates.

## Old names

Everything above has an older name that still works and is marked deprecated; scripts built against
them run unchanged. The machine-readable map is [docs/api-migration.tsv](../../../../../../../../../../docs/api-migration.tsv).

| Old | New |
|---|---|
| `ProjectX.getRs2NpcCache().query()` | `Npcs.query()` |
| `ProjectX.getRs2TileObjectCache().query()` | `Objects.query()` |
| `ProjectX.getRs2TileItemCache().query()` | `GroundItems.query()` |
| `ProjectX.getRs2PlayerCache().query()` | `Players.query()` |
| `ProjectX.getRs2XxxCache().getStream()` | `Npcs.stream()`, `Objects.stream()`, `GroundItems.stream()`, `Players.stream()` |
| `new Rs2PlayerModel()` (you) | `Players.local()` |
| `.query().withName("X")` | `.query("X")` or `.query().nameEquals("X")` |
| `.toList()` | `.list()` |
| `model.click(action)` | `model.interact(action)` |
| `Rs2NpcModel` (`api.npc.models`) | `Npc` |
| `Rs2TileObjectModel` (`api.tileobject.models`) | `SceneObject` |
| `Rs2TileItemModel` | `FloorItem` |
| `Rs2PlayerModel` (`api.player.models`) | `PlayerModel` |
| `Rs2NpcQueryable` | `NpcQuery` |
| `Rs2TileObjectQueryable` | `ObjectQuery` |
| `Rs2TileItemQueryable` | `GroundItemQuery` |
| `Rs2PlayerQueryable` | `PlayerQuery` |

The old model types are the parents of the new ones, so a `Npc` can be passed to anything that takes
an `Rs2NpcModel`. Client internals still reach the caches through `ProjectX.getRs2XxxCache()`; scripts
should not.
