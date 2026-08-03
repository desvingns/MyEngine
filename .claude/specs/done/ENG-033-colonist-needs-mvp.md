id: ENG-033
title: Colonist needs MVP (hunger/rest)
status: done
phase: engine
owner: human
source: authored colony feature scope, human-authorized 2026-08-03; MySD evidence does not prove colony behavior

scope:
  status: accepted
  accepted_on: 2026-08-03
  accepted_by: human
  basis: explicit user request to unlock MyEngine specs; existing acceptance scope retained

requirements:
  - COL-FR-001: Content defines hunger/rest decay rates and need thresholds.
  - COL-FR-002: Threshold crossings deterministically enqueue eat/sleep jobs.
  - COL-FR-003: Need-vs-work arbitration is deterministic and stable.
  - COL-FR-004: Need levels migrate through versioned saves and appear as immutable snapshot HUD bars.
  - COL-NFR-001: The same seed and command stream remain deterministic for a 10k-tick soak.

Acceptance:
  - COL-AC-001: Content-defined needs with decay rates and thresholds validate; threshold crossings enqueue eat/sleep jobs.
  - COL-AC-002: Need-vs-work priority arbitration is deterministic and tested.
  - COL-AC-003: Need levels round-trip through a versioned save migration and immutable snapshot HUD bars; the 10k-tick determinism soak passes.

gates:
  - COL-GATE-001: Content validation passes for hunger/rest decay rates and thresholds.
  - COL-GATE-002: Unit/replay tests pass for threshold jobs and need-vs-work arbitration.
  - COL-GATE-003: Save compatibility passes for versioned need-level migration and snapshot projection.
  - COL-GATE-004: Determinism soak passes for 10,000 ticks with the same seed and command stream.

traceability:
  gate_trigger: Plane/15 colony re-entry alternative for an authored colony scope with named FRs
  evidence_boundary: MySD Gate 1 is accepted for its TD reference inventory and is not used as colony evidence

close_out:
  completed_on: 2026-08-03
  result: Added content-defined needs, deterministic threshold jobs and need-vs-work arbitration,
    immutable HUD need bars, NeedRecovery job effects, and SandboxSaveCodec v17 with v1-v16 migration.
  verification: Full test/projects/content validation/replay/save-compat/benchmark/selfcheck,
    Android assembleDebug, focused need tests, and git diff --check passed.
  reviewer_boundary: Conditional roster reviewers timed out after bounded waits; local simulation,
    save, render, content, and Android boundary review found no blocker. No device/emulator proof claimed.
