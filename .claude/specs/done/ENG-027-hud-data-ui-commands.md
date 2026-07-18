id: ENG-027
title: HUD snapshot data + UI command surface
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- `EngineSnapshot` gains a HUD block: resource balances, wave counter + next-wave countdown, core HP, buildable tower list with costs/tiers — all content-derived, zero hardcoded strings (l10n via strings content, content-validate covered).
- Render draws build menu + select/upgrade panel from the snapshot only; taps produce Build/Upgrade commands through the queue.
- Per-tower kills/damage tracked deterministically in defense metrics and shown in the info panel.
- Headless test asserts the HUD block at fixed ticks; canonical replay hashes unchanged.
