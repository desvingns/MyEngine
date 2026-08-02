$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
$code = 0
$result = $null
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    function Invoke-ContentReport([string]$arguments) {
        $raw = & .\gradlew.bat --quiet :engine-devtools:run --args $arguments 2>$null
        $exitCode = $LASTEXITCODE
        $line = $raw |
            Where-Object { $_ -is [string] -and $_.TrimStart().StartsWith('{') -and $_.TrimStart().Contains('"valid":') } |
            Select-Object -Last 1
        if ($exitCode -ne 0 -or -not $line) {
            return [pscustomobject]@{ exit_code = if ($exitCode -ne 0) { $exitCode } else { 1 }; report = $null }
        }
        try {
            return [pscustomobject]@{ exit_code = 0; report = ($line | ConvertFrom-Json) }
        } catch {
            return [pscustomobject]@{ exit_code = 1; report = $null }
        }
    }

    $fixture = "games/sandbox/src/test/resources/content-fixtures/multi-spawn"
    $aggregateCheck = Invoke-ContentReport "content-report-all"
    $fixtureCheck = Invoke-ContentReport "content-report $fixture"
    $invalid = @()
    if ($aggregateCheck.exit_code -ne 0 -or $null -eq $aggregateCheck.report -or -not $aggregateCheck.report.valid) {
        $invalid += "aggregate packs"
    }
    if ($fixtureCheck.exit_code -ne 0 -or $null -eq $fixtureCheck.report -or -not $fixtureCheck.report.valid) {
        $invalid += $fixture
    }
    if ($invalid.Count -eq 0) {
        $result = [ordered]@{
            status = "pass"
            command = "engine-devtools:run content-report-all + content-report $fixture"
            exit_code = 0
            notes = "validated $($aggregateCheck.report.pack_count) pack(s) and fixture $fixture"
        }
    } else {
        $code = 1
        $result = [ordered]@{
            status = "fail"
            command = "engine-devtools:run content-report-all + content-report $fixture"
            exit_code = 1
            notes = "invalid content checks: $($invalid -join ', ')"
        }
    }
} finally {
    Pop-Location
}
$result | ConvertTo-Json -Compress
exit $code
