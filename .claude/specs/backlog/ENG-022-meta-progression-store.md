id: ENG-022
title: Meta-progression store
status: backlog
phase: engine
source: engine gap sweep 2026-07-06 (project review)

Acceptance:
- Persistent profile store separate from run saves: unlock flags + meta currency earned from run summaries (ENG-014); independently versioned with its own migration test.
- Content declares unlockables; ContentLoader cross-refs unlock ids; build commands rejected for locked towers (tested).
- Sim treats the unlock set as immutable scenario input; replays embed it so hashes reproduce.
