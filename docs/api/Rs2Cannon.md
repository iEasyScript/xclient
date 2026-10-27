# Rs2Cannon Class Documentation

> **Old name.** Since plugin API 2.7.0 scripts use the short `projectx.bot` names; this `Rs2Cannon` page documents the older, still working, deprecated name. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).

## [Back](../development.md)

## Overview
The `Rs2Cannon` class provides utility methods for interacting with the Dwarf Multicannon, including refilling and repairing it.

## Methods

### `refill`
- **Signature**: `public static boolean refill()`
- **Description**: Refills the cannon with a random amount of cannonballs (between 10 and 15) if the current ammo count is low.

### `refill`
- **Signature**: `public static boolean refill(int cannonRefillAmount)`
- **Description**: Refills the cannon if the current ammo count is less than or equal to the specified `cannonRefillAmount`. Checks for cannonballs in inventory and interacts with the cannon.

### `repair`
- **Signature**: `public static boolean repair()`
- **Description**: Repairs a broken multicannon. Finds the broken cannon object and interacts with it using the "Repair" option.
