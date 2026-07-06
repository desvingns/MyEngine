id: PROC-012
title: Emulator provisioning lane (Gradle managed devices)
status: backlog
phase: process
source: engine gap sweep 2026-07-06 (project review; unblocks the SG-004 device-pending acceptance)

Acceptance:
- Gradle managed device (or scripted AVD) definition; one command boots the emulator and runs the Android instrumentation smoke headless, emitting one JSON line.
- Consumed by the PROC-009 screenshot gate and device-lane reruns of the SG-004 save smoke.
- Windows dev box supported; documented fallback when virtualization is unavailable.
