id: ENG-022
title: Meta-progression store
status: done
owner: codex
blocked_by: none
phase: engine
source: engine gap sweep 2026-07-06 (project review)

start_gates:
  - baseline_content_validate
  - baseline_replay_pass
  - baseline_save_compat_pass
  - focused_eng022_tests
  - archive_baseline_excluded

Acceptance:
- Persistent profile store separate from run saves: unlock flags + meta currency earned from run summaries (ENG-014); independently versioned with its own migration test.
- Content declares unlockables; ContentLoader cross-refs unlock ids; build commands rejected for locked towers (tested).
- Sim treats the unlock set as immutable scenario input; replays embed it so hashes reproduce.

Completion:
- Closed: 2026-08-05.
- Scope: Android-free profile codec/store in the sandbox proof surface; optional `meta-progression.json` content, meta-gated tower/building/recipe targets, run-save v22 scenario provenance, and backward-compatible replay `unlock_ids` metadata.
- Decisions: profile currency and credited terminal run ids stay outside run saves; duplicate run ids are idempotent; unlock context is normalized and frozen at runtime creation; legacy saves/replays default to an empty set.
- Verification: focused and full Gradle tests, `projects`, Android `assembleDebug`, content
  validation, replay, save compatibility, benchmark, selfcheck, headless inspection, and
  `git diff --check` passed. The game-spec sync report is not applicable because this card came
  from the internal engine gap sweep rather than an `EG-*` game-spec source bundle.
