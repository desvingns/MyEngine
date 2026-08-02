id: ENG-032
title: Construction system (blueprint, haul, build)
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Blueprint placed via command with content-defined material costs; hauling (ENG-004) delivers; a build job applies work ticks; the completed building spawns.
- Blueprint blocking behavior pinned by test (non-blocking until built, or a content flag).
- Cancel refunds delivered materials deterministically; save mid-construction roundtrips; replay hash scenario.

Close note (2026-08-02):
- Added typed place/cancel blueprint commands, a source-aware construction-site ledger, and
  construction haul destinations backed by the existing ENG-004 hauling flow.
- Blueprints remain non-blocking until completion; placement performs a prospective route-safety
  check, and completion occupies the tile and rebuilds the goal field.
- Source selection is automatic and deterministic: eligible `HaulSourceStore` entries are tried in
  ascending `sourceId` order and retried when a source is unavailable.
- Cancel returns delivered and in-transit material to each haul's original `HaulSourceStore`,
  releases reservations, and persists through `SandboxSaveCodec` v16 with v1-v15 migration.
- Full tests, content/replay/save-compat/benchmark/selfcheck gates, Android assembleDebug, and
  `git diff --check` passed. No ADR or plugin version bump was needed.
