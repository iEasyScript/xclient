# Cannon Class Documentation

> Also known as `Rs2Cannon`, the older name, which still works but is deprecated. Scripts use the short `projectx.bot` names since plugin API 2.7.0. Guide: [SCRIPTING.md](../SCRIPTING.md), map: [api-migration.tsv](../api-migration.tsv).


## [Back](../development.md)

## Overview
The `Cannon` class provides utility methods for interacting with the Dwarf Multicannon, including refilling and repairing it.

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
