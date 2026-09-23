# ENG-036 Runtime Benchmark Contract

Status: Post-helper paired03 calibration/comparison passed; headless gate accepted locally, not published
Last updated: 2026-09-23

## Purpose

`scripts/me-benchmark.ps1` remains the broad scenario/goal-field smoke report. Its one cold
`sim_ms` value is not a warmed median baseline and cannot prove ENG-036's <=5% overhead gate.
`scripts/me-runtime-benchmark.ps1` is the original separate-process same-machine comparison.
Its retained noisy failures motivated the controlled paired gate described below; final ENG-036
acceptance uses `scripts/me-runtime-paired-benchmark.ps1`, not a cold timing or selective rerun.

The wrapper builds `:engine-devtools:installDist` in two independent checkouts, then launches the
same `scripts/perf/RuntimeSessionBenchmark.java` source against each distribution. The source uses
only the existing `SandboxSession` API so it works with both the old baseline and extracted API.
It does not edit either checkout, create a branch, remove files, or replace the ordinary gates.

## Measurement

- Fixed seed 7, tick 0, one pending tower command at tick 1, then 35 simulation ticks.
- Canonical tower position `(30,32)`, expected hash `12a65fd2b87593cf`.
- Kill scenario tower position `(2,2)`, expected hash `bb37eefc1903cc77`.
- Only `SandboxSession.step(35)` is timed. Content loading, construction/restore, projection,
  serialization, and hash verification are outside the timed block.
- Defaults: 12 warmup batches, 21 measured batches, 8 independent sessions per batch, 3 fresh
  JVM forks per checkout, fixed 512 MiB initial/maximum heap. Fork order alternates baseline-first
  and candidate-first to reduce systematic ordering bias.
- Every session, including every warmup, must match its exact replay golden; otherwise fail.
- Report raw batch nanoseconds, per-fork median, median of fork medians, and per-scenario percentage
  regression. A scenario above 5% fails the gate; do not silently discard a failed sample or widen
  the budget. A justified rerun retains both reports and explains environmental noise.
- The standalone `--self-test` checks median arithmetic, bounds, nonmutation, and fixture encoding;
  the wrapper runs it for each distribution before measuring. The sandbox Gradle source test pins
  both fixtures' tick, queue, golden, and kill counts.
- JSON records the harness SHA-256 and JVM path. The final evidence ledger must additionally record
  OS/JVM version, exact baseline commit, candidate diff/commit, command, and environmental load.

## Diagnostic extension after the first failed gate

`build/reports/eng036-runtime-benchmark-20260923-01.json` is retained unchanged: canonical +36.74%
and kill +26.68%, with both golden hashes correct. That result is a failure, not acceptance.
Its substantial inter-fork and within-fork spread warrants attribution before speculative runtime
changes: baseline canonical fork medians ranged from 5.12 ms to 11.89 ms per session.

The harness now additionally reports raw main-thread CPU time, whole-JVM CPU time, main-thread
allocated bytes, GC count/time deltas, wall-time minimum/P90/maximum, and OS/JVM metadata. These
counters are collected outside the original unchanged wall timer boundaries. They are diagnostic
only: the wall-time median and 5% comparison remain the sole performance gate; CPU figures do not
replace wall time and no GC-affected samples are discarded. Unsupported/reset counters are -1.
Windows CPU counters can be coarse, so interpret their aggregate values rather than individual
short samples. Whole-JVM CPU includes compiler/GC threads, whereas allocation counts cover only
the simulation thread. GC counters encompass the diagnostic boundary calls as well as the timed
work and do not prove which allocation triggered a collection.

No runtime optimization has been applied based solely on the first noisy failure. Coordinate a
quiet host with the main task before running the extended harness; preserve both reports.

## Controlled paired follow-up (preset before execution)

The second retained comparison, `eng036-runtime-benchmark-20260923-02.json`, failed canonical
(+17.33%) while kill passed (-0.58%). Allocations were not a regression: candidate canonical was
about 24 KiB lower per batch, and kill was only 256 bytes higher per eight sessions (one 32-byte
typed step result per session). Matched JFR recordings are retained as
`eng036-{baseline,candidate}-profile-20260923.jfr` with corresponding logs. They show late C2
compilation/recompilation; their sparse step-only samples do not identify a runtime hotspot cause.

Two additional, identical-code baseline runs using the same 32/41/8 preset are retained as
`eng036-baseline-aa-{first,second}-20260923.json`. Their apparent regressions were +108.56%
canonical and +130.55% kill despite identical median allocations. This demonstrates that the old
separate-process method cannot resolve the 5% budget reliably on this host; it does not convert
either failed comparison into a pass. The machine is an Intel i7-12700H hybrid-core Windows host.

The separately named `scripts/me-runtime-paired-benchmark.ps1` implements the agreed controlled
follow-up, without changing production code or the existing reports:

- Fixed, predeclared 128 warmup pairs, 101 measured pairs, 8 sessions per side, 35 ticks each.
- Each JVM contains two isolated URL classloaders with a platform-only parent. No engine/Kotlin
  class is shared or cast across sides; runtime class identities are explicitly checked distinct.
  Every fresh session and every warmup must produce the accepted golden, guarding contamination.
- Alternate A-then-B/B-then-A in each adjacent pair. First run A/A calibration from identical jars,
  then A/B comparison. Keep every sample and both results, even when calibration fails.
- Own JVM only: affinity `0xF`, normal priority, explicit 4 active processors, 512 MiB initial/max
  heap, G1. No global power-plan or unrelated-process change. Compile the Java harness before the
  measured JVM starts to exclude source-launch compiler activity from its warmup.
- Timed work is one reflective `step(35)` invocation per session on both sides. This small common
  invocation overhead is included and disclosed; loading, restoring, hashing, and diagnostics are
  excluded. Preserve raw wall/CPU/allocation/GC values. Reflection overhead is not subtracted.
- A/A must have absolute ratio-of-wall-medians delta <=5% for EACH scenario. A/B must have regression
  <=5% for EACH scenario; a failed A/A makes the whole result `inconclusive` regardless of A/B.
  Median paired ratios are also reported as diagnostics, not substituted for the declared gate.
- One prescribed calibration/comparison set; do not selectively rerun or discard observations to
  obtain acceptance. The wrapper requires a new output directory and keeps child stdout/stderr.

```powershell
powershell.exe -File scripts/me-runtime-paired-benchmark.ps1 -BaselineRoot 'D:/Pet/MyEngine-mysd/build/eng036-baseline-20260923' -CandidateRoot 'D:/Pet/MyEngine-mysd' -ReportDirectory '<new retained report directory>'
```

## Pre-helper controlled result — 2026-09-23

`build/reports/eng036-paired-20260923-01` retains identity artifacts and a passing source self-test;
no timing ran there because Windows PowerShell exposed a null `Start-Process.ExitCode` after the
short self-test child exited. The wrapper now retains `Process.Handle` before `WaitForExit`.
This startup repair did not change the measured code, samples, or thresholds.

The single prescribed pre-helper measured set is retained at `build/reports/eng036-paired-20260923-02/report.json`:

| Scenario | A/A delta | A/B delta | Median paired A/B ratio | Result |
|---|---:|---:|---:|---|
| Canonical | -0.554% | +4.580% | 0.997774 | Pass |
| Kill | +0.434% | +8.500% | 1.062209 | Fail |

A/A calibration passed both absolute 5% bounds. The whole gate is nevertheless **fail**, because
kill exceeds the unchanged ratio-of-wall-medians budget. Every warmup/measured hash and classloader
isolation check passed. The report directory retains all raw observations, child stderr/stdout,
loaded distribution/content SHA-256 manifest, candidate tracked diff, baseline commit declaration,
candidate HEAD/untracked hashes, and both benchmark sources/wrappers' SHA-256 identities.

No selective timing rerun followed this failure. A source/bytecode investigation found the extracted
`advanceOneTick` callback is 433 bytecodes versus this JBR's 325-byte `FreqInlineSize` default; JFR
records separate late C2 compilations. This is an inlining hypothesis, not an established cause.
After Android entered instrumentation (all Gradle inputs frozen), a minimal sandbox-only source
refactor moved existing tower/reward handling and incident selection into private helpers. Tick
validation, command/system order, terminal guard, and seed 17 remain unchanged; there is no generic
loop bypass and no `engine-runtime` edit. Independent semantic review confirmed the unchanged
command/production/spawn/tower/reward/enemy/terminal/incident order. The compiled callback is now
239 bytecodes. This bytecode change supports testing the inlining hypothesis but does not prove
that inlining caused the original regression.

Post-change correctness gates passed: full engine suite 184 tests/0 failures, Android debug assemble
and fresh installDist, then replay and v1-v7 save compatibility. The full sandbox result set was
restored after the focused save gate. Logs are retained as
`build/reports/eng036-after-helper-20260923-01.log`,
`eng036-after-helper-{replay,save}-20260923.log`, and
`eng036-after-helper-sandbox-full-20260923.log`.

## Post-helper controlled result and local acceptance — 2026-09-23

After the encompassing Android device run stopped, the root confirmed a quiet host (AVD stopped,
ADB device list empty, no active builds/tests). Exactly one prescribed unchanged A/A then A/B set
ran against the fresh helper-refactor distribution, retained at
`build/reports/eng036-paired-20260923-03/report.json`:

| Scenario | A/A delta | A/B baseline/candidate median batch ns | A/B delta | Median paired A/B ratio | Result |
|---|---:|---:|---:|---:|---|
| Canonical | -3.440% | 28,211,900 / 27,056,800 | -4.094% | 1.008766 | Pass |
| Kill | +0.596% | 29,434,500 / 28,327,500 | -3.761% | 0.971783 | Pass |

Both A/A absolute deltas satisfy <=5%, and both A/B regressions satisfy <=5%. All warmup/measured
goldens and classloader-isolation checks pass. These are the declared ratio-of-wall-medians gate
results, not a claim of a universally reproducible speedup; paired ratios and every raw sample
remain available. No retry-to-pass, sample removal, timer change or threshold relaxation occurred.

Identity anchors retained outside the timed regions:

- Exact baseline commit: `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`.
- Unchanged paired harness SHA-256:
  `0ccff0377029212d41421394dd8df14263aef1840a03c92ac805213844dec9d5`.
- Final loaded distribution/content manifest SHA-256:
  `7e289e056f855d64ab7c8c96a48b1ebbfdfa5698af7b6fbd738dee8673f2c3be`.
- Final report SHA-256:
  `fcba53a30178293fde7b7b726e6c6cffe89406a23e3f8a3191408d8efc71a1f8`.
- The same directory contains `source-identity.json`, `candidate-tracked.diff`,
  `distribution-manifest.json`, compiled harness classes and all child stdout/stderr. Subsequent
  documentation-only closeout edits do not alter the measured distribution; root must review the
  final scoped commit against these production/test/script identities before pinning it.

The independent root reviewer inspected the final report, hashes, generic runtime API and sandbox
helper behavior and accepted ENG-036 technical gates locally. Scoped commit/publication and MySD's
exact consumer pin are not performed by this closeout. Earlier failed reports remain evidence of
their tested versions/methodologies and are not retroactively changed to pass.

## Baseline preparation and original-method reproduction

The implementation-before-testing instruction was honored before any gate ran. For reproduction,
obtain a new isolated
directory containing the exact pre-extraction commit `30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb`
(for example, `git archive` into a new temporary directory; retain it rather than deleting it).
The other existing checkout at `D:/Pet/MyEngine` is at another commit and is not this baseline.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
powershell.exe -File scripts/me-runtime-benchmark.ps1 -BaselineRoot '<exact baseline directory>' -CandidateRoot 'D:/Pet/MyEngine-mysd'
```

Do not run Gradle tests or a live emulator simultaneously with timing. This gate establishes
headless runtime/session overhead only; it does not establish Android frame, allocation, or load
budgets. All earlier failures and the controlled pre-helper failed comparison remain retained as
described above. Only the final calibrated headless gate is accepted locally; publication and
consumer delivery are separate pending actions.
