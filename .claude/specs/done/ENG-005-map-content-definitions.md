id: ENG-005
title: Map definitions in content packs
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- New map content type: grid size, terrain row-strings, one-or-more named spawn points, core position; ContentLoader validates bounds, spawn->core walkability, exactly-one-core, with actionable errors and bad-map fixtures in the content-validate gate.
- Sandbox/MyTD build `TileWorld` from the pack; hardcoded 64x64 / spawn (1,1) / core (32,32) (SandboxGame.kt:110-111) are deleted.
- Equivalent-map fixture reproduces the prior replay hash; any golden re-baseline goes through an explicit `.ai/handoff.md` note (PROC-005 interplay).
- Save stores map id + content version; save-compat gate green (migration if schema changed).
- `docs/content-schemas/PROPERTIES_SCHEMA.md` updated; schema shape cites the DX-008 ADR once decided.

Closed: 2026-07-16

Evidence:
- `maps.json` now supplies the canonical sandbox 64x64 layout, named spawn, core, and resource node;
  `ContentLoader` validates dimensions, symbols/references, one core, bounds, and spawn-to-core paths.
- The sandbox builds `TileWorld` and routing coordinates from the selected map. The equivalent fixture
  preserves replay hashes `9c495d8ff30fd83d` and `83a65da1a7881b2c` with no golden re-baseline.
- `SandboxSaveCodec` v4 persists `mapId` plus content version, validates map/pack/content identity, and
  migrates v1-v3 saves through a sole available map.
- Full tests, content validation, replay, save-compat, and benchmark gates passed.
