id: DX-003
title: Replay divergence bisector
status: backlog
phase: dx
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Devtools records a per-tick hash log (current `replay-inspect` is final-hash-only); a comparer reports the first divergent tick + a state diff summary at that tick.
- Bisects against PROC-005 golden trajectories; exit codes usable as a gate.
- Test with an intentionally perturbed run fixture.
