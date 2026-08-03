id: ENG-023
title: Conveyor transport MVP
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- engine-logistics belt building (straight + corner); items advance at content-defined ticks-per-cell; deterministic update order pinned by test; backpressure (full belt stalls) deterministic.
- Endpoints pull from extractor/producer output and push into building/core inventories.
- Items-on-belt persist in save (codec bump + migration); extractor->belt->core 1k-tick replay hash scenario; 100-belt throughput benchmark (PROC-004 feed).

Result:
- Added Android-free straight/corner belt cells with content-defined ticks-per-cell, deterministic
  sink-to-source movement, full-belt backpressure, producer/core/entity endpoints, and stable item
  ordering. Producer output is consumed from unreserved `producer:<id>` haul sources.
- `SandboxSaveCodec` is v20 with v1-v19 empty-belt migrations and persisted belt cells/items;
  legacy replay hashes remain unchanged. Devtools expose a 100-belt x 1000-tick throughput report.
- No new Android/UI surface, job semantics, splitting/merging, priorities, loops, or ADR was added.

Verification:
- Focused conveyor and sandbox belt tests, full `gradlew test`, `gradlew projects`, content
  validation/schema drift, replay, save compatibility, benchmark, selfcheck, required headless
  inspect, Android `assembleDebug`, and `git diff --check` passed.
