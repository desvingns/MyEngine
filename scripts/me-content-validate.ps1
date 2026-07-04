$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
$code = 0
$result = $null
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $raw = & .\gradlew.bat --quiet :engine-devtools:run --args "content-report-all" 2>$null
    $gExit = $LASTEXITCODE
    $line = $raw | Where-Object { $_ -is [string] -and $_.TrimStart().StartsWith('{"valid"') } | Select-Object -Last 1
    if ($gExit -ne 0 -or -not $line) {
        $code = if ($gExit -ne 0) { $gExit } else { 1 }
        $result = [ordered]@{ status = "fail"; command = "engine-devtools:run content-report-all"; exit_code = $code; notes = "devtool did not emit aggregate JSON" }
    } else {
        $parsed = $line | ConvertFrom-Json
        if ($parsed.valid) {
            $result = [ordered]@{ status = "pass"; command = "engine-devtools:run content-report-all"; exit_code = 0; notes = "validated $($parsed.pack_count) pack(s)" }
        } else {
            $code = 1
            $bad = ($parsed.packs | Where-Object { -not $_.valid } | ForEach-Object { $_.root }) -join ", "
            $result = [ordered]@{ status = "fail"; command = "engine-devtools:run content-report-all"; exit_code = 1; notes = "invalid packs: $bad" }
        }
    }
} finally {
    Pop-Location
}
$result | ConvertTo-Json -Compress
exit $code
