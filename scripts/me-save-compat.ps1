$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    & .\gradlew.bat --quiet :games:sandbox:test --tests "dev.myengine.games.sandbox.SandboxVerticalSliceTest.saveLoadRoundtripPreservesHash"
    $status = if ($LASTEXITCODE -eq 0) { "pass" } else { "fail" }
    @{ status = $status; command = "games:sandbox:test saveLoadRoundtripPreservesHash"; exit_code = $LASTEXITCODE } | ConvertTo-Json -Compress
} finally {
    Pop-Location
}
