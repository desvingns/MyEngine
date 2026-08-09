# Android release lane

Status: PROC-014 accepted (2026-08-09)

The Android shell is the shipping target. Release artifacts are built by the Windows lane:

```powershell
powershell.exe -File scripts/me-android-release.ps1 `
  -KeystorePropertiesPath keystore.properties `
  -ApplicationId dev.myengine.android `
  -VersionCode 1 `
  -VersionName 0.0.1
```

## Signing

`keystore.properties` is intentionally untracked and is ignored by Git. It must contain only
local signing configuration:

```properties
storeFile=C:/secure/myengine-release.jks
storePassword=...
keyAlias=myengine
keyPassword=...
```

The path may be passed with `-Pmyengine.keystore.properties=...` or
`MYENGINE_KEYSTORE_PROPERTIES`. Passwords are never printed by the Gradle validation task or the
release report. `bundleRelease` and `assembleRelease` fail before packaging when the file or any
required key is missing.

## Version and application ID policy

The release lane accepts `-P` or environment overrides:

| Value | Gradle property | Environment variable | Default |
|---|---|---|---|
| Version code | `myengine.versionCode` | `MYENGINE_VERSION_CODE` | `1` |
| Version name | `myengine.versionName` | `MYENGINE_VERSION_NAME` | `0.0.1` |
| Application ID | `myengine.applicationId` | `MYENGINE_APPLICATION_ID` | `dev.myengine.android` |

Each shipped game owns one application ID under `dev.myengine.<game>`, with a stable suffix for
the lifetime of that listing. A game release increments `versionCode` monotonically and changes
the human-readable `versionName` according to that game's release policy. The Android module's
default remains the sandbox shell until a game-specific application is introduced.

## R8 and content smoke

The release build enables R8 and resource shrinking. `android/proguard-rules.pro` keeps the Android
shell, simulation, content, render, and sandbox entry-point packages stable. The lane opens the
AAB as a ZIP and requires `base/assets/sandbox/manifest.properties` and `maps.json`; it then
installs the signed release APK, cold-starts the activity with `am start -W`, records
`ThisTime`/`TotalTime`, and requires the running `SurfaceView`. This proves the minified release
variant can start and reach the packaged content shell.

The report schema is `proc-014-android-release-v1`. It records the AAB byte size, application
identity/version, cold-start samples and min/median/max milliseconds, plus the content-load smoke
result. Reports default to the ignored `reports/android-release/` directory. Copy the measured
values into the release handoff and use them as release/performance telemetry; the existing
`me-record-run.ps1` event can carry the release metrics in its `Note` field without changing the
simulation benchmark schema.

Build-only inspection is available with `-PreflightOnly -SkipDevice`; it does not claim a cold-start
metric. Missing SDK/emulator capability is typed `blocked`, not a release pass.
