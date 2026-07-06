id: PROC-014
title: Android release build lane
status: backlog
phase: process
source: engine gap sweep 2026-07-06 (project review; games ship exclusively on Android)

Acceptance:
- `bundleRelease` works: signing via untracked keystore properties; R8 rules keep sim + content loading intact, proven by a release-variant scenario smoke test.
- versionCode/versionName automation policy + per-game applicationId strategy documented.
- Release AAB size + cold-start metrics recorded (feeds PROC-004/PROC-010).
