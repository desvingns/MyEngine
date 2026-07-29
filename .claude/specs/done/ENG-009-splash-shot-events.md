id: ENG-009
title: Splash damage + shot events
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Content: optional splashRadius + falloff per tower; validation rejects invalid values.
- Deterministic AoE resolution (stable target order, integer math rule pinned by test).
- Snapshot carries shot/hit events (source, target, tick) so render can animate projectiles; sim stays render-free; event log determinism test.
- Golden replay hashes re-baselined via handoff note; balance report covers splash towers.

Close-out (2026-07-28):
- Accepted scope: stable entity-id-ordered Manhattan AoE with an integer per-ring falloff rule;
  no content-pack balance values were shipped.
- `ShotEvent` and `HitEvent` are immutable, transient source/target/tick presentation data from
  the latest completed simulation tick. They are neither saved nor included in the stable hash.
- `SandboxSaveCodec.SAVE_VERSION` remains `8`; all runner gates and final verifier checks passed.
