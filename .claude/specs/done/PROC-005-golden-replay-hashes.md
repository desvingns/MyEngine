id: PROC-005
title: Golden replay-hash files asserted by tests
status: done
owner: codex
blocked_by: none
phase: process
source: architecture review 2026-07-04 (P3.2)

start_gates:
  - baseline_replay_pass
  - documented_archive_baseline_excluded
  - golden_change_policy_in_handoff

Context: canonical scenario hashes (e.g. sandbox 9c495d8ff30fd83d) live as prose in
STATE.md. Nothing fails automatically if a change silently shifts a hash.

Acceptance:
- Golden hashes are checked-in files (e.g. games/sandbox/src/test/resources/golden/*.hash)
  asserted by replay tests.
- Changing a golden file requires an explicit note in .ai/handoff.md (why the hash moved).
- scripts/me-sim-replay.ps1 compares against the golden files, not hardcoded values.

Completed: 2026-08-04
Close-out owner: Codex / me-docs
Result: Added checked-in `canonical.hash`, `kill.hash`, and `resist.hash` resources; replay
tests load those contracts, and `scripts/me-sim-replay.ps1` compares discovered scenario output
against the corresponding golden files while preserving generated-game discovery.
Decision: Golden files are deterministic behavior contracts. Any intentional golden update
requires an explicit reason in `.ai/handoff.md`; DX-003 remains a separate follow-up for
per-tick replay-divergence bisection.
Verification: `:games:sandbox:test`, `:engine-devtools:test`, full `gradlew test`, `projects`,
content validation, replay, negative mismatch handling, save compatibility, benchmark, selfcheck,
headless inspect, Android `assembleDebug`, and `git diff --check` passed. No device, emulator, or
visual-golden proof is claimed.
