$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    & .\gradlew.bat --quiet :games:sandbox:test `
        --tests "dev.myengine.games.sandbox.SandboxGoalFieldTest.midRerouteSaveRestoreReachesTheSameStableHash" `
        --tests "dev.myengine.games.sandbox.SandboxSessionLifecycleTest.legacyV6EnemyRouteIsDiscardedAndGoalFieldMigrationIsReplayStable" `
        --tests "dev.myengine.games.sandbox.SandboxSessionLifecycleTest.futureSaveVersionIsRejected" `
        --tests "dev.myengine.games.sandbox.SandboxSaveMigrationMatrixTest" `
        --tests "dev.myengine.games.sandbox.SandboxMultiSpawnTest"
    $exitCode = $LASTEXITCODE
    $status = if ($exitCode -eq 0) { "pass" } else { "fail" }
    @{ status = $status; matrix = $status; command = "games:sandbox:test mid-reroute, legacy-v6-route, future-version, migration-matrix, multi-spawn"; exit_code = $exitCode } | ConvertTo-Json -Compress
} finally {
    Pop-Location
}
