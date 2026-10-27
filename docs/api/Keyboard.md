# Keyboard Class Documentation

> Also known as `Rs2Keyboard`, the older name, which still works but is deprecated. Scripts use the short `projectx.bot` names since plugin API 2.7.0. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).


## [Back](../development.md)
## Overview
The `Keyboard` class simulates keyboard actions such as typing strings, pressing, holding, and releasing keys. It directly interacts with the game's canvas to dispatch keyboard events.

## Methods

### `getCanvas`
- **Description**: Retrieves the game canvas from the ProjectX client.
- **Returns**: `Canvas` - The game's canvas used for dispatching keyboard events.

### `typeString`
- **Description**: Types a string by dispatching a sequence of `KEY_TYPED` events for each character.
- **Parameters**:
    - `word`: `String` - The string to type.
- **Behavior**: Simulates typing with delays between key presses.

### `keyPress`
- **Description**: Presses a single character key.
- **Parameters**:
    - `key`: `char` - The character key to press.
- **Behavior**: Dispatches a `KEY_TYPED` event for the character.

### `holdShift`
- **Description**: Holds the shift key down.
- **Behavior**: Dispatches a `KEY_PRESSED` event for the shift key.

### `releaseShift`
- **Description**: Releases the shift key.
- **Behavior**: Dispatches a `KEY_RELEASED` event for the shift key.

### `keyHold`
- **Description**: Holds down a specified key.
- **Parameters**:
    - `key`: `int` - The key code (from `KeyEvent`) to hold.
- **Behavior**: Dispatches a `KEY_PRESSED` event for the specified key.

### `keyRelease`
- **Description**: Releases a specified key.
- **Parameters**:
    - `key`: `int` - The key code (from `KeyEvent`) to release.
- **Behavior**: Dispatches a `KEY_RELEASED` event for the specified key.

### `keyPress`
- **Description**: Simulates a complete key press (press and release).
- **Parameters**:
    - `key`: `int` - The key code to press.
- **Behavior**: Calls `keyHold` and `keyRelease` successively.

### `enter`
- **Description**: Simulates pressing the Enter key.
- **Behavior**: Calls `keyHold` and `keyRelease` for `KeyEvent.VK_ENTER`.

## Static Members

### `pressedKeys`
- **Type**: `Map<Integer, Boolean>`
- **Description**: Keeps track of the pressed state of keys.

## Usage Example
```java
import java.awt.event.KeyEvent;

Keyboard.typeString("Hello, world!");
Keyboard.keyPress('a');
Keyboard.holdShift();
Keyboard.releaseShift();
Keyboard.enter();
Keyboard.keyPress(KeyEvent.VK_ESCAPE);
```
