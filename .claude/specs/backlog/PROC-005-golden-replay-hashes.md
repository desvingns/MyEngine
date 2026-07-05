id: PROC-005
title: Golden replay-hash files asserted by tests
status: backlog
phase: process
source: architecture review 2026-07-04 (P3.2)

Context: canonical scenario hashes (e.g. sandbox 9c495d8ff30fd83d) live as prose in
STATE.md. Nothing fails automatically if a change silently shifts a hash.

Acceptance:
- Golden hashes are checked-in files (e.g. games/sandbox/src/test/resources/golden/*.hash)
  asserted by replay tests.
- Changing a golden file requires an explicit note in .ai/handoff.md (why the hash moved).
- scripts/me-sim-replay.ps1 compares against the golden files, not hardcoded values.
