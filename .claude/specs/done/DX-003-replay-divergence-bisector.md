id: DX-003
title: Replay divergence bisector
status: done
owner: codex
blocked_by: none
phase: dx
source: engine gap sweep 2026-07-06 (project review)

start_gates:
  - baseline_replay_pass
  - documented_archive_baseline_excluded
  - golden_change_policy_in_handoff
  - "per_tick_trajectory_source_approved: DX-003 owns checked-in per-tick trajectory fixtures for canonical, kill, and resist scenarios; PROC-005 final-hash goldens remain unchanged"

Acceptance:
- Devtools records a per-tick hash log (current `replay-inspect` is final-hash-only); a comparer reports the first divergent tick + a state diff summary at that tick.
- Bisects against DX-003-owned per-tick trajectory fixtures derived from the PROC-005 canonical, kill, and resist scenarios; PROC-005 final-hash goldens remain unchanged. Exit codes are usable as a gate.
- Test with an intentionally perturbed run fixture.

Completed: 2026-08-04
Close-out owner: Codex / me-docs
Result: Added deterministic tick-0 plus actual-tick trajectories in versioned
`dx-003-trajectory-v1` JSONL; canonical, kill, and resist fixtures match the PROC-005
final hashes. The comparer reports the first divergent tick, sorted `changed_fields`,
and a `hash_only` fallback. `replay-inspect --trajectory` and `replay-bisect` use exit
codes 0 for match, 1 for divergence, and 2 for invalid input or runtime errors.
Decision: The resistance=50 behavior uses a narrow devtools seam. Legacy `replay-inspect`
and the PROC-005 `.hash` resources / `scripts/me-sim-replay.ps1` contract are unchanged.
No Android, save-schema, continuous-runtime, ADR, or plugin-contract changes were made.
Verification: Focused/full tests, `projects`, content validation, replay, save-compat,
benchmark (`sim_ms=973`), selfcheck, headless inspect, Android `assembleDebug`, CLI exit-code
checks, and `git diff --check` passed. The canonical-perturbed fixture diverges at tick 5
on `core_health`.
