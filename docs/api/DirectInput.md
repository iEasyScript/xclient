# Direct input
## [Back](../development.md)

## Overview

Every click the helpers (`Inventory`, `Bank`, `Objects`, ...) make normally goes through the virtual mouse: the cursor moves
to the target and clicks it. **Direct input** sends the same action straight to the game
instead, with no mouse movement and no click events.

Use it when an action must not wait for, or fight over, the mouse:

- **Prayer flicking.** The prayer clicks happen while the mouse is busy attacking or looting,
  and a 1-tick flick's two clicks reach the game on the same tick.
- **Fast, repetitive actions** where every mouse glide costs time, such as Tithe Farm.

Available from client **1.0.46** (plugin API **2.6.28**). The `withDirectInput` helpers and the
prayer flick methods need client **1.0.47** (plugin API **2.6.29**). Set `minClientVersion` to
match, or catch `NoSuchMethodError` and fall back to the mouse.

> **Detection.** The game sees an action with no mouse movement behind it, which a player using
> a mouse never produces. That is easier to detect than a mouse click. Offer it as a setting so
> the player chooses it, and use it for the actions that need it rather than for everything.

## Methods (`ProjectX`)

### `withDirectInput(Runnable action)`
- **Description**: Runs `action` with direct input on for the calling thread, then restores the
  previous setting, even if the action throws. The recommended way to use it.

### `withDirectInput(Supplier<T> action)`
- **Returns**: whatever `action` returns.
- **Description**: As above, for actions that return a result.

### `setDirectInput(boolean on)`
- **Description**: Turns direct input on or off for the calling thread. If you use this
  directly, clear it in a `finally` block: script threads are shared with other scripts.

### `isDirectInput()`
- **Returns**: `boolean`, whether direct input is on for the calling thread.

## How it behaves

- **Per thread.** Only clicks made on the thread that turned it on are affected. Other scripts
  running at the same time keep using the mouse.
- **Waits for the game to take the action.** An item selected with "Use" is selected before your
  next line runs, so "use X on Y" works as two calls in a row.
- **Some clicks still use the mouse.** Minimap walking (its coordinates are screen positions) and
  anything in another world view, such as on a boat.
- **From the client thread** (an `@Subscribe` handler), actions are sent immediately and nothing
  sleeps. That is what makes same-tick prayer flicking possible.

## Usage examples

### A single action
```java
ProjectX.withDirectInput(() -> Inventory.interact("Bones", "Bury"));
```

### Use an item on an object
```java
int PATCH_ID = 8550;
ProjectX.withDirectInput(() -> {
    Inventory.interact("Watering can(8)", "Use");
    Objects.query().withId(PATCH_ID).nearest().interact();
});
```

### Only when the player turned it on
```java
Runnable doClicks = () -> Inventory.interact("Bones", "Bury");
boolean directInputEnabled = true;
if (directInputEnabled) {
    ProjectX.withDirectInput(doClicks);
} else {
    doClicks.run();
}
```

### 1-tick prayer flicking
See [Prayers: flicking](Prayers.md#1-tick-flicking).
```java
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;

public class Flicker {
    @Subscribe
    public void onGameTick(GameTick tick) {
        if (Players.local().isInteracting()) {
            ProjectX.withDirectInput(Prayers::flickQuickPrayer);
        }
    }
}
```
