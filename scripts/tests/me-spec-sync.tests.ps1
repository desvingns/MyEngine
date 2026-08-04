[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$syncScript = Join-Path $repoRoot "scripts\me-spec-sync.ps1"
$fixtureRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("myengine-me-spec-sync-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $fixtureRoot -Force | Out-Null

function Write-Utf8NoBom {
    param([string]$Path, [string]$Content)
    $parent = Split-Path -Parent $Path
    New-Item -ItemType Directory -Path $parent -Force | Out-Null
    [System.IO.File]::WriteAllText($Path, $Content, (New-Object System.Text.UTF8Encoding($false)))
}

function New-Fixture {
    param(
        [string]$Name,
        [string]$SourceText,
        [string]$GapText,
        [string]$TraceText,
        [string]$CardStatus = "**done**",
        [switch]$SourceIsAbsolute,
        [switch]$GapWithoutStatus,
        [switch]$TraceWithoutStatus
    )

    $caseRoot = Join-Path $fixtureRoot $Name
    $bundleRoot = Join-Path $caseRoot "bundle\spec"
    $cardPath = Join-Path $caseRoot "card.md"
    New-Item -ItemType Directory -Path $bundleRoot -Force | Out-Null

    if ($GapWithoutStatus) {
        $GapText = $GapText.Replace(" | Status |", " | State |")
    }
    if ($TraceWithoutStatus) {
        $TraceText = $TraceText.Replace("engine_gap_status", "status")
    }
    Write-Utf8NoBom -Path (Join-Path $bundleRoot "engine-gap-analysis.md") -Content $GapText
    Write-Utf8NoBom -Path (Join-Path $bundleRoot "traceability.csv") -Content $TraceText

    $sourceValue = if ($SourceIsAbsolute) { $bundleRoot } else { "bundle/spec" }
    $card = @(
        "---",
        "id: TEST-$Name",
        "title: Test completed card",
        "status: $CardStatus",
        "phase: process",
        "source: $sourceValue ($SourceText)",
        "---",
        "# Fixture",
        ""
    ) -join "`n"
    Write-Utf8NoBom -Path $cardPath -Content $card
    return [pscustomobject][ordered]@{
        root = $caseRoot
        card = $cardPath
        bundle = $bundleRoot
        gap = Join-Path $bundleRoot "engine-gap-analysis.md"
        trace = Join-Path $bundleRoot "traceability.csv"
    }
}

function Invoke-Sync {
    param(
        [string]$Card,
        [string]$FixtureRoot,
        [switch]$Apply,
        [switch]$AllowExternalWrite
    )

    $arguments = @("-NoProfile", "-File", $syncScript, "-CardPath", $Card, "-Root", $FixtureRoot)
    if ($Apply) { $arguments += "-Apply" }
    if ($AllowExternalWrite) { $arguments += "-AllowExternalWrite" }
    $output = @(& powershell.exe @arguments 2>&1 | ForEach-Object { $_.ToString() })
    $nonEmpty = @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $parsed = $null
    if ($nonEmpty.Count -eq 1) {
        try { $parsed = $nonEmpty[0] | ConvertFrom-Json -ErrorAction Stop } catch { $parsed = $null }
    }
    return [pscustomobject][ordered]@{
        exit_code = $LASTEXITCODE
        output_lines = $nonEmpty
        parsed = $parsed
    }
}

function Assert-Case {
    param([bool]$Condition, [string]$Name)
    if (-not $Condition) { throw "Contract case failed: $Name" }
}

$gapAligned = @(
    "# Engine Gap Analysis",
    "",
    "| Gap | Type | Priority | Status | Note |",
    "|---|---|---|---|---|",
    "| ``EG-001`` | reusable engine | High | **done** | Note, with comma |",
    "| ``EG-002`` | tooling | Medium | (done) | Already shipped |",
    ""
) -join "`n"
$traceAligned = @(
    "requirement_id,user_story_id,acceptance_id,design_section,engine_gap_id,engine_gap_status",
    "FR-001,US-001,AC-001,`"Design, section`",EG-001,**done**",
    "FR-002,US-002,AC-002,Loop,EG-002,(done)",
    ""
) -join "`n"

$gapStale = $gapAligned.Replace("**done**", "active").Replace("(done)", "**(active)**")
$traceStale = $traceAligned.Replace("**done**", "active").Replace("(done)", "(active)")
$failed = @()
$cases = @()

try {
    $aligned = New-Fixture -Name "aligned-relative" -SourceText "EG-001, EG-002" -GapText $gapAligned -TraceText $traceAligned
    $alignedResult = Invoke-Sync -Card $aligned.card -FixtureRoot $aligned.root
    $alignedOk = $alignedResult.exit_code -eq 0 -and $alignedResult.output_lines.Count -eq 1 -and
        $null -ne $alignedResult.parsed -and $alignedResult.parsed.verdict -eq "pass" -and
        @($alignedResult.parsed.referenced_gap_ids).Count -eq 2
    if (-not $alignedOk) { $failed += "aligned-relative" }
    $cases += [ordered]@{ name = "aligned-relative"; verdict = if ($alignedOk) { "pass" } else { "fail" }; exit_code = $alignedResult.exit_code }

    $absolute = New-Fixture -Name "aligned-absolute" -SourceText "EG-001" -GapText $gapAligned -TraceText $traceAligned -SourceIsAbsolute
    $absoluteResult = Invoke-Sync -Card $absolute.card -FixtureRoot $absolute.root
    $absoluteOk = $absoluteResult.exit_code -eq 0 -and $absoluteResult.parsed.source_path -eq ([System.IO.Path]::GetFullPath($absolute.bundle))
    if (-not $absoluteOk) { $failed += "aligned-absolute" }
    $cases += [ordered]@{ name = "aligned-absolute"; verdict = if ($absoluteOk) { "pass" } else { "fail" }; exit_code = $absoluteResult.exit_code }

    $externalRoot = Join-Path $fixtureRoot "external-bundle"
    $externalCardRoot = Join-Path $fixtureRoot "external-card"
    New-Item -ItemType Directory -Force -Path $externalRoot, $externalCardRoot | Out-Null
    Write-Utf8NoBom -Path (Join-Path $externalRoot "engine-gap-analysis.md") -Content $gapStale
    Write-Utf8NoBom -Path (Join-Path $externalRoot "traceability.csv") -Content $traceStale
    $externalCard = Join-Path $externalCardRoot "card.md"
    Write-Utf8NoBom -Path $externalCard -Content (@(
        "---", "id: TEST-external", "title: External test card", "status: done", "phase: process",
        "source: $externalRoot (EG-001, EG-002)", "---", ""
    ) -join "`n")
    $externalGapBeforeGuard = [System.IO.File]::ReadAllText((Join-Path $externalRoot "engine-gap-analysis.md"))
    $externalTraceBeforeGuard = [System.IO.File]::ReadAllText((Join-Path $externalRoot "traceability.csv"))
    $externalGuard = Invoke-Sync -Card $externalCard -FixtureRoot $externalCardRoot -Apply
    $externalGapAfterGuard = [System.IO.File]::ReadAllText((Join-Path $externalRoot "engine-gap-analysis.md"))
    $externalTraceAfterGuard = [System.IO.File]::ReadAllText((Join-Path $externalRoot "traceability.csv"))
    $externalGuardOk = $externalGuard.exit_code -eq 1 -and $externalGuard.parsed.summary -match "AllowExternalWrite" -and
        $externalGapBeforeGuard -ceq $externalGapAfterGuard -and $externalTraceBeforeGuard -ceq $externalTraceAfterGuard
    $externalApply = Invoke-Sync -Card $externalCard -FixtureRoot $externalCardRoot -Apply -AllowExternalWrite
    $externalApplyOk = $externalApply.exit_code -eq 0 -and $externalApply.parsed.applied -eq $true
    if (-not ($externalGuardOk -and $externalApplyOk)) { $failed += "external-write-guard" }
    $cases += [ordered]@{ name = "external-write-guard"; verdict = if ($externalGuardOk -and $externalApplyOk) { "pass" } else { "fail" }; exit_code = if ($externalGuardOk -and $externalApplyOk) { 0 } else { 1 } }

    $stale = New-Fixture -Name "stale-read-only" -SourceText "EG-001, EG-002" -GapText $gapStale -TraceText $traceStale
    $beforeGap = [System.IO.File]::ReadAllText($stale.gap)
    $beforeTrace = [System.IO.File]::ReadAllText($stale.trace)
    $staleResult = Invoke-Sync -Card $stale.card -FixtureRoot $stale.root
    $afterGap = [System.IO.File]::ReadAllText($stale.gap)
    $afterTrace = [System.IO.File]::ReadAllText($stale.trace)
    $staleOk = $staleResult.exit_code -eq 1 -and $staleResult.output_lines.Count -eq 1 -and
        $null -ne $staleResult.parsed -and $staleResult.parsed.verdict -eq "fail" -and
        @($staleResult.parsed.stale_gap_rows).Count -eq 2 -and
        @($staleResult.parsed.stale_traceability_rows).Count -eq 2 -and
        $beforeGap -ceq $afterGap -and $beforeTrace -ceq $afterTrace
    if (-not $staleOk) { $failed += "stale-read-only" }
    $cases += [ordered]@{ name = "stale-read-only"; verdict = if ($staleOk) { "pass" } else { "fail" }; exit_code = $staleResult.exit_code }

    $applyFixture = New-Fixture -Name "apply-persisted" -SourceText "EG-001, EG-002" -GapText $gapStale -TraceText $traceStale
    $applyResult = Invoke-Sync -Card $applyFixture.card -FixtureRoot $applyFixture.root -Apply
    $persistedGap = [System.IO.File]::ReadAllText($applyFixture.gap)
    $persistedTrace = [System.IO.File]::ReadAllText($applyFixture.trace)
    $applyOk = $applyResult.exit_code -eq 0 -and $applyResult.output_lines.Count -eq 1 -and
        $null -ne $applyResult.parsed -and $applyResult.parsed.verdict -eq "pass" -and
        $applyResult.parsed.applied -eq $true -and $persistedGap -match '\| done \|' -and
        $persistedTrace -match '"Design, section",EG-001,done' -and
        $persistedTrace -match 'EG-001,done' -and $persistedTrace -match 'EG-002,done'
    if (-not $applyOk) { $failed += "apply-persisted" }
    $cases += [ordered]@{ name = "apply-persisted"; verdict = if ($applyOk) { "pass" } else { "fail" }; exit_code = $applyResult.exit_code }

    $targetedGap = $gapStale + "| ``EG-003`` | unrelated | Low | active | Preserve |`n"
    $targetedTrace = $traceStale + "FR-003,US-003,AC-003,Unrelated,EG-003,active`n"
    $targeted = New-Fixture -Name "targeted-apply" -SourceText "EG-001" -GapText $targetedGap -TraceText $targetedTrace
    $targetedResult = Invoke-Sync -Card $targeted.card -FixtureRoot $targeted.root -Apply
    $targetedGapText = [System.IO.File]::ReadAllText($targeted.gap)
    $targetedTraceText = [System.IO.File]::ReadAllText($targeted.trace)
    $targetedOk = $targetedResult.exit_code -eq 0 -and $targetedResult.parsed.applied -eq $true -and
        $targetedGapText -match '\| `EG-001` \| reusable engine \| High \| done \|' -and
        $targetedGapText -match '\| `EG-002` \| tooling \| Medium \| \*\*\(active\)\*\* \|' -and
        $targetedGapText -match '\| `EG-003` \| unrelated \| Low \| active \|' -and
        $targetedTraceText -match 'EG-001,done' -and $targetedTraceText -match 'EG-002,\(active\)' -and
        $targetedTraceText -match 'EG-003,active'
    if (-not $targetedOk) { $failed += "targeted-apply" }
    $cases += [ordered]@{ name = "targeted-apply"; verdict = if ($targetedOk) { "pass" } else { "fail" }; exit_code = $targetedResult.exit_code }

    $missingSource = New-Fixture -Name "missing-source" -SourceText "EG-001" -GapText $gapAligned -TraceText $traceAligned
    $missingSourceCard = [System.IO.File]::ReadAllText($missingSource.card).Replace("bundle/spec", "does-not-exist")
    Write-Utf8NoBom -Path $missingSource.card -Content $missingSourceCard
    $missingSourceResult = Invoke-Sync -Card $missingSource.card -FixtureRoot $missingSource.root
    $missingSourceOk = $missingSourceResult.exit_code -eq 1 -and $missingSourceResult.parsed.summary -match "missing or unresolvable"
    if (-not $missingSourceOk) { $failed += "missing-source" }
    $cases += [ordered]@{ name = "missing-source"; verdict = if ($missingSourceOk) { "pass" } else { "fail" }; exit_code = $missingSourceResult.exit_code }

    $missingIds = New-Fixture -Name "missing-eg-ids" -SourceText "" -GapText $gapAligned -TraceText $traceAligned
    $missingIdsResult = Invoke-Sync -Card $missingIds.card -FixtureRoot $missingIds.root
    $missingIdsOk = $missingIdsResult.exit_code -eq 1 -and $missingIdsResult.parsed.summary -match "at least one EG-\* id"
    if (-not $missingIdsOk) { $failed += "missing-eg-ids" }
    $cases += [ordered]@{ name = "missing-eg-ids"; verdict = if ($missingIdsOk) { "pass" } else { "fail" }; exit_code = $missingIdsResult.exit_code }

    $missingColumns = New-Fixture -Name "missing-columns" -SourceText "EG-001" -GapText $gapAligned -TraceText $traceAligned -GapWithoutStatus -TraceWithoutStatus
    $missingColumnsResult = Invoke-Sync -Card $missingColumns.card -FixtureRoot $missingColumns.root
    $missingColumnsOk = $missingColumnsResult.exit_code -eq 1 -and $missingColumnsResult.parsed.summary -match "required columns Gap, Type, Priority, Status, Note"
    if (-not $missingColumnsOk) { $failed += "missing-columns" }
    $cases += [ordered]@{ name = "missing-columns"; verdict = if ($missingColumnsOk) { "pass" } else { "fail" }; exit_code = $missingColumnsResult.exit_code }

    $malformed = New-Fixture -Name "malformed-card" -SourceText "EG-001" -GapText $gapAligned -TraceText $traceAligned
    Write-Utf8NoBom -Path $malformed.card -Content "status: done`nsource: bundle/spec (EG-001)`n"
    $malformedResult = Invoke-Sync -Card $malformed.card -FixtureRoot $malformed.root
    $malformedOk = $malformedResult.exit_code -eq 1 -and $malformedResult.parsed.summary -match "front matter is missing"
    if (-not $malformedOk) { $failed += "malformed-card" }
    $cases += [ordered]@{ name = "malformed-card"; verdict = if ($malformedOk) { "pass" } else { "fail" }; exit_code = $malformedResult.exit_code }

    $oneLinePass = $alignedResult.output_lines.Count -eq 1 -and $alignedResult.exit_code -eq 0
    $oneLineFail = $staleResult.output_lines.Count -eq 1 -and $staleResult.exit_code -eq 1
    $oneLineOk = $oneLinePass -and $oneLineFail
    if (-not $oneLineOk) { $failed += "one-line-exit-contract" }
    $cases += [ordered]@{ name = "one-line-exit-contract"; verdict = if ($oneLineOk) { "pass" } else { "fail" }; exit_code = if ($oneLineOk) { 0 } else { 1 } }

    $exitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
    [ordered]@{
        agent = "me-spec-sync-test"
        verdict = if ($exitCode -eq 0) { "pass" } else { "fail" }
        summary = if ($exitCode -eq 0) { "Spec back-sync alignment, stale reporting, persisted apply, schema/card errors, path resolution, CSV quoting, one-line JSON, exit codes, and read-only behavior are covered." } else { "Contract failures: $($failed -join ', ')." }
        fixture_root = $fixtureRoot
        cases = @($cases)
    } | ConvertTo-Json -Compress -Depth 8
    exit $exitCode
} catch {
    [ordered]@{
        agent = "me-spec-sync-test"
        verdict = "fail"
        summary = $_.Exception.Message
        fixture_root = $fixtureRoot
        cases = @($cases)
    } | ConvertTo-Json -Compress -Depth 8
    exit 1
}
