param(
    [string]$Workflow = "manual",
    [string]$Phase = "unknown",
    [string]$Agent = "codex",
    [string]$Model = "unknown",
    [ValidateSet("pass", "fail", "partial", "blocked", "needs_human")]
    [string]$Verdict = "pass",
    [int]$Retries = 0,
    [string]$ChangedFiles = "",
    [string]$Tests = "not_run",
    [string]$ContentValidate = "not_run",
    [string]$Replay = "not_run",
    [string]$SaveCompat = "not_run",
    [string]$Benchmark = "not_run",
    [double]$SimMs = -1,
    [double]$FrameMs = -1,
    [double]$DurationMin = 0,
    [string]$TokenUsage = "",
    [long]$EstimatedTokens = -1,
    [int]$MalformedJsonCount = 0,
    [string]$GateFailures = "",
    [string]$AttributedAgent = "",
    [string]$Note = "",
    [string]$FailureCluster = ""
)

$root = Split-Path -Parent $PSScriptRoot
$runs = Join-Path $root ".ai\runs"
New-Item -ItemType Directory -Force -Path $runs | Out-Null
$path = Join-Path $runs "telemetry.jsonl"

$simMetric = if ($SimMs -ge 0) { $SimMs } else { $null }
$frameMetric = if ($FrameMs -ge 0) { $FrameMs } else { $null }

$tokenUsageByAgent = [ordered]@{}
if (-not [string]::IsNullOrWhiteSpace($TokenUsage)) {
    foreach ($entry in ($TokenUsage -split "," | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })) {
        $parts = $entry -split "=", 2
        if ($parts.Count -ne 2 -or [string]::IsNullOrWhiteSpace($parts[0])) {
            throw "TokenUsage entries must use agent=non-negative-integer format: '$entry'"
        }
        $tokenValue = 0L
        if (-not [long]::TryParse($parts[1].Trim(), [ref]$tokenValue) -or $tokenValue -lt 0) {
            throw "TokenUsage value must be a non-negative integer: '$entry'"
        }
        $tokenUsageByAgent[$parts[0].Trim()] = $tokenValue
    }
}

$tokenSource = "explicit"
if ($tokenUsageByAgent.Count -eq 0) {
    $estimatePayload = [ordered]@{
        workflow = $Workflow
        phase = $Phase
        agent = $Agent
        model = $Model
        verdict = $Verdict
        changed_files = $ChangedFiles
        note = $Note
    } | ConvertTo-Json -Compress -Depth 6
    $estimated = if ($EstimatedTokens -ge 0) {
        $tokenSource = "orchestrator"
        $EstimatedTokens
    } else {
        $tokenSource = "chars_per_4"
        [long][math]::Ceiling($estimatePayload.Length / 4.0)
    }
    $tokenUsageByAgent[$Agent] = $estimated
}
$estimatedTokenTotal = [long](($tokenUsageByAgent.Values | Measure-Object -Sum).Sum)

$event = [ordered]@{
    run_id = [guid]::NewGuid().ToString()
    timestamp = (Get-Date).ToUniversalTime().ToString("o")
    workflow = $Workflow
    phase = $Phase
    agent = $Agent
    model = $Model
    verdict = $Verdict
    retries = $Retries
    changed_files = @($ChangedFiles.Split(",") | Where-Object { $_.Trim().Length -gt 0 } | ForEach-Object { $_.Trim() })
    metrics = [ordered]@{
        tests = $Tests
        content_validate = $ContentValidate
        replay = $Replay
        save_compat = $SaveCompat
        benchmark = $Benchmark
        frame_ms = $frameMetric
        sim_ms = $simMetric
    }
    token_usage = [ordered]@{
        source = $tokenSource
        estimated_total = $estimatedTokenTotal
        by_agent = $tokenUsageByAgent
    }
    duration_min = $DurationMin
    malformed_json_count = $MalformedJsonCount
    gate_failures = @($GateFailures.Split(",") | Where-Object { $_.Trim().Length -gt 0 } | ForEach-Object { $_.Trim() })
    attributed_agent = $AttributedAgent
    note = $Note
    failure_cluster = $FailureCluster
}

$line = ($event | ConvertTo-Json -Compress -Depth 6)
Add-Content -Path $path -Value $line
$count = (Get-Content -Path $path).Count
$retroDue = (($count % 5) -eq 0)
$result = [ordered]@{
    status = "recorded"
    path = ".ai/runs/telemetry.jsonl"
    retro_due = $retroDue
    reflect_required = ($retroDue -or ($Verdict -ne "pass"))
    events = $count
}
$result | ConvertTo-Json -Compress
