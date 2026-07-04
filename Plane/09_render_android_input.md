# Phase 09 - Rendering, Android Shell And Input

Status: Planned

## Цель

Подключить presentation layer: render simulation snapshots, Android launcher, desktop launcher, camera, gestures and debug overlay. Rendering must observe state, not own state.

## Входы

- Phase 05-08 simulation systems
- Stack scaffold
- Android/Desktop modules

## Work Packages

### 09.1 Render State Boundary

- Define snapshot/presentation model.
- No simulation mutation from renderer.
- Threading assumptions documented.

### 09.2 Camera

- Pan.
- Zoom.
- World/screen coordinate conversion.
- Clamp to map bounds.

### 09.3 Tile And Entity Rendering

- Placeholder atlas or generated simple shapes.
- Tiles.
- Towers.
- Enemies.
- Core/base.
- Selection highlight.
- Range overlay.

### 09.4 Input

- Tap tile.
- Pan gesture.
- Pinch zoom.
- Build command creation.
- Selection state outside simulation or explicit command if authoritative.

### 09.5 Android Shell

- Lifecycle.
- Pause/resume.
- Save trigger.
- Back button policy.
- Orientation/screen size policy.

### 09.6 Desktop Shell

- Fast local run.
- Debug keys.
- Optional fixed window size presets.

### 09.7 Debug Overlay

- FPS.
- Simulation tick.
- Entity count.
- Wave.
- Selected tile.
- Last command/error.

## Deliverables

- Render module.
- Android launcher.
- Desktop launcher.
- Input adapter.
- Debug overlay.
- Basic smoke tests or manual checklist.

## Verification

- Desktop run.
- Android assemble.
- Unit tests for coordinate conversion.
- Manual smoke: pan/zoom/select/build command.
- If possible: screenshot/visual smoke.

## Acceptance Gates

- Simulation has no render dependency.
- Input creates commands.
- Android lifecycle does not corrupt simulation.
- Debug overlay exists and can be toggled.
- App can be launched on desktop or Android, or exact blocker is documented.

## Standalone Prompt

```text
Ты выполняешь Phase 09: rendering, Android shell and input.

Подключи render/presentation layer to existing simulation. Implement camera, placeholder tile/entity rendering, Android/Desktop launchers, pan/zoom/tap/build input and debug overlay. Rendering must not mutate authoritative simulation state.

Run desktop/android build where possible, add coordinate/input tests, update Plane progress.
```

