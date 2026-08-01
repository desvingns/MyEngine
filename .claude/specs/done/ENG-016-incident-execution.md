id: ENG-016
title: Incident execution pipeline + RNG fix
status: done
phase: engine
source: engine gap sweep 2026-07-06 (project review; SandboxGame.kt:143 allocates IncidentDirector per tick with fresh SeededRandom(17), result discarded)

Acceptance:
- Selection moves off the per-tick fresh `SeededRandom(17)` onto the sim RNG stream, on a content-defined cadence (pacing window + cooldowns); the dead per-tick call is removed.
- Selected incidents EXECUTE via a content-declared effect interpreter (spawn wave, resource event, modifier).
- Full replay-hash coverage of selection + effects; no-incident packs remain valid (content fixture).
- Cooldown/pacing state persists in save (codec bump + migration).

## Completion

Closed 2026-08-02. ENG-016 is implemented and verified: optional typed incident effects use a
stateful deterministic director with a persistent simulation RNG cursor, cadence start/end ticks,
pacing threat windows, and cooldowns. The sandbox interpreter is atomic and supports spawn-wave,
resource-event, and modifier effects; repeated resource/modifier effects aggregate via `Long`
before overflow checks. `ContentValidationError` diagnostics include the cross-field incident path.
`SandboxSaveCodec` v10 persists incident director/RNG/modifier state and migrates v1-v9.

Verification: full Gradle test, projects, content validation, replay, save-compat, benchmark, and
diff-check lanes pass; focused `SandboxIncidentTest` and content tests pass; simulation and save
reviews pass. Canonical replay is `e4892bcc18f9d8dc`, kill replay is `a763da4ac32b15b4`;
remediation rerun benchmark is `sim=418 ms`, `kill=85 ms`, `spatial-index-1k=6.1036 ms`, and
`goal-field=10.427 ms`. Earlier first-run `614/120 ms` values are superseded. Gradle verification
uses process-local `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. No device proof is
claimed; existing Android/device and performance follow-ups remain manual-pending. Default pack
balance and Android production/render/input boundaries are unchanged.
