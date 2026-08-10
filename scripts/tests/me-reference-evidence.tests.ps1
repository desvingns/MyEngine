[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$scriptPath = Join-Path $repoRoot "scripts\me-reference-evidence.ps1"
$validRoot = Join-Path $PSScriptRoot "fixtures\proc-015-valid"
$invalidRoot = Join-Path $PSScriptRoot "fixtures\proc-015-invalid"

function Invoke-ReferenceGate {
    param([string]$Mode, [string]$Root, [string]$GapId = "")
    $arguments = @("-NoProfile", "-File", $scriptPath, "-Mode", $Mode, "-EvidenceRoot", $Root, "-RepoRoot", $repoRoot)
    if (-not [string]::IsNullOrWhiteSpace($GapId)) { $arguments += @("-EngineGapId", $GapId) }
    $raw = & powershell.exe @arguments 2>&1 | Out-String
    $exitCode = $LASTEXITCODE
    $json = $raw.Trim() | ConvertFrom-Json -ErrorAction Stop
    return [pscustomobject]@{ exit_code = $exitCode; report = $json }
}

$checks = [ordered]@{}
$failed = [System.Collections.Generic.List[string]]::new()

$valid = Invoke-ReferenceGate -Mode gate1 -Root $validRoot
$checks.valid_gate1 = [ordered]@{ status = if ($valid.exit_code -eq 0 -and $valid.report.verdict -eq "pass") { "pass" } else { "fail" }; counts = $valid.report.counts }
if ($checks.valid_gate1.status -ne "pass") { [void]$failed.Add("valid_gate1") }

$invalid = Invoke-ReferenceGate -Mode validate -Root $invalidRoot
$checks.invalid_schema = [ordered]@{ status = if ($invalid.exit_code -ne 0 -and $invalid.report.verdict -eq "fail") { "pass" } else { "fail" }; errors = $invalid.report.errors }
if ($checks.invalid_schema.status -ne "pass") { [void]$failed.Add("invalid_schema") }

$bridge = Invoke-ReferenceGate -Mode bridge -Root $validRoot -GapId "PROC-015"
$bridgePass = $bridge.exit_code -eq 0 -and $bridge.report.verdict -eq "pass" -and $bridge.report.bridge.duplicate_card -eq $true -and $bridge.report.bridge.api_stability_scanned -eq $true -and $bridge.report.bridge.writes.Count -eq 0
$checks.bridge_dedup = [ordered]@{ status = if ($bridgePass) { "pass" } else { "fail" }; bridge = $bridge.report.bridge }
if ($checks.bridge_dedup.status -ne "pass") { [void]$failed.Add("bridge_dedup") }

$exitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
[ordered]@{
    agent = "me-reference-evidence-test"
    verdict = if ($exitCode -eq 0) { "pass" } else { "fail" }
    summary = if ($exitCode -eq 0) { "PROC-015 evidence import, Gate 1 rejection, and gap dedup contracts passed." } else { "PROC-015 evidence contracts failed: $($failed -join ', ')" }
    checks = $checks
} | ConvertTo-Json -Compress -Depth 10
exit $exitCode
