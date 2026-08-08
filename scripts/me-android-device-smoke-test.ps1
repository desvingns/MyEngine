$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $root "scripts\me-android-device-smoke.ps1"

function Invoke-PreflightContract {
    param([string]$SdkPath)

    $raw = & powershell.exe -NoProfile -File $scriptPath -SdkRoot $SdkPath -PreflightOnly 2>&1 | Out-String
    $exitCode = $LASTEXITCODE
    $lines = @($raw -split "`r?`n" | Where-Object { $_.Trim() })
    $jsonLines = @()
    foreach ($line in $lines) {
        try {
            $jsonLines += [ordered]@{
                raw = $line.Trim()
                value = ($line.Trim() | ConvertFrom-Json -ErrorAction Stop)
            }
        } catch { }
    }
    return [ordered]@{
        exit_code = $exitCode
        lines = $lines
        json_lines = $jsonLines
    }
}

$missingSdk = Join-Path $root "__proc012_missing_sdk_contract__"
$firstRun = Invoke-PreflightContract $missingSdk
$secondRun = Invoke-PreflightContract $missingSdk
$report = if ($firstRun.json_lines.Count -eq 1) { $firstRun.json_lines[0].value } else { $null }

$failures = @()
if ($firstRun.lines.Count -ne 1) { $failures += "expected_one_output_line" }
if ($firstRun.json_lines.Count -ne 1 -or $secondRun.json_lines.Count -ne 1) { $failures += "expected_one_json_result_per_run" }
if ($null -eq $report) { $failures += "no_json_report" }
if ($firstRun.exit_code -ne 2 -or $secondRun.exit_code -ne 2) { $failures += "expected_blocked_exit_code_2" }
if ($null -ne $report -and $report.verdict -ne "blocked") { $failures += "expected_blocked_verdict" }
if ($null -ne $report -and $report.reason -ne "preflight_failed") { $failures += "expected_preflight_reason" }
if ($null -ne $report -and [string]::IsNullOrWhiteSpace([string]$report.fallback)) { $failures += "missing_fallback" }
if ($null -ne $report -and $report.status -ne "blocked") { $failures += "expected_blocked_status" }
if ($null -ne $report -and $report.exit_code -ne 2) { $failures += "expected_result_exit_code_2" }
if ($null -ne $report -and $report.preflight.pass) { $failures += "expected_preflight_failure" }
if ($firstRun.json_lines.Count -eq 1 -and $secondRun.json_lines.Count -eq 1) {
    $firstJson = $firstRun.json_lines[0].value | ConvertTo-Json -Compress -Depth 12
    $secondJson = $secondRun.json_lines[0].value | ConvertTo-Json -Compress -Depth 12
    if ($firstJson -ne $secondJson) { $failures += "nondeterministic_preflight_result" }
}

$manifestPath = Join-Path $root "android\src\androidTest\AndroidManifest.xml"
$sourcePath = Join-Path $root "android\src\androidTest\kotlin\dev\myengine\android\AndroidSmokeInstrumentationTest.kt"
$androidGradlePath = Join-Path $root "android\build.gradle.kts"
$manifest = if (Test-Path -LiteralPath $manifestPath -PathType Leaf) { Get-Content -Raw -LiteralPath $manifestPath } else { "" }
$source = if (Test-Path -LiteralPath $sourcePath -PathType Leaf) { Get-Content -Raw -LiteralPath $sourcePath } else { "" }
$androidGradle = if (Test-Path -LiteralPath $androidGradlePath -PathType Leaf) { Get-Content -Raw -LiteralPath $androidGradlePath } else { "" }
$testAnnotationCount = [regex]::Matches($source, '(?m)^\s*@Test\b').Count
if ($manifest -notmatch 'android:name="androidx\.test\.runner\.AndroidJUnitRunner"') { $failures += "missing_instrumentation_runner" }
if ($manifest -notmatch 'android:targetPackage="dev\.myengine\.android"') { $failures += "missing_instrumentation_target_package" }
if ($source -notmatch '(?m)^\s*package\s+dev\.myengine\.android\s*$') { $failures += "missing_instrumentation_source_package" }
if ($source -notmatch 'class\s+AndroidSmokeInstrumentationTest') { $failures += "missing_instrumentation_test_class" }
if ($testAnnotationCount -ne 1) { $failures += "expected_one_instrumentation_test_annotation" }
if ($androidGradle -notmatch 'androidTestImplementation\(libs\.androidx\.test\.runner\)') { $failures += "missing_androidx_test_runner_dependency" }
if ($androidGradle -notmatch 'androidTestImplementation\(libs\.junit4\)') { $failures += "missing_junit4_dependency" }

$result = [ordered]@{
    agent = "me-android-device-smoke-test"
    verdict = if ($failures.Count -eq 0) { "pass" } else { "fail" }
    summary = if ($failures.Count -eq 0) { "PROC-012 deterministic preflight and instrumentation discovery contracts passed." } else { "PROC-012 contract failed." }
    failures = $failures
    child_exit_code = $firstRun.exit_code
    child_report = $report
}
$result | ConvertTo-Json -Compress -Depth 12
exit $(if ($failures.Count -eq 0) { 0 } else { 1 })
