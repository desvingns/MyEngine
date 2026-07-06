id: ENG-029
title: Audio event hooks (snapshot event feed)
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Deterministic gameplay event feed on the snapshot (shot, hit, death, wave-start, build, sell) with tick + type; same seed produces the same event log (test). Shares the ENG-009 event plumbing.
- Content maps event types to sound refs; validation checks files exist.
- Android playback (SoundPool) consumes the feed; sim has no audio dependency; volume/mute is presentation state only.
