param(
    [string]$TelemetryPath = ".ai\runs\telemetry.jsonl"
)

$root = Split-Path -Parent $PSScriptRoot
$path = Join-Path $root $TelemetryPath
$retroDir = Join-Path $root ".ai\retro"
New-Item -ItemType Directory -Force -Path $retroDir | Out-Null
$retroPath = Join-Path $retroDir ("retro-" + (Get-Date -Format "yyyy-MM-dd") + ".md")

if (-not (Test-Path $path)) {
    $empty = [ordered]@{ status = "no_telemetry"; events = 0; retro = $retroPath }
    $empty | ConvertTo-Json -Compress
    exit 0
}

$events = Get-Content -Path $path | Where-Object { $_.Trim().Length -gt 0 } | ForEach-Object { $_ | ConvertFrom-Json }
$total = @($events).Count
$verdicts = @{}
$clusters = @{}
foreach ($event in $events) {
    if (-not $verdicts.ContainsKey($event.verdict)) { $verdicts[$event.verdict] = 0 }
    $verdicts[$event.verdict] = 1 + $verdicts[$event.verdict]
    if ($event.failure_cluster -and $event.failure_cluster.Length -gt 0) {
        if (-not $clusters.ContainsKey($event.failure_cluster)) { $clusters[$event.failure_cluster] = 0 }
        $clusters[$event.failure_cluster] = 1 + $clusters[$event.failure_cluster]
    }
}

$lines = @(
    "# MyEngine Retro " + (Get-Date -Format "yyyy-MM-dd"),
    "",
    "- Events: $total",
    "- Verdicts: " + (($verdicts.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", "),
    "- Failure clusters: " + ($(if ($clusters.Count -eq 0) { "none" } else { ($clusters.GetEnumerator() | Sort-Object Name | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", " })),
    "",
    "## Candidate Improvements",
    "",
    "- Review any repeated failure cluster before changing pipeline instructions.",
    "- Keep project-local lessons separate from pipeline-level changes."
)
Set-Content -Path $retroPath -Value $lines

$result = [ordered]@{
    status = "retro_written"
    events = $total
    retro = $retroPath
    failure_clusters = $clusters.Keys
}
$result | ConvertTo-Json -Compress
