$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    & .\gradlew.bat --quiet :engine-devtools:run --args "replay-inspect"
    if ($LASTEXITCODE -ne 0) {
        @{ status = "fail"; command = "engine-devtools:run replay-inspect"; exit_code = $LASTEXITCODE } | ConvertTo-Json -Compress
    }
} finally {
    Pop-Location
}
