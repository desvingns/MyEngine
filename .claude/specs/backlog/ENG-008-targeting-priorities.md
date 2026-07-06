id: ENG-008
title: Targeting priority modes
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Modes first/last/nearest/strongest/weakest implemented as pure selectors with entity-id tiebreak; per-type default from content, per-tower override via a command through the queue.
- Mode persisted in save (codec bump + migration test); snapshot exposes the mode for HUD.
- Unit test per mode plus a replay hash test covering a mid-run mode switch.
