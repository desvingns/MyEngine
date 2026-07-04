# MyEngine Content Properties Schema

Status: Phase 06 accepted  
Last updated: 2026-07-02

Content packs are external `.properties` files for the v0.1 foundation. The parser is intentionally
small and JVM-only. A future ADR can move to JSON/YAML if content complexity warrants it.

## Manifest

`manifest.properties`

- `id`
- `version`
- `schemaVersion`
- `engineMin`
- `engineMax`
- `locales`
- `dependencies`

## Definitions

Definition files use `<id>.<field>=<value>`.

### Tiles

- `buildable`: boolean
- `blocksMovement`: boolean
- `isCore`: optional boolean

### Resources

- `displayKey`: localization key

### Towers

- `range`: positive int
- `damage`: positive int
- `cooldownTicks`: positive int
- `costResource`: resource id
- `costAmount`: non-negative int

### Enemies

- `health`: positive int
- `speedTilesPerTick`: positive int
- `rewardResource`: resource id
- `rewardAmount`: non-negative int
- `coreDamage`: positive int

### Recipes

- `inputResource`: optional resource id
- `inputAmount`: optional int, defaults to 0
- `outputResource`: resource id
- `outputAmount`: positive int
- `durationTicks`: positive int

### Waves

- `startTick`: non-negative int
- `spawns`: comma-separated `enemyId:count`

### Incidents

- `minThreat`: non-negative int
- `maxThreat`: non-negative int
- `weight`: positive int

### Strings

Arbitrary localization keys to display strings.
