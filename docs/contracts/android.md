# android Contract

Status: Draft  
Owner: Android shipping shell

## Responsibilities

- Android application entry point.
- Lifecycle handling.
- Touch/gesture input forwarding.
- Save directory and permissions boundary.
- Frame pacing and platform configuration.

## Non-Responsibilities

- Authoritative simulation logic, content validation rules, core persistence model, or desktop tools.

## Dependencies

- May depend on game modules, `engine-render`, and approved Android/libGDX backend dependencies.
- Simulation modules must not depend on `android`.

## Public Contracts

- Activity/application shell.
- Input forwarding adapter.
- Android save-location adapter.
- Android smoke test entry points.

## Test Gates

- Startup smoke test.
- Pause/resume smoke test.
- Input-to-command mapping test.
- Save directory access test.

