$root = Split-Path -Parent $PSScriptRoot
$driftScript = Join-Path $PSScriptRoot 'me-schema-docs-drift.ps1'
$fixtureRoot = Join-Path $PSScriptRoot 'fixtures/schema-docs-drift'

function Invoke-DriftGate {
    param(
        [string]$CodePath,
        [string]$DocsPath
    )

    $rawLines = @(& powershell.exe -NoProfile -File $driftScript `
        -CodePath $CodePath `
        -DocsPath $DocsPath 2>&1)
    $exitCode = $LASTEXITCODE
    $outputLines = @($rawLines | ForEach-Object { $_.ToString() })
    $parsed = $null
    $parseError = $null
    if ($outputLines.Count -eq 1) {
        try {
            $parsed = $outputLines[0] | ConvertFrom-Json -ErrorAction Stop
        } catch {
            $parseError = $_.Exception.Message
        }
    }

    [pscustomobject]@{
        exit_code = $exitCode
        output_lines = $outputLines
        parsed = $parsed
        parse_error = $parseError
    }
}

$cases = @(
    @{ name = 'code-not-doc'; expected_exit = 1; expected_verdict = 'fail'; expected = 'code_not_doc'; key = 'codeOnly' },
    @{ name = 'doc-not-code'; expected_exit = 1; expected_verdict = 'fail'; expected = 'doc_not_code'; key = 'docOnly' },
    @{ name = 'aligned'; expected_exit = 0; expected_verdict = 'pass'; expected = ''; key = '' }
)

$failed = @()
$caseResults = @()
foreach ($case in $cases) {
    $caseRoot = Join-Path $fixtureRoot $case.name
    $run = Invoke-DriftGate `
        -CodePath (Join-Path $caseRoot 'ContentLoader.kt') `
        -DocsPath (Join-Path $caseRoot 'PROPERTIES_SCHEMA.md')
    $parsed = $run.parsed
    $observed = @()
    if ($null -ne $parsed -and -not [string]::IsNullOrWhiteSpace($case.expected)) {
        $observed = @($parsed.PSObject.Properties[$case.expected].Value)
    }
    $codeNotDoc = @()
    $docNotCode = @()
    if ($null -ne $parsed) {
        $codeNotDoc = @($parsed.code_not_doc)
        $docNotCode = @($parsed.doc_not_code)
    }
    $expectedCodeNotDoc = @()
    $expectedDocNotCode = @()
    if ($case.name -eq 'code-not-doc') {
        $expectedCodeNotDoc = @('codeOnly')
    } elseif ($case.name -eq 'doc-not-code') {
        $expectedDocNotCode = @('docOnly')
    }
    $directionOk = (@($codeNotDoc) -join '|') -eq (@($expectedCodeNotDoc) -join '|') -and
        (@($docNotCode) -join '|') -eq (@($expectedDocNotCode) -join '|')
    $shapeOk = $run.output_lines.Count -eq 1 -and $null -ne $parsed
    $statusOk = $run.exit_code -eq $case.expected_exit -and $parsed.verdict -eq $case.expected_verdict
    $ok = $shapeOk -and $statusOk -and $directionOk
    if (-not $ok) { $failed += $case.name }
    $caseResults += [ordered]@{
        name = $case.name
        verdict = if ($ok) { 'pass' } else { 'fail' }
        exit_code = $run.exit_code
        output_line_count = $run.output_lines.Count
        expected_direction = $case.expected
        expected_key = $case.key
        observed_keys = $observed
        parse_error = $run.parse_error
    }
}

$errorCases = @(
    @{ name = 'missing-code'; code_path = Join-Path $fixtureRoot 'missing/ContentLoader.kt'; docs_path = Join-Path $fixtureRoot 'aligned/PROPERTIES_SCHEMA.md' },
    @{ name = 'missing-doc'; code_path = Join-Path $fixtureRoot 'aligned/ContentLoader.kt'; docs_path = Join-Path $fixtureRoot 'missing/PROPERTIES_SCHEMA.md' }
)
foreach ($case in $errorCases) {
    $run = Invoke-DriftGate -CodePath $case.code_path -DocsPath $case.docs_path
    $parsed = $run.parsed
    $ok = $run.exit_code -eq 1 -and
        $run.output_lines.Count -eq 1 -and
        $null -ne $parsed -and
        $parsed.verdict -eq 'fail' -and
        $parsed.code_not_doc.Count -eq 0 -and
        $parsed.doc_not_code.Count -eq 0
    if (-not $ok) { $failed += $case.name }
    $caseResults += [ordered]@{
        name = $case.name
        verdict = if ($ok) { 'pass' } else { 'fail' }
        exit_code = $run.exit_code
        output_line_count = $run.output_lines.Count
        expected_direction = 'input_error'
        expected_key = ''
        observed_keys = @()
        parse_error = $run.parse_error
    }
}

$exitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
$result = [ordered]@{
    agent = 'me-schema-docs-drift-test'
    verdict = if ($exitCode -eq 0) { 'pass' } else { 'fail' }
    summary = if ($exitCode -eq 0) { 'Both schema drift directions, aligned input, and JSON-line/exit-code failures are covered by deterministic fixtures.' } else { "Fixture failures: $($failed -join ', ')." }
    cases = @($caseResults)
}
$result | ConvertTo-Json -Compress -Depth 6
exit $exitCode
