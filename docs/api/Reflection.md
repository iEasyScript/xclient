# Reflection Class Documentation

> Also known as `Rs2Reflection`, the older name, which still works but is deprecated. Scripts use the short `projectx.bot` names since plugin API 2.7.0. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).


## [Back](../development.md)
The `Reflection` class provides utility methods for manipulating game elements through Java reflection. This allows for operations that are normally inaccessible through standard API calls, such as obtaining and modifying game entity attributes and invoking hidden actions within the game's client.

## Methods

### `getGroundItemActions(ItemComposition item)`
- **Parameters**: `ItemComposition item` - The item composition to analyze.
- **Returns**: `String[]` - An array of possible actions for the item on the ground.
- **Description**: Extracts ground item actions from an item's composition using reflection to access the relevant field.

### `setItemId(MenuEntry menuEntry, int itemId)`
- **Parameters**:
    - `MenuEntry menuEntry` - The menu entry to modify.
    - `int itemId` - The item ID to set.
- **Description**: Sets the item ID in a menu entry, presumably to modify the context of an interaction dynamically.

### `getObjectByName(String[] names, boolean exact)`
- **Parameters**:
    - `String[] names` - Array of object names to find.
    - `boolean exact` - Specifies whether to match names exactly.
- **Returns**: `ArrayList<Integer>` - A list of object IDs matching the names.
- **Description**: Finds object IDs from the game's object definitions using their names, with an option for exact or partial matches.

### `invokeMenu(int param0, int param1, int opcode, int identifier, int itemId, String option, String target, int x, int y)`
- **Parameters**: Parameters defining the context and details of the menu action to invoke.
- **Description**: Directly invokes a game menu action using reflection, typically used for simulating clicks and other interactions programmatically.

## Usage Example

This example reads the ground-item actions of an item definition:

```java
net.runelite.api.ItemComposition def = Items.getItemComposition(995);
String[] actions = Reflection.getGroundItemActions(def);
System.out.println("Ground actions: " + String.join(", ", actions));
```
