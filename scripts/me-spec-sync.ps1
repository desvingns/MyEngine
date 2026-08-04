[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$CardPath,
    [string]$Root = "",
    [switch]$Apply,
    [switch]$AllowExternalWrite
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($Root)) {
    $Root = Split-Path -Parent $PSScriptRoot
}

function Normalize-Status {
    param([AllowNull()][string]$RawStatus)

    if ($null -eq $RawStatus) { return $null }
    $value = $RawStatus.Trim()
    $value = ($value -replace '^[`*_]+', '') -replace '[`*_]+$', ''
    $value = $value.Trim()
    if ($value -match '^\((backlog|active|done)\)$') {
        return $Matches[1].ToLowerInvariant()
    }
    if ($value -match '^(backlog|active|done)(?:\s*\([^)]*\))?$') {
        return $Matches[1].ToLowerInvariant()
    }
    return $null
}

function Normalize-Token {
    param([AllowNull()][string]$RawValue)

    if ($null -eq $RawValue) { return "" }
    return (($RawValue.Trim() -replace '^[`*]+', '') -replace '[`*]+$', '').Trim()
}

function Get-FrontMatter {
    param([Parameter(Mandatory = $true)][string]$Path)

    $lines = [System.IO.File]::ReadAllLines($Path)
    if ($lines.Count -eq 0) { throw "Card is empty: $Path" }

    $start = 0
    $end = $lines.Count
    if ($lines[0].Trim([char]0xFEFF).Trim() -eq "---") {
        $start = 1
        $end = -1
        for ($index = 1; $index -lt $lines.Count; $index++) {
            if ($lines[$index].Trim() -eq "---") {
                $end = $index
                break
            }
        }
        if ($end -lt 0) { throw "Card front matter is unterminated: $Path" }
    } else {
        $end = $lines.Count
        for ($index = 0; $index -lt $lines.Count; $index++) {
            if ([string]::IsNullOrWhiteSpace($lines[$index])) {
                $end = $index
                break
            }
        }
    }

    $fields = [ordered]@{}
    for ($index = $start; $index -lt $end; $index++) {
        if ($lines[$index] -match '^\s*([A-Za-z][A-Za-z0-9_-]*)\s*:\s*(.*?)\s*$') {
            $fields[$Matches[1].ToLowerInvariant()] = $Matches[2].Trim()
        }
    }
    foreach ($required in @("id", "title", "status", "phase", "source")) {
        if (-not $fields.Contains($required) -or [string]::IsNullOrWhiteSpace([string]$fields[$required])) {
            throw "Card front matter is missing '$required': $Path"
        }
    }
    return $fields
}

function ConvertTo-PipeCells {
    param([Parameter(Mandatory = $true)][AllowEmptyString()][string]$Line)

    $trimmed = $Line.Trim()
    if (-not $trimmed.StartsWith("|")) { return @() }
    $body = $trimmed.Trim('|')
    return @($body.Split('|') | ForEach-Object { $_.Trim() })
}

function ConvertFrom-PipeCells {
    param([Parameter(Mandatory = $true)][object[]]$Cells)

    return "| " + (($Cells | ForEach-Object { ([string]$_).Trim() }) -join " | ") + " |"
}

function Read-GapRows {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$TargetIds
    )

    $lines = [System.IO.File]::ReadAllLines($Path)
    $headerIndex = -1
    $header = @()
    for ($index = 0; $index -lt $lines.Count; $index++) {
        $cells = @(ConvertTo-PipeCells -Line $lines[$index])
        if ($cells.Count -gt 0 -and $cells[0] -eq "Gap") {
            $headerIndex = $index
            $header = $cells
            break
        }
    }
    if ($headerIndex -lt 0) { throw "Gap analysis table header was not found: $Path" }

    $headerIndexes = @{}
    for ($index = 0; $index -lt $header.Count; $index++) {
        $headerIndexes[[string]$header[$index]] = $index
    }
    foreach ($required in @("Gap", "Type", "Priority", "Status", "Note")) {
        if (-not $headerIndexes.ContainsKey($required)) {
            throw "Gap analysis table is missing required columns Gap, Type, Priority, Status, Note; missing '$required' in $Path"
        }
    }

    $records = @()
    $targetSet = @{}
    foreach ($target in $TargetIds) { $targetSet[$target] = $true }
    for ($index = $headerIndex + 1; $index -lt $lines.Count; $index++) {
        $cells = @(ConvertTo-PipeCells -Line $lines[$index])
        if ($cells.Count -eq 0 -or $cells[0] -match '^[-:]+$') { continue }
        if ($cells.Count -lt $header.Count) { continue }

        $gapId = Normalize-Token -RawValue $cells[$headerIndexes["Gap"]]
        if (-not $targetSet.ContainsKey($gapId)) { continue }
        if (@($records | Where-Object { $_.id -eq $gapId }).Count -gt 0) {
            throw "Duplicate target gap row '$gapId': $Path"
        }
        $statusRaw = [string]$cells[$headerIndexes["Status"]]
        $status = Normalize-Status -RawStatus $statusRaw
        if ($null -eq $status) { throw "Unknown gap status '$statusRaw' for '$gapId': $Path" }
        $records += [pscustomobject][ordered]@{
            id = $gapId
            row = $index + 1
            status = $status
            status_raw = $statusRaw
            line_index = $index
            cells = @($cells)
        }
    }
    $missing = @()
    foreach ($target in $TargetIds) {
        if (@($records | Where-Object { $_.id -eq $target }).Count -eq 0) { $missing += $target }
    }
    if ($missing.Count -gt 0) { throw "Target gap rows were not found: $($missing -join ', ')" }

    return [pscustomobject][ordered]@{
        path = $Path
        lines = @($lines)
        header_indexes = $headerIndexes
        records = @($records)
    }
}

function ConvertTo-CsvField {
    param([AllowNull()][string]$Value)

    if ($null -eq $Value) { return "" }
    if ($Value -match '[,\"\r\n]') {
        return '"' + $Value.Replace('"', '""') + '"'
    }
    return $Value
}

function Read-TraceabilityRows {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string[]]$TargetIds
    )

    Add-Type -AssemblyName Microsoft.VisualBasic
    $parser = New-Object Microsoft.VisualBasic.FileIO.TextFieldParser($Path)
    try {
        $parser.TextFieldType = [Microsoft.VisualBasic.FileIO.FieldType]::Delimited
        $parser.SetDelimiters(',')
        $parser.HasFieldsEnclosedInQuotes = $true
        $headers = @($parser.ReadFields())
        $expected = @("requirement_id", "user_story_id", "acceptance_id", "design_section", "engine_gap_id", "engine_gap_status")
        if (($headers -join ',') -ne ($expected -join ',')) {
            throw "Traceability header must be '$($expected -join ',')': $Path"
        }

        $rows = @()
        $rowNumber = 1
        while (-not $parser.EndOfData) {
            $fields = @($parser.ReadFields())
            $rowNumber++
            if ($fields.Count -ne $headers.Count) {
                throw "Traceability row $rowNumber has $($fields.Count) columns; expected $($headers.Count): $Path"
            }
            $values = [ordered]@{}
            for ($index = 0; $index -lt $headers.Count; $index++) {
                $values[$headers[$index]] = [string]$fields[$index]
            }
            $gapId = Normalize-Token -RawValue $values["engine_gap_id"]
            $status = Normalize-Status -RawStatus $values["engine_gap_status"]
            if ($null -eq $status) {
                $rawTraceStatus = [string]$values["engine_gap_status"]
                throw "Unknown traceability status '$rawTraceStatus' at row $rowNumber`: $Path"
            }
            $rows += [pscustomobject][ordered]@{
                row = $rowNumber
                values = $values
                gap_id = $gapId
                status = $status
            }
        }
    } finally {
        $parser.Close()
    }

    $targetSet = @{}
    foreach ($target in $TargetIds) { $targetSet[$target] = $true }
    $targetRows = @($rows | Where-Object { $targetSet.ContainsKey($_.gap_id) })
    foreach ($target in $TargetIds) {
        if (@($targetRows | Where-Object { $_.gap_id -eq $target }).Count -eq 0) {
            throw "Traceability rows were not found for target gap '$target': $Path"
        }
    }
    return [pscustomobject][ordered]@{
        path = $Path
        headers = @($headers)
        rows = @($rows)
        target_rows = @($targetRows)
    }
}

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Content
    )

    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}

function Get-Newline {
    param([Parameter(Mandatory = $true)][string]$Text)
    if ($Text.Contains("`r`n")) { return "`r`n" }
    return "`n"
}

function Get-SyncState {
    param(
        [Parameter(Mandatory = $true)][string]$GapPath,
        [Parameter(Mandatory = $true)][string]$TraceabilityPath,
        [Parameter(Mandatory = $true)][string[]]$TargetIds
    )

    $gapState = Read-GapRows -Path $GapPath -TargetIds $TargetIds
    $traceState = Read-TraceabilityRows -Path $TraceabilityPath -TargetIds $TargetIds
    $gapById = @{}
    foreach ($gap in $gapState.records) { $gapById[$gap.id] = $gap }
    $traceStale = @($traceState.target_rows | Where-Object {
        $_.status -ne "done" -or $_.status -ne $gapById[$_.gap_id].status
    })
    $gapStale = @($gapState.records | Where-Object { $_.status -ne "done" })
    return [pscustomobject][ordered]@{
        gap = $gapState
        traceability = $traceState
        gap_stale = @($gapStale)
        traceability_stale = @($traceStale)
    }
}

function Test-Board {
    param([Parameter(Mandatory = $true)][string]$RepoRoot)

    $checker = Join-Path $RepoRoot "scripts\me-spec-board-check.ps1"
    if (-not (Test-Path -LiteralPath $checker -PathType Leaf)) { return }
    $raw = @(& powershell.exe -NoProfile -File $checker 2>&1)
    if ($LASTEXITCODE -ne 0 -or $raw.Count -ne 1) {
        throw "Spec board check failed before apply."
    }
    $report = $raw[0].ToString() | ConvertFrom-Json -ErrorAction Stop
    if ($report.verdict -ne "pass") { throw "Spec board check returned '$($report.verdict)' before apply." }
}

$result = $null
$exitCode = 1
try {
    if ([string]::IsNullOrWhiteSpace($CardPath)) { throw "CardPath is required" }
    $repoRoot = [System.IO.Path]::GetFullPath($Root)
    $cardFullPath = if ([System.IO.Path]::IsPathRooted($CardPath)) {
        [System.IO.Path]::GetFullPath($CardPath)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $repoRoot $CardPath))
    }
    if (-not (Test-Path -LiteralPath $cardFullPath -PathType Leaf)) {
        throw "Card was not found: $cardFullPath"
    }
    $frontMatter = Get-FrontMatter -Path $cardFullPath
    $cardStatus = Normalize-Status -RawStatus $frontMatter["status"]
    if ($cardStatus -ne "done") { throw "Card status must be done; observed '$($frontMatter["status"])'." }

    $sourceRaw = [string]$frontMatter["source"]
    $sourcePathText = $sourceRaw
    $referenceText = $sourceRaw
    if ($sourceRaw -match '^\s*(?<path>.+?)\s*\((?<refs>[^()]*)\)\s*$') {
        $sourcePathText = $Matches.path.Trim()
        $referenceText = $Matches.refs
    }
    $targetIds = @([regex]::Matches($referenceText, '(?i)\bEG-\d+\b') | ForEach-Object { $_.Value.ToUpperInvariant() } | Sort-Object -Unique)
    if ($targetIds.Count -eq 0) { throw "Card source must contain at least one EG-* id: $sourceRaw" }
    if ($sourcePathText -eq $sourceRaw) {
        $sourcePathText = [regex]::Replace($sourcePathText, '(?i)\bEG-\d+\b', '')
        $sourcePathText = $sourcePathText -replace '\s*[\[\]\{\},;|#\s]+\s*$', ''
    }
    $sourcePathText = $sourcePathText.Trim().Trim('"', "'")
    if ([string]::IsNullOrWhiteSpace($sourcePathText)) { throw "Card source path is missing: $sourceRaw" }

    $sourcePath = if ([System.IO.Path]::IsPathRooted($sourcePathText)) {
        [System.IO.Path]::GetFullPath($sourcePathText)
    } else {
        [System.IO.Path]::GetFullPath((Join-Path $repoRoot $sourcePathText))
    }
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Container)) {
        throw "Source bundle is missing or unresolvable: $sourcePath"
    }
    $gapPath = Join-Path $sourcePath "engine-gap-analysis.md"
    $traceabilityPath = Join-Path $sourcePath "traceability.csv"
    if (-not (Test-Path -LiteralPath $gapPath -PathType Leaf)) { throw "Source bundle is missing engine-gap-analysis.md: $sourcePath" }
    if (-not (Test-Path -LiteralPath $traceabilityPath -PathType Leaf)) { throw "Source bundle is missing traceability.csv: $sourcePath" }

    $sourceIsExternal = -not $sourcePath.StartsWith($repoRoot.TrimEnd('\') + '\', [System.StringComparison]::OrdinalIgnoreCase)
    if ($Apply -and $sourceIsExternal -and -not $AllowExternalWrite) {
        throw "External apply requires -AllowExternalWrite: $sourcePath"
    }
    if ($Apply) { Test-Board -RepoRoot $repoRoot }
    $state = Get-SyncState -GapPath $gapPath -TraceabilityPath $traceabilityPath -TargetIds $targetIds
    $staleCount = $state.gap_stale.Count + $state.traceability_stale.Count
    $applied = $false
    if ($Apply -and $staleCount -gt 0) {
        $gapText = [System.IO.File]::ReadAllText($gapPath)
        $gapNewline = Get-Newline -Text $gapText
        $gapLines = [System.Collections.Generic.List[string]]::new()
        foreach ($line in $state.gap.lines) { [void]$gapLines.Add($line) }
        foreach ($record in $state.gap.records) {
            if ($record.status -ne "done") {
                $updatedCells = @($record.cells)
                $updatedCells[$state.gap.header_indexes["Status"]] = "done"
                $gapLines[$record.line_index] = ConvertFrom-PipeCells -Cells $updatedCells
            }
        }
        $gapHasFinalNewline = $gapText.EndsWith("`r`n") -or $gapText.EndsWith("`n")
        $gapOutput = $gapLines -join $gapNewline
        if ($gapHasFinalNewline) { $gapOutput += $gapNewline }

        $traceText = [System.IO.File]::ReadAllText($traceabilityPath)
        $traceNewline = Get-Newline -Text $traceText
        $traceLines = @()
        $traceLines += (ConvertTo-CsvField -Value "requirement_id") + "," +
            (ConvertTo-CsvField -Value "user_story_id") + "," +
            (ConvertTo-CsvField -Value "acceptance_id") + "," +
            (ConvertTo-CsvField -Value "design_section") + "," +
            (ConvertTo-CsvField -Value "engine_gap_id") + "," +
            (ConvertTo-CsvField -Value "engine_gap_status")
        $targetSet = @{}
        foreach ($target in $targetIds) { $targetSet[$target] = $true }
        foreach ($row in $state.traceability.rows) {
            $values = $row.values
            if ($targetSet.ContainsKey($row.gap_id)) { $values["engine_gap_status"] = "done" }
            $traceLines += (($state.traceability.headers | ForEach-Object { ConvertTo-CsvField -Value ([string]$values[$_]) }) -join ',')
        }
        $traceHasFinalNewline = $traceText.EndsWith("`r`n") -or $traceText.EndsWith("`n")
        $traceOutput = $traceLines -join $traceNewline
        if ($traceHasFinalNewline) { $traceOutput += $traceNewline }

        Write-Utf8NoBom -Path $gapPath -Content $gapOutput
        Write-Utf8NoBom -Path $traceabilityPath -Content $traceOutput
        $state = Get-SyncState -GapPath $gapPath -TraceabilityPath $traceabilityPath -TargetIds $targetIds
        if ($state.gap_stale.Count -gt 0 -or $state.traceability_stale.Count -gt 0) {
            throw "Post-apply verification found stale rows."
        }
        $applied = $true
        $staleCount = 0
    }

    $verdict = if ($staleCount -eq 0) { "pass" } else { "fail" }
    $exitCode = if ($verdict -eq "pass") { 0 } else { 1 }
    $result = [ordered]@{
        agent = "me-spec-sync"
        verdict = $verdict
        summary = if ($verdict -eq "pass") { if ($applied) { "Source bundle statuses were applied and verified." } else { "Source bundle statuses are synchronized." } } else { "Source bundle has $staleCount stale targeted status row(s); rerun with -Apply." }
        card = $cardFullPath
        source = $sourcePath
        source_path = $sourcePath
        source_is_external = $sourceIsExternal
        source_references = @($targetIds)
        referenced_gap_ids = @($targetIds)
        gaps = @($state.gap.records | ForEach-Object { [ordered]@{ id = $_.id; row = $_.row; status = $_.status; stale = ($_.status -ne "done") } })
        traceability_rows = @($state.traceability.target_rows | ForEach-Object { [ordered]@{ row = $_.row; engine_gap_id = $_.gap_id; status = $_.status; stale = ($_.status -ne "done") } })
        stale_gap_rows = @($state.gap_stale | ForEach-Object { [ordered]@{ id = $_.id; row = $_.row; status = $_.status } })
        stale_traceability_rows = @($state.traceability_stale | ForEach-Object { [ordered]@{ row = $_.row; engine_gap_id = $_.gap_id; status = $_.status } })
        applied = $applied
        changed_files = if ($applied) { @($gapPath, $traceabilityPath) } else { @() }
        read_only = -not $Apply
    }
} catch {
    $result = [ordered]@{
        agent = "me-spec-sync"
        verdict = "fail"
        summary = $_.Exception.Message
        card = $CardPath
        applied = $false
        changed_files = @()
        read_only = -not $Apply
    }
    $exitCode = 1
}

$result | ConvertTo-Json -Compress -Depth 8
exit $exitCode
