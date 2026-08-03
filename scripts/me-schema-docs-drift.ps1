param(
    [string]$CodePath,
    [string]$DocsPath
)

# Deterministic ContentLoader/properties-schema drift gate.
# The source-side extraction follows the field access conventions used by ContentLoader;
# nested JSON assets, arbitrary strings, and sound-event registries are intentionally out
# of this .properties field contract.

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($CodePath)) {
    $CodePath = Join-Path $root "engine-content/src/main/kotlin/dev/myengine/content/ContentLoader.kt"
}
if ([string]::IsNullOrWhiteSpace($DocsPath)) {
    $DocsPath = Join-Path $root "docs/content-schemas/PROPERTIES_SCHEMA.md"
}

$codeKeys = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
$docKeys = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)

function Add-CodeKey([string]$key) {
    if ([string]::IsNullOrWhiteSpace($key)) { return }
    $normalized = $key.Trim()
    if ($normalized -match '^\$prefix\.(?<field>[A-Za-z][A-Za-z0-9_.<>-]*)$') {
        $normalized = "upgrade.<branch>.<tier>.$($Matches.field)"
    } elseif ($normalized -match '^\$\{prefix\}(?<field>[A-Za-z][A-Za-z0-9_.<>-]*)$') {
        $normalized = "modifier.<index>.$($Matches.field)"
    }
    if ($normalized -notmatch '^[A-Za-z][A-Za-z0-9_.<>-]*$') { return }
    if ($normalized -match '^(upgrade|modifier|cycle|effect|effects|resist)\.$') { return }
    [void]$codeKeys.Add($normalized)
}

function Add-DocKey([string]$key) {
    if ([string]::IsNullOrWhiteSpace($key)) { return }
    $normalized = $key.Trim()
    if ($normalized -match '^(?<prefix>effect|effects)\.\d+$') {
        $normalized = "$($Matches.prefix).<index>"
    }
    if ($normalized -notmatch '^[A-Za-z][A-Za-z0-9_.<>-]*$') { return }
    if ($normalized -match '\.(properties|json)$') { return }
    [void]$docKeys.Add($normalized)
}

$exitCode = 0
$result = $null
try {
    if (-not (Test-Path -LiteralPath $CodePath -PathType Leaf)) {
        throw "ContentLoader source file is missing: $CodePath"
    }
    if (-not (Test-Path -LiteralPath $DocsPath -PathType Leaf)) {
        throw "Schema documentation file is missing: $DocsPath"
    }

    $codeLines = Get-Content -LiteralPath $CodePath
    foreach ($line in $codeLines) {
        # Manifest fields use a local required(...) helper and direct getProperty(...).
        foreach ($match in [regex]::Matches($line, '(?<!\.)\brequired\(\s*"([A-Za-z][A-Za-z0-9_.<>-]*)"')) {
            Add-CodeKey $match.Groups[1].Value
        }
        foreach ($match in [regex]::Matches($line, 'props\.getProperty\(\s*"([A-Za-z][A-Za-z0-9_.<>-]*)"')) {
            Add-CodeKey $match.Groups[1].Value
        }

        # Definition fields are passed as the final quoted argument to fields/scopedFields
        # required/optional helpers. Selecting the final literal avoids treating a definition
        # id such as "endless" as a schema key.
        if ($line -match '\b(?:fields|scopedFields)\.(?:required|optional)[A-Za-z]*\(') {
            $matches = [regex]::Matches($line, '"([A-Za-z][A-Za-z0-9_.<>-]*)"')
            if ($matches.Count -gt 0) {
                Add-CodeKey $matches[$matches.Count - 1].Groups[1].Value
            }
        }
        foreach ($match in [regex]::Matches($line, '"(\$prefix\.[A-Za-z][A-Za-z0-9_.<>-]*)"')) {
            Add-CodeKey $match.Groups[1].Value
        }

        # Direct field reads/checks cover optional fields whose validation helper has no
        # literal field argument on the call site.
        foreach ($match in [regex]::Matches($line, '(?:fields|this)\.containsKey\(\s*"([A-Za-z][A-Za-z0-9_.<>-]*)"')) {
            Add-CodeKey $match.Groups[1].Value
        }
        foreach ($match in [regex]::Matches($line, '(?:fields|this)\[\s*"([A-Za-z][A-Za-z0-9_.<>-]*)"')) {
            Add-CodeKey $match.Groups[1].Value
        }
        foreach ($match in [regex]::Matches($line, 'val\s+\w+Field\s*=\s*"([A-Za-z][A-Za-z0-9_.<>-]*)"')) {
            Add-CodeKey $match.Groups[1].Value
        }
    }

    $codeText = Get-Content -Raw -LiteralPath $CodePath
    if ($codeText -match 'fieldPrefix.*spritePath') {
        @(
            'spritePath',
            'atlasPath',
            'atlasKey',
            'upgrade.<branch>.<tier>.spritePath',
            'upgrade.<branch>.<tier>.atlasPath',
            'upgrade.<branch>.<tier>.atlasKey'
        ) | ForEach-Object { Add-CodeKey $_ }
    }
    if ($codeText -match 'startsWith\("resist\."\)') {
        Add-CodeKey 'resist.<damageTypeId>'
    }
    if ($codeText -match 'startsWith\("modifier\."\)') {
        @(
            'modifier.<index>.healthPercent',
            'modifier.<index>.speedPercent',
            'modifier.<index>.count'
        ) | ForEach-Object { Add-CodeKey $_ }
    }
    if ($codeText -match 'startsWith\("cycle\."\)') {
        Add-CodeKey 'cycle.<index>'
    }
    if ($codeText -match '\(\?:effect\|effects\)') {
        Add-CodeKey 'effect.<index>'
        Add-CodeKey 'effects.<index>'
    }

    $inPropertiesSchema = $false
    $section = ''
    foreach ($line in Get-Content -LiteralPath $DocsPath) {
        if ($line -eq '## Manifest') { $inPropertiesSchema = $true }
        if ($line -eq '## Nested Map Assets') { break }
        if (-not $inPropertiesSchema) { continue }
        if ($line -match '^###\s+(.+)$') { $section = $Matches[1] }
        if ($section -in @('Sounds', 'Strings')) { continue }

        # Field bullets are the authoritative documentation list. Restrict extraction to the
        # text before its first colon so examples/enum values cannot become field keys.
        if ($line.TrimStart().StartsWith('-')) {
            $bullet = $line
            $colon = $bullet.IndexOf(':')
            if ($colon -ge 0) { $bullet = $bullet.Substring(0, $colon) }
            foreach ($match in [regex]::Matches($bullet, '`([^`]+)`')) {
                Add-DocKey $match.Groups[1].Value.Split('=')[0]
            }
        }

        # These inline forms document dynamic/legacy properties rather than values or JSON keys.
        foreach ($match in [regex]::Matches($line, '`([^`=]+)=([^`]+)`')) {
            $candidate = $match.Groups[1].Value.Trim()
            if ($candidate -match '^(damageTypeId|resist\.|effect|effects\.)') {
                Add-DocKey $candidate
            }
        }
        if ($line -match 'legacy spelling\s+`([^`]+)`') {
            Add-DocKey $Matches[1]
        }
    }

    $codeNotDoc = @($codeKeys | Where-Object { -not $docKeys.Contains($_) } | Sort-Object)
    $docNotCode = @($docKeys | Where-Object { -not $codeKeys.Contains($_) } | Sort-Object)
    $driftCount = $codeNotDoc.Count + $docNotCode.Count
    if ($driftCount -gt 0) { $exitCode = 1 }
    $summary = if ($driftCount -eq 0) {
        'ContentLoader property keys and schema documentation are aligned.'
    } else {
        "Schema drift detected: $($codeNotDoc.Count) code-not-doc and $($docNotCode.Count) doc-not-code key(s)."
    }
    $result = [ordered]@{
        agent = 'me-schema-docs-drift'
        verdict = if ($driftCount -eq 0) { 'pass' } else { 'fail' }
        summary = $summary
        code_not_doc = $codeNotDoc
        doc_not_code = $docNotCode
        code_key_count = $codeKeys.Count
        doc_key_count = $docKeys.Count
    }
} catch {
    $exitCode = 1
    $result = [ordered]@{
        agent = 'me-schema-docs-drift'
        verdict = 'fail'
        summary = $_.Exception.Message
        code_not_doc = @()
        doc_not_code = @()
        code_key_count = 0
        doc_key_count = 0
    }
}

$result | ConvertTo-Json -Compress -Depth 5
exit $exitCode
