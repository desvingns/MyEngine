$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
$checks = [ordered]@{}
$failed = @()

function Get-LastJsonReport([string]$Raw) {
    foreach ($line in ($Raw -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 8)) {
        try {
            return $line.Trim() | ConvertFrom-Json -ErrorAction Stop
        } catch { }
    }
    return $null
}

function Add-ScriptCheck([string]$Name, [string]$Command, [scriptblock]$Action) {
    $raw = & $Action 2>$null | Out-String
    $childExitCode = $LASTEXITCODE
    $report = Get-LastJsonReport $raw
    if ($null -eq $report) {
        $report = [ordered]@{ verdict = 'fail'; summary = "$Name did not return one JSON result" }
        $childExitCode = 1
    }
    $reportVerdict = if ($report.PSObject.Properties.Name -contains 'verdict') {
        [string]$report.verdict
    } elseif ($report.PSObject.Properties.Name -contains 'status') {
        [string]$report.status
    } else {
        ''
    }
    $status = if ($childExitCode -eq 0 -and $reportVerdict -eq 'pass') { 'pass' } else { 'fail' }
    $checks[$Name] = [ordered]@{
        status = $status
        command = $Command
        exit_code = $childExitCode
        report = $report
    }
    if ($status -ne 'pass') { $script:failed += $Name }
}

function Add-CommandCheck([string]$Name, [string]$Command, [scriptblock]$Action) {
    $raw = & $Action 2>$null | Out-String
    $childExitCode = $LASTEXITCODE
    $status = if ($childExitCode -eq 0) { 'pass' } else { 'fail' }
    $checks[$Name] = [ordered]@{
        status = $status
        command = $Command
        exit_code = $childExitCode
    }
    if ($status -ne 'pass') { $script:failed += $Name }
}

try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }

    Add-ScriptCheck 'schema_docs_drift' 'powershell.exe -File scripts/me-schema-docs-drift.ps1' {
        & powershell.exe -NoProfile -File (Join-Path $root 'scripts/me-schema-docs-drift.ps1')
    }
    Add-ScriptCheck 'schema_docs_fixtures' 'powershell.exe -File scripts/me-schema-docs-drift-test.ps1' {
        & powershell.exe -NoProfile -File (Join-Path $root 'scripts/me-schema-docs-drift-test.ps1')
    }
    Add-CommandCheck 'tests' '.\gradlew.bat --quiet test' {
        & .\gradlew.bat --quiet test
    }
    Add-ScriptCheck 'content_validate' 'powershell.exe -File scripts/me-content-validate.ps1' {
        & powershell.exe -NoProfile -File (Join-Path $root 'scripts/me-content-validate.ps1')
    }
    Add-CommandCheck 'replay' 'powershell.exe -File scripts/me-sim-replay.ps1' {
        & powershell.exe -NoProfile -File (Join-Path $root 'scripts/me-sim-replay.ps1')
    }
    Add-ScriptCheck 'save_compat' 'powershell.exe -File scripts/me-save-compat.ps1' {
        & powershell.exe -NoProfile -File (Join-Path $root 'scripts/me-save-compat.ps1')
    }

    $exitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
    $result = [ordered]@{
        agent = 'me-pre-push'
        verdict = if ($exitCode -eq 0) { 'pass' } else { 'fail' }
        summary = if ($exitCode -eq 0) { 'DX-005 and PROC-006 pre-push gates passed.' } else { "Pre-push gates failed: $($failed -join ', ')." }
        checks = $checks
    }
    $result | ConvertTo-Json -Compress -Depth 10
    exit $exitCode
} finally {
    Pop-Location
}
