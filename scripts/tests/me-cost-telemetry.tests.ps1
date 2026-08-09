$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$recordSource = Get-Content -Raw (Join-Path $repoRoot "scripts\me-record-run.ps1")
$retroSource = Get-Content -Raw (Join-Path $repoRoot "scripts\me-retro.ps1")
$fixturePath = Join-Path $repoRoot "scripts\fixtures\telemetry-cost.jsonl"
$failed = @()
$cases = @()

$recordContractOk = $recordSource.Contains('[string]$TokenUsage = ""') -and
    $recordSource.Contains('[long]$EstimatedTokens = -1') -and
    $recordSource.Contains('source = $tokenSource') -and
    $recordSource.Contains('by_agent = $tokenUsageByAgent') -and
    $recordSource.Contains('chars_per_4')
if (-not $recordContractOk) { $failed += "record-contract" }
$cases += [ordered]@{ name = "record-contract"; verdict = if ($recordContractOk) { "pass" } else { "fail" }; exit_code = if ($recordContractOk) { 0 } else { 1 } }

$retroContractOk = $retroSource.Contains('$tokensByWorkflow') -and
    $retroSource.Contains('$tokensByAgent') -and
    $retroSource.Contains('Cost-driven proposal') -and
    $retroSource.Contains('estimated_tokens_by_workflow') -and
    $retroSource.Contains('estimated_tokens_by_agent')
if (-not $retroContractOk) { $failed += "retro-contract" }
$cases += [ordered]@{ name = "retro-contract"; verdict = if ($retroContractOk) { "pass" } else { "fail" }; exit_code = if ($retroContractOk) { 0 } else { 1 } }

$raw = @(& powershell.exe -NoProfile -File (Join-Path $repoRoot "scripts\me-retro.ps1") `
    -TelemetryPath "scripts\fixtures\telemetry-cost.jsonl" -NoWrite 2>&1)
$exitCode = $LASTEXITCODE
$outputLines = @($raw | ForEach-Object { $_.ToString() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
$parsed = $null
if ($outputLines.Count -eq 1) {
    try { $parsed = $outputLines[0] | ConvertFrom-Json -ErrorAction Stop } catch { }
}
$workflowTokens = if ($null -ne $parsed -and $null -ne $parsed.estimated_tokens_by_workflow) {
    [int]$parsed.estimated_tokens_by_workflow.feature
} else { -1 }
$agentTokens = if ($null -ne $parsed -and $null -ne $parsed.estimated_tokens_by_agent) {
    [int]$parsed.estimated_tokens_by_agent.'me-runner'
} else { -1 }
$retroRunOk = $exitCode -eq 0 -and $outputLines.Count -eq 1 -and $null -ne $parsed -and
    $parsed.status -eq "retro_written" -and $parsed.token_events -eq 3 -and
    $parsed.estimated_tokens -eq 4000 -and $workflowTokens -eq 2000 -and $agentTokens -eq 2000 -and
    $parsed.cost_proposal -match "Cost-driven proposal"
if (-not $retroRunOk) { $failed += "retro-cost-aggregation" }
$cases += [ordered]@{ name = "retro-cost-aggregation"; verdict = if ($retroRunOk) { "pass" } else { "fail" }; exit_code = $exitCode }

$finalExitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
[ordered]@{
    agent = "me-cost-telemetry-test"
    verdict = if ($finalExitCode -eq 0) { "pass" } else { "fail" }
    summary = if ($finalExitCode -eq 0) { "Telemetry token estimates, per-workflow/per-agent retro aggregation, and cost-driven proposal output are covered." } else { "Cost telemetry contract failures: $($failed -join ', ')." }
    cases = @($cases)
} | ConvertTo-Json -Compress -Depth 8
exit $finalExitCode
