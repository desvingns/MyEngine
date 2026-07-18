# MyEngine Engine Roadmap

Aggregated engine gaps across all games, with demand counts. Rules:
`docs/agentic/SPEC_BOARD.md` (Engine Roadmap, Gap Dedup Rule). A gap demanded by
two or more games outranks single-game gaps of the same severity.

Updated by: the `/me-spec` backlog bridge (new gaps / new demand) and `/me`
close-out (status changes).

Last updated: 2026-07-18

## Capabilities

| Capability | Cards | Demanded by | Demand | Status |
|---|---|---|---:|---|
| Defense kill-reward deposit into player resources | SG-002, MTD-001 | signal-garden, mytd | 2 | **done** (SG-002 implemented 2026-07-04; MTD-001 closed 2026-07-05 as duplicate; MyTD gold maps to content-defined `rewardResource`) |
| Render surface + palette (snapshot -> RenderFrame) | SG-003 (+follow-up), MTD-005 | signal-garden, mytd | 2 | **done** (MTD-005 accepted 2026-07-16: Android Canvas consumes immutable RenderFrame, MotionEvent uses InputAdapter, scoped JVM/build/replay gates pass; device smoke and performance profiling remain manual-pending) |
| Content pack authoring/validation (game pack) | SG-001 | signal-garden | 1 | done (2026-07-04) |
| Android lifecycle save smoke (incl. any-tick save via pending-CommandQueue persistence) | SG-004 (+follow-up) | signal-garden | 1 | **done** (SG-004 implemented 2026-07-04; follow-up closed 2026-07-05; `SandboxSaveCodec` v1->v2, quiescent-save precondition dropped) |
| Balance report with suspicious-value checks | SG-005 | signal-garden | 1 | **done** (SG-005 implemented 2026-07-05: devtools `balance-report`/`balance-delta` JSON compares baseline vs changed content and flags enemy/core/resource deltas) |
| Gold cost gating in placeTower | MTD-002 | mytd | 1 | **done** (2026-07-05; existing generic `tower.costResource`/`costAmount` gate verified with `SandboxTowerCostGatingTest`) |
| Tower upgrade hook | MTD-003 | mytd | 1 | **done** (2026-07-05; content-defined upgrade tiers, `UpgradeTowerCommand`, deterministic spend/reject, save v3 branch+tier persistence) |
| Difficulty modifiers | MTD-004 | mytd | 1 | **done** (2026-07-16; data-defined `DifficultyContent`, deterministic pre-tick materialization, easy/normal/hard values from the MyTD balance plan) |
| Command DTO relocation out of engine-render + InputAdapter state fix | ENG-024 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; DTOs in `engine-core` command package, explicit `InputUiState`, caller-owned `CommandId`; full test/replay/save-compat/Android/static gates pass; hashes unchanged) |
| Map definitions in content packs (size, terrain, spawns, core) | ENG-005 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; DX-008 hybrid JSON map asset, validated `maps.json`, data-driven sandbox world/routing, v4 map/content save identity, canonical hashes unchanged) |
| Win/lose conditions + run summary | ENG-014 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; map-defined terminal rules, immutable snapshot summary, save v5 terminal-state persistence, all gates pass) |
| Android SurfaceView renderer + Choreographer fixed-tick loop | ENG-026 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-16; 20 Hz Android-local Choreographer policy, immutable RenderFrame SurfaceView, command-queue-only input, pause/save/Bundle command-ID restoration; JVM/build/replay/save-compat gates pass, device/performance checks manual-pending) |
| HUD snapshot data + UI command surface | ENG-027 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | **done** (2026-07-18; content-derived immutable HUD snapshot, Android build/select/upgrade panels through `InputAdapter`, deterministic per-tower damage/kills, save v6 metrics persistence; full gates pass, device/layout/performance checks manual-pending) |
| Goal-field pathfinding + repath on world change (mazing) | ENG-002 | mytd, vision:td, vision:mindustry-like | 3 | **done** (2026-07-18; deterministic `GoalField`, prospective all-spawn placement rejection, same-tick reroute, v6-derived field/legacy canonicalization; replay/save/benchmark gates pass) |
| Multiple spawn points + per-wave routing | ENG-007 | vision:td, vision:mindustry-like | 2 | backlog |
| Targeting priority modes | ENG-008 | mytd, vision:td | 2 | backlog |
| Splash damage + shot events | ENG-009 | mytd, vision:td, vision:mindustry-like | 3 | backlog |
| Status effects framework | ENG-010 | mytd, vision:td, vision:mindustry-like | 3 | backlog |
| Enemy armor + damage types | ENG-011 | vision:td, vision:mindustry-like | 2 | backlog |
| Flying enemies | ENG-025 | vision:td | 1 | backlog |
| Boss/elite enemies + wave modifiers | ENG-012 | mytd, vision:td | 2 | backlog |
| Tower sell/refund | ENG-013 | mytd, vision:td | 2 | backlog |
| Game speed control (presentation-side) | ENG-015 | mytd, vision:td, vision:rimworld-like | 3 | backlog |
| Endless wave generation | ENG-018 | vision:td, vision:mindustry-like | 2 | backlog |
| Walls + player-placed blockers | ENG-019 | vision:td, vision:mindustry-like | 2 | backlog |
| Spatial index + 1k-entity benchmark | ENG-020 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | backlog |
| Save slots + autosave policy | ENG-021 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | backlog |
| Meta-progression store | ENG-022 | mytd, vision:td | 2 | backlog |
| Sprite/atlas references in content schema | ENG-028 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | backlog |
| Audio event hooks (snapshot event feed) | ENG-029 | mytd, vision:td, vision:rimworld-like, vision:mindustry-like | 4 | backlog |
| Wave preview + early wave call | ENG-030 | mytd, vision:td | 2 | backlog |
| A* point-to-point pathfinding for agents | ENG-001 | vision:rimworld-like, vision:mindustry-like | 2 | backlog |
| Job execution system (JobBoard wired into tick) | ENG-003 | vision:rimworld-like, vision:mindustry-like | 2 | backlog |
| First worker agent MVP (hauling) | ENG-004 | vision:rimworld-like, vision:mindustry-like | 2 | backlog |
| Stockpile zones + designations | ENG-031 | vision:rimworld-like, vision:mindustry-like | 2 | backlog |
| Construction system (blueprint, haul, build) | ENG-032 | vision:rimworld-like, vision:mindustry-like | 2 | backlog |
| Colonist needs MVP (hunger/rest) | ENG-033 | vision:rimworld-like | 1 | backlog |
| Incident execution pipeline + RNG fix | ENG-016 | vision:rimworld-like, vision:mindustry-like, vision:td | 3 | backlog |
| Research/tech tree + unlock gating | ENG-017 | vision:mindustry-like, vision:td | 2 | backlog |
| Seeded procedural map generation | ENG-006 | vision:rimworld-like, vision:mindustry-like, vision:td | 3 | backlog |
| Enemy attacks on structures | ENG-034 | vision:mindustry-like | 1 | backlog |
| Resource extractor building | ENG-035 | vision:mindustry-like, vision:rimworld-like | 2 | backlog |
| Conveyor transport MVP | ENG-023 | vision:mindustry-like | 1 | backlog |
| New-game scaffolder script | DX-001 | agent pipeline | - | backlog |
| Headless state inspector (agent eyes) | DX-002 | agent pipeline | - | backlog |
| Replay divergence bisector | DX-003 | agent pipeline | - | backlog |
| Desktop content hot-reload | DX-004 | agent pipeline | - | backlog |
| Schema-docs drift gate | DX-005 | agent pipeline | - | backlog |
| Engine cookbook (agent task recipes) | DX-006 | agent pipeline | - | backlog |
| Fuzz tests for ContentLoader + SaveCodec | DX-007 | agent pipeline | - | backlog |
| ADR: JSON vs properties content format | DX-008 | agent pipeline | - | **done** (2026-07-16; ADR-0003 accepts `.properties` for flat definitions and JSON for nested assets) |
| Codex adapter parity audit + selfcheck coverage | PROC-011 | process | - | backlog |
| Emulator provisioning lane (managed devices) | PROC-012 | process | - | backlog |
| Spec board hygiene | PROC-013 | process | - | backlog |
| Android release build lane | PROC-014 | process | - | backlog |

## Known duplicates

- **MTD-001 duplicates SG-002** (reward deposit): SG-002 shipped in `DefenseRuntime`
  (TowerUpdateResult -> inventory deposit). MTD-001 is closed by reference; MyTD's gold balance is
  a content-defined resource, not a separate engine concept.

## Notes

- Statuses in `.claude/specs/backlog/*.md` cards may lag reality (SG-001..003 cards
  still said `backlog` after implementation). Close-out now flips card status;
  see `docs/agentic/PIPELINE.md` step 7. (SG-001..003 statuses flipped to done in the
  2026-07-06 review; directory moves deferred to PROC-013.)
- `vision:*` tags (2026-07-06 gap sweep) mark demand from the target-genre vision
  (RimWorld-like / Infinitode+BlockDefense TD / Mindustry-like) rather than an existing
  game repo; each vision direction counts as 1 in Demand.
- The 2026-07-06 gap sweep (ENG-001..035, DX-001..008, PROC-011..014; see
  `docs/reviews/2026-07-06-project-and-backlog-review.md`) materializes the sequencing
  PROC-003 asks for; PROC-003 may be closed by reference once the recommended order
  below is adopted.

## Recommended order (2026-07-06 review)

1. Human: commit the in-flight MTD-003 close-out changes; then run PROC-013 (board hygiene).
2. MTD-004 -> ENG-024 -> ENG-005 -> ENG-014 -> MTD-005 -> ENG-026 -> ENG-027 =
   **first playable Android TD milestone** (complete 2026-07-18; manual device/layout/performance
   evidence remains explicitly pending).
3. P1 opener: ENG-002 (done) -> ENG-013 -> ENG-008 -> ENG-015 -> ENG-030. The next exact backlog
   action is `ENG-013`.
4. DX-008 is done: use its hybrid-format ADR for ENG-017/ENG-028 schema work. Other high-leverage
   pipeline cards remain DX-002, DX-006, and DX-005.

## Deliberately not carded (2026-07-06, bounded scope)

Power grid, fluid transport, unit production/control (Mindustry deep-end);
temperature/roofing, animals, trading (RimWorld deep-end); achievements/quests,
tutorials (game-side, not engine). Revisit when a game spec demands them.
