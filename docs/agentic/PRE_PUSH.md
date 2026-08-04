# MyEngine pre-push lane

The repository provides a local Git hook for deterministic gates. Enable it once per clone:

```powershell
git config core.hooksPath .githooks
```

The hook delegates to `scripts/me-pre-push.ps1`. The lane runs, in order:

```text
powershell.exe -File scripts/me-schema-docs-drift.ps1
powershell.exe -File scripts/me-schema-docs-drift-test.ps1
.\gradlew.bat --quiet test
powershell.exe -File scripts/me-content-validate.ps1
powershell.exe -File scripts/me-sim-replay.ps1
powershell.exe -File scripts/me-save-compat.ps1
powershell.exe -File scripts/me-benchmark.ps1
```

Run the aggregate directly on Windows with:

```powershell
powershell.exe -NoProfile -File scripts\me-pre-push.ps1
```

The script uses the Android Studio JBR and local Android SDK as defaults when `JAVA_HOME` or
`ANDROID_HOME` is unset. Each child check must emit one compact JSON line; the aggregate emits one
final JSON line containing every check, command, exit code, status, and parsed report. Missing or
malformed JSON, a non-zero exit code, or a non-pass report fails the lane and exits 1, blocking the
push. All checks must pass for exit 0.

The hook is process tooling only; it does not change simulation, Android runtime, content saves,
or replay state. Close-out verification may additionally run `gradlew projects`,
`:android:assembleDebug`, `me-selfcheck`, and `git diff --check`.
