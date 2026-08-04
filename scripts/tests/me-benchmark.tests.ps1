$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$benchmarkScript = Join-Path $repoRoot "scripts\me-benchmark.ps1"
$budgetPath = Join-Path $repoRoot "config\performance-budgets.v1.json"
$fixtureRoot = Join-Path $repoRoot "scripts\fixtures\performance-budgets"

function Invoke-BenchmarkFixture {
    param(
        [string]$ReportPath,
        [string]$Budget = $budgetPath
    )

    $rawLines = @(& powershell.exe -NoProfile -File $benchmarkScript `
        -ReportPath $ReportPath `
        -BudgetPath $Budget 2>&1)
    $exitCode = $LASTEXITCODE
    $outputLines = @($rawLines | ForEach-Object { $_.ToString() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $parsed = $null
    if ($outputLines.Count -eq 1) {
        try { $parsed = $outputLines[0] | ConvertFrom-Json -ErrorAction Stop } catch { }
    }
    [pscustomobject]@{
        exit_code = $exitCode
        output_lines = $outputLines
        parsed = $parsed
    }
}

$failed = @()
$cases = @()

$pass = Invoke-BenchmarkFixture (Join-Path $fixtureRoot "pass.json")
$passFrame = if ($null -ne $pass.parsed) { @($pass.parsed.checks | Where-Object { $_.metric -eq "frame_ms" }) | Select-Object -First 1 } else { $null }
$passOk = $pass.exit_code -eq 0 -and
    $pass.output_lines.Count -eq 1 -and
    $null -ne $pass.parsed -and
    $pass.parsed.verdict -eq "pass" -and
    $pass.parsed.metrics.sim_ms -eq 700 -and
    $passFrame.status -eq "not_measured"
if (-not $passOk) { $failed += "pass" }
$cases += [ordered]@{ name = "pass"; verdict = if ($passOk) { "pass" } else { "fail" }; exit_code = $pass.exit_code }

$fail = Invoke-BenchmarkFixture (Join-Path $fixtureRoot "fail.json")
$canonicalFail = if ($null -ne $fail.parsed) { @($fail.parsed.checks | Where-Object { $_.scenario -eq "canonical" }) | Select-Object -First 1 } else { $null }
$failOk = $fail.exit_code -eq 1 -and
    $fail.output_lines.Count -eq 1 -and
    $null -ne $fail.parsed -and
    $fail.parsed.verdict -eq "fail" -and
    $canonicalFail.status -eq "fail" -and
    $canonicalFail.delta -gt 0
if (-not $failOk) { $failed += "budget-failure" }
$cases += [ordered]@{ name = "budget-failure"; verdict = if ($failOk) { "pass" } else { "fail" }; exit_code = $fail.exit_code }

$frameFail = Invoke-BenchmarkFixture (Join-Path $fixtureRoot "frame-fail.json")
$frameCheck = if ($null -ne $frameFail.parsed) { @($frameFail.parsed.checks | Where-Object { $_.metric -eq "frame_ms" }) | Select-Object -First 1 } else { $null }
$frameFailOk = $frameFail.exit_code -eq 1 -and
    $frameFail.output_lines.Count -eq 1 -and
    $null -ne $frameFail.parsed -and
    $frameFail.parsed.verdict -eq "fail" -and
    $frameCheck.status -eq "fail"
if (-not $frameFailOk) { $failed += "frame-budget-failure" }
$cases += [ordered]@{ name = "frame-budget-failure"; verdict = if ($frameFailOk) { "pass" } else { "fail" }; exit_code = $frameFail.exit_code }

$missing = Invoke-BenchmarkFixture (Join-Path $fixtureRoot "missing-metric.json")
$missingOk = $missing.exit_code -eq 1 -and
    $missing.output_lines.Count -eq 1 -and
    $null -ne $missing.parsed -and
    $missing.parsed.verdict -eq "fail" -and
    $missing.parsed.error -match "Missing numeric benchmark metric"
if (-not $missingOk) { $failed += "missing-metric" }
$cases += [ordered]@{ name = "missing-metric"; verdict = if ($missingOk) { "pass" } else { "fail" }; exit_code = $missing.exit_code }

$missingReport = Invoke-BenchmarkFixture (Join-Path $fixtureRoot "does-not-exist.json")
$missingReportOk = $missingReport.exit_code -eq 1 -and $missingReport.output_lines.Count -eq 1 -and
    $null -ne $missingReport.parsed -and $missingReport.parsed.error -match "report not found"
if (-not $missingReportOk) { $failed += "missing-report" }
$cases += [ordered]@{ name = "missing-report"; verdict = if ($missingReportOk) { "pass" } else { "fail" }; exit_code = $missingReport.exit_code }

$missingBudget = Invoke-BenchmarkFixture (Join-Path $fixtureRoot "pass.json") (Join-Path $fixtureRoot "does-not-exist.json")
$missingBudgetOk = $missingBudget.exit_code -eq 1 -and $missingBudget.output_lines.Count -eq 1 -and
    $null -ne $missingBudget.parsed -and $missingBudget.parsed.error -match "Budget config not found"
if (-not $missingBudgetOk) { $failed += "missing-budget" }
$cases += [ordered]@{ name = "missing-budget"; verdict = if ($missingBudgetOk) { "pass" } else { "fail" }; exit_code = $missingBudget.exit_code }

$recordSource = Get-Content -Raw (Join-Path $repoRoot "scripts\me-record-run.ps1")
$telemetryArgsOk = $recordSource.Contains('[double]$SimMs = -1') -and
    $recordSource.Contains('[double]$FrameMs = -1') -and
    $recordSource.Contains('sim_ms = $simMetric') -and
    $recordSource.Contains('frame_ms = $frameMetric')
if (-not $telemetryArgsOk) { $failed += "telemetry-args" }
$cases += [ordered]@{ name = "telemetry-args"; verdict = if ($telemetryArgsOk) { "pass" } else { "fail" }; exit_code = if ($telemetryArgsOk) { 0 } else { 1 } }

$exitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
[ordered]@{
    agent = "me-benchmark-test"
    verdict = if ($exitCode -eq 0) { "pass" } else { "fail" }
    summary = if ($exitCode -eq 0) { "Performance budget pass/fail, missing metrics, missing inputs, frame not-measured handling, and telemetry parameters are covered." } else { "Benchmark contract failures: $($failed -join ', ')." }
    cases = @($cases)
} | ConvertTo-Json -Compress -Depth 8
exit $exitCode
