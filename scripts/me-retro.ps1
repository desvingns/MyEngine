param(
    [string]$TelemetryPath = ".ai\runs\telemetry.jsonl",
    [string]$OutputPath = "",
    [switch]$NoWrite
)

$root = Split-Path -Parent $PSScriptRoot
$path = Join-Path $root $TelemetryPath
$retroDir = Join-Path $root ".ai\retro"
New-Item -ItemType Directory -Force -Path $retroDir | Out-Null
$retroPath = if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Join-Path $retroDir ("retro-" + (Get-Date -Format "yyyy-MM-dd") + ".md")
} else {
    $OutputPath
}

if (-not (Test-Path $path)) {
    $empty = [ordered]@{ status = "no_telemetry"; events = 0; retro = $retroPath }
    $empty | ConvertTo-Json -Compress
    exit 0
}

$events = Get-Content -Path $path | Where-Object { $_.Trim().Length -gt 0 } | ForEach-Object { $_ | ConvertFrom-Json }
$total = @($events).Count
$verdicts = @{}
$clusters = @{}
$attributions = @{}
$gateFailures = @{}
$tokensByWorkflow = @{}
$tokensByAgent = @{}
$totalRetries = 0
$totalMalformed = 0
$totalTokens = 0L
$tokenEvents = 0

function Add-Counter {
    param(
        [hashtable]$Table,
        [string]$Key,
        [long]$Amount
    )
    if (-not $Table.ContainsKey($Key)) { $Table[$Key] = 0L }
    $Table[$Key] = [long]$Table[$Key] + $Amount
}

foreach ($event in $events) {
    if (-not $verdicts.ContainsKey($event.verdict)) { $verdicts[$event.verdict] = 0 }
    $verdicts[$event.verdict] = 1 + $verdicts[$event.verdict]
    if ($event.failure_cluster -and $event.failure_cluster.Length -gt 0) {
        if (-not $clusters.ContainsKey($event.failure_cluster)) { $clusters[$event.failure_cluster] = 0 }
        $clusters[$event.failure_cluster] = 1 + $clusters[$event.failure_cluster]
    }
    if ($event.attributed_agent -and $event.attributed_agent.Length -gt 0) {
        if (-not $attributions.ContainsKey($event.attributed_agent)) { $attributions[$event.attributed_agent] = 0 }
        $attributions[$event.attributed_agent] = 1 + $attributions[$event.attributed_agent]
    }
    foreach ($gate in @($event.gate_failures)) {
        if ($gate -and $gate.Length -gt 0) {
            if (-not $gateFailures.ContainsKey($gate)) { $gateFailures[$gate] = 0 }
            $gateFailures[$gate] = 1 + $gateFailures[$gate]
        }
    }
    if ($event.retries) { $totalRetries += [int]$event.retries }
    if ($event.malformed_json_count) { $totalMalformed += [int]$event.malformed_json_count }

    if ($null -ne $event.token_usage) {
        $eventTokens = 0L
        $hasAgentTokenData = $false
        if ($null -ne $event.token_usage.by_agent) {
            foreach ($property in @($event.token_usage.by_agent.PSObject.Properties)) {
                $value = 0L
                if ([long]::TryParse([string]$property.Value, [ref]$value) -and $value -ge 0) {
                    Add-Counter $tokensByAgent ([string]$property.Name) $value
                    $eventTokens += $value
                    $hasAgentTokenData = $true
                }
            }
        }
        if (-not $hasAgentTokenData -and $null -ne $event.token_usage.estimated_total) {
            $fallbackValue = 0L
            if ([long]::TryParse([string]$event.token_usage.estimated_total, [ref]$fallbackValue) -and $fallbackValue -ge 0) {
                $fallbackAgent = if ($event.agent) { [string]$event.agent } else { "unknown" }
                Add-Counter $tokensByAgent $fallbackAgent $fallbackValue
                $eventTokens = $fallbackValue
            }
        }
        if ($eventTokens -gt 0 -or $hasAgentTokenData) {
            $workflow = if ($event.workflow) { [string]$event.workflow } else { "unknown" }
            Add-Counter $tokensByWorkflow $workflow $eventTokens
            $totalTokens += $eventTokens
            $tokenEvents++
        }
    }
}

function Format-Counters {
    param([hashtable]$Table)
    if ($Table.Count -eq 0) { return "none" }
    return (($Table.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", ")
}

$costProposal = if ($tokensByAgent.Count -gt 0) {
    $topAgent = $tokensByAgent.GetEnumerator() |
        Sort-Object -Property @{ Expression = "Value"; Descending = $true }, @{ Expression = "Name"; Descending = $false } |
        Select-Object -First 1
    "- Cost-driven proposal: review the model assignment for ``{0}`` because it has the highest estimated token volume ({1}); queue any model change through `/me --improve` and a human gate." -f $topAgent.Name, $topAgent.Value
} else {
    "- Cost-driven proposal: not available until a telemetry event carries token usage."
}

$tokenOutputByWorkflow = [ordered]@{}
$tokensByWorkflow.GetEnumerator() | Sort-Object Name | ForEach-Object { $tokenOutputByWorkflow[$_.Name] = $_.Value }
$tokenOutputByAgent = [ordered]@{}
$tokensByAgent.GetEnumerator() | Sort-Object Name | ForEach-Object { $tokenOutputByAgent[$_.Name] = $_.Value }

$lines = @(
    "# MyEngine Retro " + (Get-Date -Format "yyyy-MM-dd"),
    "",
    "- Events: $total",
    "- Verdicts: " + (($verdicts.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", "),
    "- Failure clusters: " + ($(if ($clusters.Count -eq 0) { "none" } else { ($clusters.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", " })),
    "- Agent attributions: " + ($(if ($attributions.Count -eq 0) { "none" } else { ($attributions.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", " })),
    "- Gate failures: " + ($(if ($gateFailures.Count -eq 0) { "none" } else { ($gateFailures.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", " })),
    "- Total retries: $totalRetries; malformed envelopes: $totalMalformed",
    "- Estimated tokens: " + ($(if ($tokenEvents -eq 0) { "not_available" } else { $totalTokens })),
    "- Estimated tokens by workflow: " + (Format-Counters $tokensByWorkflow),
    "- Estimated tokens by agent: " + (Format-Counters $tokensByAgent),
    "",
    "## Candidate Improvements",
    "",
    "- Review any repeated failure cluster before changing pipeline instructions.",
    "- Target the agent prompt with the highest attribution count first (see SELF_IMPROVEMENT.md, Agent Attribution).",
    $costProposal,
    "- Keep project-local lessons separate from pipeline-level changes.",
    "- Queue surviving proposals under .ai/proposals/ for /me --improve [--drain]."
)
if (-not $NoWrite) { Set-Content -Path $retroPath -Value $lines }

$result = [ordered]@{
    status = "retro_written"
    events = $total
    retro = $retroPath
    failure_clusters = $clusters.Keys
    attributed_agents = $attributions.Keys
    gate_failures = $gateFailures.Keys
    token_events = $tokenEvents
    estimated_tokens = if ($tokenEvents -eq 0) { $null } else { $totalTokens }
    estimated_tokens_by_workflow = $tokenOutputByWorkflow
    estimated_tokens_by_agent = $tokenOutputByAgent
    cost_proposal = $costProposal
}
$result | ConvertTo-Json -Compress
