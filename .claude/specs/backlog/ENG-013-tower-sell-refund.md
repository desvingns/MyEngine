id: ENG-013
title: Tower sell/refund
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `SellTowerCommand` refunds a content-defined ratio (validated 0..1) of cumulative spend (base + tiers), frees occupancy, deterministic.
- Goal-field invalidation on sell (ENG-002 interplay) is tested.
- Sell mid-run replay hash test; a pending sell command survives save (pending-queue persistence).

Closed: 2026-07-18

Implementation:
- `SellTowerCommand` is deterministic and serializes through the existing pending-command queue.
- Tower content requires `sellRefundRatio` as a decimal in inclusive range `0..1`; both sandbox and
  Signal Garden content packs supply it.
- Sale refunds `floor(cumulative base plus applied-tier spend per resource * ratio)`, atomically
  rejects capacity overflow, frees occupancy, removes tower metrics, and rebuilds the goal field
  before same-tick enemy movement.
- `SandboxTowerSellTest` covers base and multi-resource upgraded refunds, atomic capacity rejection,
  same-tick goal-field movement, deterministic repeated mid-run sell hashing, and pending-sell
  lifecycle restore.
- `SandboxSaveCodec.SAVE_VERSION` remains `6`; no ADR was required.

Verification:
- Full `./gradlew.bat test` -> pass; content validation -> pass (`validated 2 pack(s)`);
  replay -> pass (`463d87684ca6cbee`, `40c7bda7e3bc1316`); save-compat -> pass; benchmark -> pass
  (`canonical=335 ms`, `kill=70 ms`, `goal_field_rebuild_ns=6505600`).
- Required domain reviewers and final `me-verifier` -> pass. Initial test/content gate failures
  found missing required `sellRefundRatio` fields in a test fixture and the Signal Garden pack;
  both were repaired before the final successful rerun.
