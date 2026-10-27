# Objects Class Documentation

> Also known as `Rs2GameObject`, the older name, which still works but is deprecated. Scripts use the short `projectx.bot` names since plugin API 2.7.0. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).


## [Back](../development.md)


## Finding things: `Objects.query()`

To find an entity, start with `Objects.query()` rather than the static helpers below; it is the supported way to search and returns a typed model you can click.

```java
SceneObject tree = Objects.query("Tree").nearestReachable();
if (tree != null) {
    tree.interact("Chop down");
}

// or search and click in one go
Objects.query("Tree").interact("Chop down");
```

All filters and terminals are described in the [Queryable API guide](../../runelite-client/src/main/java/net/runelite/client/plugins/projectx/api/QUERYABLE_API.md). The older static helpers documented below still work.

## Overview
The `Objects` class provides methods to interact with game objects within the game environment. It offers functionalities to locate, interact, and manipulate game objects based on various criteria like ID, name, or location.

## Methods

### `clickObject`
- **Signature**: `private static boolean clickObject(TileObject object, String action)`
- **Description**: Handles the clicking of a game object, applying a specified action. This method includes detailed logic for handling the action based on the object's properties and current game state.

### `convertGameObjectToObjectComposition`
- **Signature**: `public static ObjectComposition convertGameObjectToObjectComposition(TileObject tileObject)`
- **Description**: Converts a `TileObject` to an `ObjectComposition` by fetching its definition from the client, considering proximity restrictions.

### `exists`
- **Signature**: `public static boolean exists(int id)`
- **Description**: Checks if a game object with the specified ID exists in the visible area.

### `findBank`
- **Signature**: `public static GameObject findBank()`
- **Description**: Searches for a game object that acts as a bank within the visible area.

### `findChest`
- **Signature**: `public static GameObject findChest()`
- **Description**: Searches for a game object that acts as a chest within the visible area.

### `findDoor`
- **Signature**: `public static WallObject findDoor(int id)`
- **Description**: Finds a door object by ID within the visible tiles of the current scene.

### `findGameObjectByLocation`
- **Signature**: `public static TileObject findGameObjectByLocation(WorldPoint worldPoint)`
- **Description**: Finds a game object at a specific world point.

### `findObject`
- **Signature**: `@Deprecated public static ObjectComposition findObject(int id)`
- **Description**: Fetches the object composition for a given ID. This method is deprecated and will be removed in favor of other object fetching methods.

### `findObjectById`
- **Signature**: `public static TileObject findObjectById(int id)`
- **Description**: Locates a game object based on its ID from various object types like ground objects, wall objects, etc.

### `findObjectByIdAndDistance`
- **Signature**: `public static TileObject findObjectByIdAndDistance(int id, int distance)`
- **Description**: Finds a game object by ID that is within a specified distance.

### `findObjectByLocation`
- **Signature**: `public static TileObject findObjectByLocation(WorldPoint worldPoint)`
- **Description**: Finds a game object based on its world location.

### `get`
- **Signature**: `public static GameObject get(String name, boolean exact)`
- **Description**: Retrieves a game object by name, with an option for exact matching.

### `getGameObject`
- **Signature**: `public static GameObject getGameObject(LocalPoint localPoint)`
- **Description**: Retrieves a game object at a given local point.

### `getGameObjects`
- **Signature**: `public static List<GameObject> getGameObjects()`
- **Description**: Retrieves a list of all game objects within the game's visible area.

### `getGroundObjects`
- **Signature**: `public static List<GroundObject> getGroundObjects()`
- **Description**: Retrieves a list of all ground objects within a specified distance from the player.

### `getTiles`
- **Signature**: `public static List<Tile> getTiles()`
- **Description**: Retrieves all tiles within the game's visible area up to a certain distance.

### `getWallObjects`
- **Signature**: `public static List<WallObject> getWallObjects()`
- **Description**: Retrieves a list of all wall objects within a specified distance from the player.

### `hasLineOfSight`
- **Signature**: `public static boolean hasLineOfSight(TileObject tileObject)`
- **Description**: Checks if there is a line of sight to the specified tile object from the player's current location.

### `interact`
- **Signature**: `public static boolean interact(int id, String action, int distance)`
- **Description**: Interacts with a game object by ID, performing a specified action, only if it is within a certain distance.

## Additional Details
This class is crucial for scripts that need to dynamically interact with game objects based on various conditions and parameters, enhancing the automation capabilities within the game environment.
