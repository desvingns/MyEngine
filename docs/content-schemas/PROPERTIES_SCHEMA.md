# MyEngine Content Properties Schema

Status: Phase 06 accepted; DX-008 hybrid format accepted
Last updated: 2026-07-16

Content packs use the DX-008 hybrid format defined by
[`ADR-0003-content-format-hybrid.md`](../DECISIONS/ADR-0003-content-format-hybrid.md): flat entity
definitions remain external `.properties` files, while nested assets use structured JSON. The
loader remains Android-free; Android packages the same content directory as app assets.

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
- `upgrade.<branch>.<tier>.range`: optional positive int
- `upgrade.<branch>.<tier>.damage`: optional positive int
- `upgrade.<branch>.<tier>.cooldownTicks`: optional positive int
- `upgrade.<branch>.<tier>.costResource`: optional resource id
- `upgrade.<branch>.<tier>.costAmount`: optional non-negative int

Upgrade tier fields are authored inside `towers.properties` under the base tower id. If any
field for a tier is present, all five tier fields are required. Branch ids must not contain dots,
and tiers are positive integers.

### Difficulties

`difficulties.properties` uses the same `<id>.<field>=<value>` format. The file is optional;
packs without it retain their existing behavior until a difficulty is selected.

- `healthMult`: positive decimal multiplier for enemy health
- `countMult`: positive decimal multiplier for each wave spawn count
- `rewardMult`: positive decimal multiplier for enemy reward
- `goldRateMult`: positive decimal multiplier for the final enemy payout

Difficulty selection materializes an effective registry before simulation. Health and spawn counts
use `floor(base * multiplier)` with a minimum of 1. The final payout applies `rewardMult` and then
`goldRateMult` as sequential data multipliers, retaining decimal precision and rounding the final
result half-up. Multipliers are parsed as decimal values and never through binary floating point.

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

## Nested Map Assets

`maps.json` is optional for migration compatibility, but required by a game that materializes a
content-defined world. It contains a top-level `maps` array. Each map has:

- `id`: unique map id.
- `width` and `height`: positive grid dimensions.
- `terrainRows`: exactly `height` strings, each exactly `width` characters.
- `terrainMapping`: one-character symbol keys mapped to `{ "tile": "<tile id>" }`; a mapping may
  add `{ "resource": { "id": "<resource id>", "amount": <non-negative integer> } }`.
- `spawns`: one-or-more `{ "id", "x", "y" }` named spawn objects.
- `core`: one `{ "x", "y" }` coordinate that points to the single terrain cell whose referenced
  tile has `isCore=true`.
- `terminalRules`: optional object governing the map's run boundary. `winCondition` is
  `"finite_waves"` by default, or `"no_win"`/`"endless"` for a map that never wins from wave
  completion. `leakBudget`, when declared, is a positive integer; reaching it loses the run before
  core-health exhaustion. Core-health exhaustion is always a loss.

The loader validates dimensions, row widths, unknown terrain symbols and tile/resource ids, spawn
and core bounds, exactly one core, a walkable path from every named spawn to the core, and terminal
rule values. Errors include `maps.json`, the map id, and a field path suitable for the
content-validation report.
