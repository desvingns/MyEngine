# me-selfcheck.ps1
# Deterministic adapter/marketplace self-check for MyEngine.
# Verifies that canonical docs exist, that the Claude/Codex adapters are present and
# reference canon (thin-adapter rule), and that the marketplace lists both plugins.
# Emits exactly one compact JSON object (runner convention).

$root = Split-Path -Parent $PSScriptRoot

$canon = @(
    "docs/agentic/PIPELINE.md",
    "docs/agentic/AGENT_CONTRACTS.md",
    "docs/agentic/SPEC_BOARD.md",
    "docs/agentic/SELF_IMPROVEMENT.md",
    "docs/GAME_SPEC_PIPELINE.md",
    "AGENTS.md",
    "STATE.md",
    ".ai/handoff.md"
)

$artifacts = @(
    ".claude-plugin/marketplace.json",
    "claude-plugins/me-dev/.claude-plugin/plugin.json",
    "claude-plugins/me-dev/skills/me/SKILL.md",
    "claude-plugins/me-spec/.claude-plugin/plugin.json",
    "claude-plugins/me-spec/skills/me-spec/SKILL.md",
    "codex-plugins/me-dev/skills/me-dev/SKILL.md",
    "codex-plugins/me-spec/skills/me-spec/SKILL.md"
)

$missing = @()
foreach ($rel in ($canon + $artifacts)) {
    if (-not (Test-Path (Join-Path $root $rel))) { $missing += $rel }
}

# Adapters must reference canon docs (they are thin, not a source of truth).
$refChecks = @(
    @{ file = "claude-plugins/me-dev/skills/me/SKILL.md";        needle = "docs/agentic" },
    @{ file = "claude-plugins/me-spec/skills/me-spec/SKILL.md";  needle = "GAME_SPEC_PIPELINE" },
    @{ file = "codex-plugins/me-dev/skills/me-dev/SKILL.md";     needle = "docs/agentic" },
    @{ file = "codex-plugins/me-spec/skills/me-spec/SKILL.md";   needle = "GAME_SPEC_PIPELINE" }
)
$refMissing = @()
foreach ($c in $refChecks) {
    $p = Join-Path $root $c.file
    if (Test-Path $p) {
        $txt = Get-Content -Raw -Path $p
        if ($txt -notmatch [regex]::Escape($c.needle)) { $refMissing += $c.file }
    }
}

# JSON manifests must parse.
$jsonBad = @()
foreach ($rel in @(".claude-plugin/marketplace.json", "claude-plugins/me-dev/.claude-plugin/plugin.json", "claude-plugins/me-spec/.claude-plugin/plugin.json")) {
    $p = Join-Path $root $rel
    if (Test-Path $p) {
        try { Get-Content -Raw -Path $p | ConvertFrom-Json -ErrorAction Stop | Out-Null }
        catch { $jsonBad += $rel }
    }
}

# Marketplace must list both plugin sources.
$mkMissing = @()
$mk = Join-Path $root ".claude-plugin/marketplace.json"
if (Test-Path $mk) {
    $mkTxt = Get-Content -Raw -Path $mk
    foreach ($src in @("claude-plugins/me-dev", "claude-plugins/me-spec")) {
        if ($mkTxt -notmatch [regex]::Escape($src)) { $mkMissing += $src }
    }
} else {
    $mkMissing += ".claude-plugin/marketplace.json"
}

$failures = $missing.Count + $refMissing.Count + $jsonBad.Count + $mkMissing.Count
$boardCheckRaw = & powershell.exe -NoProfile -File (Join-Path $root "scripts/me-spec-board-check.ps1") 2>$null | Out-String
$boardCheckExit = $LASTEXITCODE
$boardCheck = $null
try {
    $boardCheck = $boardCheckRaw.Trim() | ConvertFrom-Json -ErrorAction Stop
} catch {
    $boardCheck = [ordered]@{
        verdict = "fail"
        error = "spec board checker did not return one JSON result"
    }
    $boardCheckExit = 1
}
if ($boardCheckExit -ne 0 -or $null -eq $boardCheck -or $boardCheck.verdict -ne "pass") { $failures++ }

$schemaDriftRaw = & powershell.exe -NoProfile -File (Join-Path $root "scripts/me-schema-docs-drift.ps1") 2>$null | Out-String
$schemaDriftExit = $LASTEXITCODE
$schemaDrift = $null
try {
    $schemaDrift = $schemaDriftRaw.Trim() | ConvertFrom-Json -ErrorAction Stop
} catch {
    $schemaDrift = [ordered]@{
        verdict = "fail"
        summary = "schema-docs drift gate did not return one JSON result"
    }
    $schemaDriftExit = 1
}
if ($schemaDriftExit -ne 0 -or $null -eq $schemaDrift -or $schemaDrift.verdict -ne "pass") { $failures++ }
$verdict = if ($failures -eq 0) { "pass" } else { "fail" }

$result = [ordered]@{
    agent                          = "me-selfcheck"
    verdict                        = $verdict
    missing_files                  = @($missing)
    adapters_not_referencing_canon = @($refMissing)
    unparseable_json               = @($jsonBad)
    marketplace_missing_sources    = @($mkMissing)
    spec_board_check               = $boardCheck
    schema_docs_drift              = $schemaDrift
}
$result | ConvertTo-Json -Compress -Depth 6
if ($verdict -eq "pass") { exit 0 } else { exit 1 }
