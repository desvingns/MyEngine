id: ENG-035
title: Resource extractor building
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review; split from ENG-023)

Acceptance:
- Extractor building type: requires an adjacent/underlying resource node (placement validated), produces into its own inventory via the ProducerSystem path at a content-defined rate.
- Node depletion (finite/infinite per content) is deterministic.
- Save + replay coverage; output consumable by hauling (ENG-004) and belts (ENG-023).

Result:
- Implemented finite/infinite content resource nodes, output-only extractor recipes, deterministic
  underlying/adjacent binding, partial final batches, stable `producer:<id>` haul sources, and
  source consumption through ENG-004 hauling.
- `SandboxSaveCodec` is v19 with v1-v18 migration; depleted node state and extractor bindings /
  progress round-trip deterministically. Belt transport remains the separate ENG-023 scope.
- No new command, Android/UI, or belt system was introduced; no ADR was required.

Verification:
- Full Gradle tests, content validation, replay, save compatibility, benchmark, selfcheck,
  required headless inspect, and `git diff --check` passed.
