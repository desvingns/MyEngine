# Read-only deterministic validation of the MyEngine spec board and roadmap.
# Emits exactly one compact JSON object and exits 1 on validation mismatch.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Get-FrontMatter {
    param([string]$Path)

    $lines = [System.IO.File]::ReadAllLines($Path)
    $fields = [ordered]@{}
    $start = 0
    $end = $lines.Count

    if ($lines.Count -gt 0 -and $lines[0].Trim([char]0xFEFF).Trim() -eq "---") {
        $start = 1
        $end = -1
        for ($i = 1; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Trim() -eq "---") {
                $end = $i
                break
            }
        }
        if ($end -lt 0) { return $null }
    } else {
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ([string]::IsNullOrWhiteSpace($lines[$i])) {
                $end = $i
                break
            }
        }
    }

    for ($i = $start; $i -lt $end; $i++) {
        if ($lines[$i] -match '^\s*([A-Za-z][A-Za-z0-9_-]*)\s*:\s*(.*?)\s*$') {
            $fields[$Matches[1].ToLowerInvariant()] = $Matches[2].Trim()
        }
    }

    return $fields
}

function Normalize-Status {
    param([string]$RawStatus)

    if ($null -eq $RawStatus) { return $null }
    $value = $RawStatus.Trim()
    if ($value -match '^\*{0,2}(backlog|active|done)\*{0,2}(?:\s*\([^)]*\))?\s*$') {
        return $Matches[1].ToLowerInvariant()
    }
    return $null
}

function Add-ToMapList {
    param(
        [hashtable]$Map,
        [string]$Key,
        [string]$Value
    )

    if (-not $Map.ContainsKey($Key)) { $Map[$Key] = @() }
    $Map[$Key] = @($Map[$Key]) + $Value
}

$boardDirs = @("backlog", "active", "done")
$cards = @()
$invalidCards = @()

foreach ($dir in $boardDirs) {
    $path = Join-Path $root (Join-Path ".claude/specs" $dir)
    if (-not (Test-Path -LiteralPath $path -PathType Container)) { continue }

    foreach ($file in (Get-ChildItem -LiteralPath $path -File -Filter "*.md" | Sort-Object FullName)) {
        $frontMatter = Get-FrontMatter -Path $file.FullName
        if ($null -eq $frontMatter) {
            $invalidCards += [ordered]@{ file = $file.Name; location = $dir; reason = "unterminated_front_matter" }
            continue
        }

        $hasId = $frontMatter.Contains("id") -and -not [string]::IsNullOrWhiteSpace([string]$frontMatter["id"])
        $hasStatus = $frontMatter.Contains("status") -and -not [string]::IsNullOrWhiteSpace([string]$frontMatter["status"])
        if (-not $hasId -and -not $hasStatus) { continue }
        if (-not $hasId -or -not $hasStatus) {
            $invalidCards += [ordered]@{ file = $file.Name; location = $dir; reason = "missing_id_or_status" }
            continue
        }

        $cards += [pscustomobject][ordered]@{
            id = ([string]$frontMatter["id"]).Trim()
            status = ([string]$frontMatter["status"]).Trim().ToLowerInvariant()
            location = $dir
            file = $file.Name
        }
    }
}

$cardGroups = @($cards | Group-Object id)
$duplicateIds = @($cardGroups | Where-Object { $_.Count -gt 1 } | Sort-Object Name | ForEach-Object { $_.Name })

$roadmapPath = Join-Path $root ".claude/specs/ENGINE_ROADMAP.md"
$roadmapStatuses = @{}
$roadmapRows = 0
$roadmapInvalidStatuses = @()
$roadmapDuplicateIds = @()

if (Test-Path -LiteralPath $roadmapPath -PathType Leaf) {
    foreach ($line in [System.IO.File]::ReadAllLines($roadmapPath)) {
        if ($line -notmatch '^\s*\|') { continue }
        $columns = @($line.Trim().Trim('|').Split('|') | ForEach-Object { $_.Trim() })
        if ($columns.Count -lt 5) { continue }
        if ($columns[0] -eq "Capability" -or $columns[0] -match '^[-:]+$') { continue }

        $roadmapRows++
        $roadmapStatus = Normalize-Status -RawStatus $columns[4]
        $roadmapIds = @([regex]::Matches($columns[1], '\b[A-Z][A-Z0-9]*-\d+\b') | ForEach-Object { $_.Value })
        if ($null -eq $roadmapStatus) {
            $roadmapInvalidStatuses += [ordered]@{ row = $roadmapRows; raw_status = $columns[4]; ids = $roadmapIds }
        }

        foreach ($id in $roadmapIds) {
            if ($roadmapStatuses.ContainsKey($id)) {
                $roadmapDuplicateIds += $id
            } else {
                $roadmapStatuses[$id] = $roadmapStatus
            }
        }
    }
}

$boardIds = @($cards | Select-Object -ExpandProperty id -Unique)
$roadmapDuplicateBoardIds = @($roadmapDuplicateIds | Where-Object { $boardIds -contains $_ } | Sort-Object -Unique)
$duplicateIds = @($duplicateIds + $roadmapDuplicateBoardIds | Sort-Object -Unique)

$cardStatusMismatches = @()
$locationMismatches = @()
$missingRoadmapIds = @()
$validStatuses = @("backlog", "active", "done")

foreach ($card in ($cards | Sort-Object id, location, file)) {
    $roadmapStatus = $null
    if ($roadmapStatuses.ContainsKey($card.id)) { $roadmapStatus = $roadmapStatuses[$card.id] }

    if ($null -eq $roadmapStatus -or $null -eq (Normalize-Status -RawStatus $roadmapStatus)) {
        $missingRoadmapIds += $card.id
    } elseif ($card.status -ne $roadmapStatus) {
        $cardStatusMismatches += [ordered]@{ id = $card.id; card_status = $card.status; roadmap_status = $roadmapStatus }
    }

    if ($card.status -notin $validStatuses -or $card.location -ne $card.status) {
        $locationMismatches += [ordered]@{ id = $card.id; status = $card.status; location = $card.location; expected_location = $card.status }
    }
}

$missingRoadmapIds = @($missingRoadmapIds | Sort-Object -Unique)
$invalidCards = @($invalidCards | Sort-Object location, file)
$roadmapInvalidStatuses = @($roadmapInvalidStatuses)
$failures = $cardStatusMismatches.Count + $locationMismatches.Count + $duplicateIds.Count + $missingRoadmapIds.Count + $invalidCards.Count + $roadmapInvalidStatuses.Count
$verdict = if ($failures -eq 0) { "pass" } else { "fail" }

$counts = [ordered]@{
    cards = $cards.Count
    backlog = @($cards | Where-Object status -eq "backlog").Count
    active = @($cards | Where-Object status -eq "active").Count
    done = @($cards | Where-Object status -eq "done").Count
    roadmap_rows = $roadmapRows
    roadmap_ids = @($roadmapStatuses.Keys).Count
}

$result = [ordered]@{
    verdict = $verdict
    card_status_mismatches = @($cardStatusMismatches)
    location_mismatches = @($locationMismatches)
    duplicate_ids = @($duplicateIds)
    missing_roadmap_ids = @($missingRoadmapIds)
    counts = $counts
}

if ($invalidCards.Count -gt 0) { $result.invalid_cards = @($invalidCards) }
if ($roadmapInvalidStatuses.Count -gt 0) { $result.invalid_roadmap_statuses = @($roadmapInvalidStatuses) }

$result | ConvertTo-Json -Compress -Depth 8
if ($verdict -eq "pass") { exit 0 } else { exit 1 }
