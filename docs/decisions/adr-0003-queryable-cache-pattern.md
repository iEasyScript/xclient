# ADR 0003: Queryable Cache Pattern for Game Entities

- Status: Accepted (2026-02-08)

## Context
Scripts frequently need fast, thread-safe access to NPCs, players, tile objects/items, and boat state. Direct RuneLite API traversal on script threads is error-prone and can violate client-thread rules.

## Decision
Expose entity data through Guice-managed caches (`Rs2NpcCache`, `Rs2PlayerCache`, `Rs2TileObjectCache`, `Rs2TileItemCache`, `Rs2BoatCache`, `Rs2PlayerStateCache`) accessed only via `ProjectX.getRs2XxxCache().query()` or `.getStream()`. Caches refresh per game tick and support world-view queries for boat contexts.

## Consequences
- Scripts must not instantiate caches/queryables directly; tests and new utilities should follow the same pattern.
- Cache access stays tick-consistent and client-thread safe via `ClientThread.runOnClientThreadOptional` wrappers where needed.
- Performance remains predictable by limiting repeated client traversal and encouraging query filters (e.g., `within`, `fromWorldView`).
