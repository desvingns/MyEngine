id: ENG-025
title: Flying enemies
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review; split from ENG-011)

Acceptance:
- Enemy movement mode ground|air in content; air routes deterministically ignoring blockers; ground unchanged; path-block rule (ENG-002) exempts air.
- Towers gain canTargetAir/canTargetGround; targeting filters respect the flags; a pack with air waves but no air-capable tower produces a balance-report warning.
- Mixed-wave replay hash + air-leak tests; movement mode persisted in save.

Close-out:
- Added `MovementMode` content/runtime state with ground compatibility defaults and air routing
  through a blocker-ignoring GoalField.
- Added tower air/ground capability validation and deterministic target filtering, including splash
  targets and a balance warning for air waves without an air-capable tower.
- Bumped `SandboxSaveCodec` v20 -> v21; v1-v20 saves migrate air movement to ground, while v21
  persists the movement mode.
- Added focused content, defense, sandbox mixed-wave, air-leak, replay, and save-migration coverage.
- No Android production, renderer, or ADR changes were required.

Verification:
- `gradlew test`, `gradlew projects`, content validation, replay, save-compat, benchmark,
  selfcheck, required headless inspect, `:android:assembleDebug`, and `git diff --check` passed.
- No device/emulator or visual-golden proof is claimed.
