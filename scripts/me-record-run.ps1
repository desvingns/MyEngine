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
