id: MTD-005
title: Real render surface and touch input
status: done
phase: first-game
source: D:/Pet/MyTD/spec (EG-005, FR-001, FR-016, AC-005, AC-007)

Acceptance:
- Snapshot renders tiles, path, core, towers (with tier), and enemies.
- Touch input adapter: tap to select/build, drag pan, pinch zoom.
- Rendering/input do not mutate authoritative simulation state.
- Camera pan/zoom remains tested; Android debug assemble passes.

Evidence (2026-07-16):
- Accepted implementation: Android `SandboxRenderView` projects immutable snapshots through
  `PlaceholderRenderSurface` and draws tiles, path, core, tower tiers, enemies, and overlay with
  `Canvas`/`RenderPalette`. `MotionEvent` maps to the existing `InputAdapter` for tap, drag-pan,
  and pinch-zoom; only the Activity callback submits a produced command and advances the session.
- Runner -> pass: `:engine-render:test`, `:games:sandbox:test`, `:android:assembleDebug`, and
  `scripts\me-sim-replay.ps1`; canonical hash `9c495d8ff30fd83d` and kill hash
  `83a65da1a7881b2c` remain unchanged at tick 35.
- `me-verifier` -> pass: Android-free simulation, snapshot-only rendering, external content, and
  versioned saves all hold.

Limitations (manual device checks still pending):
- No emulator/device smoke ran. Verify tap builds after one tick, drag never builds, pinch changes
  zoom, draw order is tiles -> path -> entities, and debug rotation/process recreation preserves the
  stable hash and next command id.
- Profile prolonged pan/pinch with FrameMetrics/JankStats and Allocation Tracker. Each redraw now
  projects a snapshot/frame and creates intermediate primitive lists; this is a known performance
  risk, not a completed performance claim.
