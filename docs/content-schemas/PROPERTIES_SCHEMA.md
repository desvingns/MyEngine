# MyEngine Content Properties Schema

Status: Phase 06 accepted; DX-008 hybrid format accepted; ENG-016, ENG-028, and ENG-009 fields documented
Last updated: 2026-08-02

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
- `spritePath`: optional pack-relative file path, or
- `atlasPath` + `atlasKey`: optional pair identifying a key in the pack-relative minimal atlas
  index; declare either the sprite field or the atlas pair, never both

### Resources

- `displayKey`: localization key

### Towers

- `displayKey`: required localization key
- `range`: positive int
- `damage`: positive int
- `cooldownTicks`: positive int
- `costResource`: resource id
- `costAmount`: non-negative int
- `sellRefundRatio`: required decimal in the inclusive range `0..1`; selling returns
  `floor(cumulative base and applied-tier spend per resource * sellRefundRatio)`
- `targetingMode`: optional targeting priority: `first`, `last`, `nearest`, `strongest`, or
  `weakest`; omitted schema-v1 content defaults deterministically to `nearest`, while an authored
  invalid value is rejected
- `splashRadius`: optional positive integer Manhattan radius centered on the selected primary
  target; omitted means the tower damages only that primary target
- `falloff`: optional integer percentage from `0` through `100` of base damage removed per
  Manhattan-distance ring; it requires `splashRadius` and defaults to `0` when omitted
- `upgrade.<branch>.<tier>.displayKey`: required localization key when the tier exists
- `upgrade.<branch>.<tier>.range`: optional positive int
- `upgrade.<branch>.<tier>.damage`: optional positive int
- `upgrade.<branch>.<tier>.cooldownTicks`: optional positive int
- `upgrade.<branch>.<tier>.costResource`: optional resource id
- `upgrade.<branch>.<tier>.costAmount`: optional non-negative int
- `upgrade.<branch>.<tier>.spritePath`: optional pack-relative file path, or
- `upgrade.<branch>.<tier>.atlasPath` + `upgrade.<branch>.<tier>.atlasKey`: optional atlas pair

Upgrade tier fields are authored inside `towers.properties` under the base tower id. If any
field for a tier is present, all six tier fields are required. Branch ids must not contain dots,
and tiers are positive integers. Every tower and tier `displayKey` must resolve in
`strings.properties`.

For a splash tower, candidates are live enemy entities within the declared Manhattan radius and
are resolved in ascending entity-id order. For distance `d`, integer damage is
`floor(baseDamage * max(0, 100 - d * falloff) / 100)`; zero-damage candidates receive no damage
and emit no hit event. This rule uses integer arithmetic only and is the authoritative balance
semantics; no game pack is required to declare splash values.

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
- `isElite`: optional boolean; defaults to `false`
- `isBoss`: optional boolean; defaults to `false`; an enemy cannot be both elite and boss
- `healthScalePercent`: optional positive percentage from `1` through `10000`; defaults to `100`
- `speedScalePercent`: optional positive percentage from `1` through `10000`; defaults to `100`
- `rewardScalePercent`: optional positive percentage from `1` through `10000`; defaults to `100`
- `spritePath`: optional pack-relative file path, or `atlasPath` + `atlasKey` optional pair

Enemy scaling is materialized when the wave spawns. Health and speed use integer floor with a
minimum of one; rewards use deterministic half-up rounding. Elite/boss flags and effective enemy
stats are retained in the versioned sandbox save so a mid-wave restore does not fall back to base
content values. Boss state is exposed to immutable render snapshots for presentation emphasis.

### Buildings

`buildings.properties` is optional and intentionally contains no gameplay fields yet. It uses the
same `<id>.<field>=<value>` format and provides a smallest data-driven home for future building
definitions:

- `spritePath`: optional pack-relative file path, or
- `atlasPath` + `atlasKey`: optional pair

All declared visual paths must stay inside the content-pack root and point to an existing file. The
minimal atlas index is a UTF-8 text file with one region key per non-empty, non-comment line; a
`key=value` line is also accepted. Missing files, path escapes, unreadable indexes, and missing keys
are rejected with pack, file, definition id, field, path, and/or key context. Omitted references
remain valid and use deterministic palette fallback in placeholder consumers. The content loader
only validates opaque metadata; sprite decoding remains platform-owned.

### Recipes

- `inputResource`: optional resource id
- `inputAmount`: optional int, defaults to 0
- `outputResource`: resource id
- `outputAmount`: positive int
- `durationTicks`: positive int

### Waves

- `startTick`: non-negative int
- `spawns`: comma-separated `enemyId:count`
- `modifier.<index>.healthPercent`: optional positive percentage from `1` through `10000`
- `modifier.<index>.speedPercent`: optional positive percentage from `1` through `10000`
- `modifier.<index>.count`: positive number of consecutive enemies covered by this modifier
- `earlyCallBonusResourceId`: optional resource id for the bonus granted by an accepted early
  wave call
- `earlyCallBonusAmount`: optional positive int paired with `earlyCallBonusResourceId`

The early-call bonus fields must be either both present or both absent. When present, the resource
id must resolve to a resource defined by the same content pack and the amount must be positive;
partial, non-positive, or unknown-resource values are rejected during content validation.

Wave modifier indexes must be contiguous from `0`. Modifiers cover enemies in numeric-index order
and within each wave spawn entry's authored order; once all declared counts are covered, remaining
enemies use the unmodified effective enemy stats. Enemy scaling is applied before the wave modifier.

### Incidents

`incidents.properties` is optional. A pack without this file, or with no incident definitions,
remains valid and has no selectable incidents. Definitions use the standard
`<incidentId>.<field>=<value>` form. `minThreat`/`maxThreat` are the legacy threat envelope;
the stateful cadence selector uses the explicit pacing window below, which defaults to that
envelope when omitted.

- `minThreat`: non-negative int
- `maxThreat`: non-negative int; must be greater than or equal to `minThreat`
- `weight`: required positive int used for weighted selection among eligible incidents
- `cadenceStartTick`: optional non-negative long, inclusive; defaults to `0`
- `cadenceIntervalTicks`: optional non-negative int; defaults to `0`. A value of `0` disables
  cadence eligibility (the compatibility selector may still use the legacy threat envelope).
  The legacy spelling `cadenceTicks` is accepted as an alias when `cadenceIntervalTicks` is absent.
- `cadenceEndTick`: optional non-negative long, inclusive; when present it must be greater than or
  equal to `cadenceStartTick`; omitted means that the cadence window has no explicit end
- `pacingMinThreat`: optional non-negative int; defaults to `minThreat`
- `pacingMaxThreat`: optional non-negative int; defaults to `maxThreat` and must be greater than or
  equal to `pacingMinThreat`
- `cooldownTicks`: optional non-negative int; defaults to `0`. After a selected incident, the same
  incident is ineligible until `selectionTick + cooldownTicks`; `0` means no cooldown state is set
- `effects`: optional comma-separated typed effect descriptors, or indexed `effect.0=...`,
  `effect.1=...` fields. Use one form only; indexed entries are applied in numeric index order.

Cadence eligibility is inclusive at `cadenceStartTick`, then every
`cadenceIntervalTicks` ticks, through the inclusive `cadenceEndTick` when present. An incident is
selected only when the current tick is in its cadence window, the current threat budget is inside
`pacingMinThreat..pacingMaxThreat`, and its cooldown has expired. Eligible incidents are sorted by
id before weighted selection. The stateful director consumes one persistent simulation RNG cursor;
the cursor, cooldowns, selection history, and typed effects are part of the versioned sandbox save.

Typed effect syntax and validation:

- `spawn_wave:waveId`: queues the referenced content wave; `waveId` must resolve in the same pack
- `resource_event:resourceId:amount`: adds a positive integer `amount` of the referenced resource;
  `resourceId` must resolve in the same pack
- `modifier:modifierId:amount:durationTicks`: applies a positive integer `amount` for a positive
  integer `durationTicks`; `modifierId` is a non-blank data-defined id

Repeated resource events with the same resource id and repeated modifiers with the same modifier id
are aggregated before integer-overflow and inventory-capacity checks; modifier duration uses the
maximum declared duration. Effect preflight is atomic: unknown references, invalid amounts,
overflow, or capacity failure leave the authoritative state unchanged. Invalid cross-field values
and effect syntax are reported as `ContentValidationError` diagnostics with the
`incidents.properties` file, incident id, and field path (for example `effects[0]`); unknown waves
and resources are reported at the corresponding indexed effect path.

### Sounds

`sounds.properties` is optional. A pack without it remains valid and has no sound mapping. Each
entry maps a gameplay event id to a pack-relative audio file path:

```properties
shot=sounds/shot.wav
wave_start=sounds/wave-start.wav
```

Supported event ids are `shot`, `hit`, `death`, `wave-start`, `build`, and `sell`. Event ids are
trimmed, case-insensitive, and accept `_` as an alias for `-`; the normalized id is the stable
`GameplayEventType` id. Values are opaque references for platform presentation consumers; the
content loader does not decode audio.

Validation rejects unknown event ids, duplicate ids after normalization, blank paths, paths that
escape the content-pack root, and paths that do not resolve to an existing regular file. Valid
references remain pack-relative so Android asset consumers can resolve them without adding an
audio dependency to simulation.

### Strings

Arbitrary localization keys map to display strings. In addition to resource, tower, and tower-tier
`displayKey` references, every pack must define the HUD keys below; content validation rejects a
missing reference or required HUD key.

- `hud.resources`
- `hud.wave`
- `hud.nextWave`
- `hud.coreHealth`
- `hud.build`
- `hud.upgrade`
- `hud.damage`
- `hud.kills`
- `hud.tier`

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
