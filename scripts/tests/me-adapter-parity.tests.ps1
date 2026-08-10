[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

function Read-RepoText {
    param([string]$RelativePath)
    $path = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) { throw "Missing adapter file: $RelativePath" }
    return [System.IO.File]::ReadAllText($path)
}

function Test-ModeParity {
    param(
        [string]$ClaudePath,
        [string]$CodexPath,
        [string[]]$Modes
    )
    $claude = Read-RepoText $ClaudePath
    $codex = Read-RepoText $CodexPath
    $missing = @()
    foreach ($mode in $Modes) {
        if ($claude -notmatch [regex]::Escape($mode)) { $missing += "claude:$mode" }
        if ($codex -notmatch [regex]::Escape($mode)) { $missing += "codex:$mode" }
    }
    return [ordered]@{
        claude_path = $ClaudePath
        codex_path = $CodexPath
        modes = @($Modes)
        missing = @($missing)
        verdict = if ($missing.Count -eq 0) { "pass" } else { "fail" }
    }
}

$failed = @()
$checks = @()
try {
    $meModes = @(
        "--discuss", "--spec", "--feature --next", "--bugfix", "--balance", "--perf",
        "--content-validate", "--save-compat", "--reflect", "--improve",
        "--improve --drain", "--upgrade"
    )
    $meSpecModes = @("--greenfield-game", "--engine-feature", "--reference-game")

    $meResult = Test-ModeParity `
        "claude-plugins/me-dev/skills/me/SKILL.md" `
        "codex-plugins/me-dev/skills/me-dev/SKILL.md" `
        $meModes
    $specResult = Test-ModeParity `
        "claude-plugins/me-spec/skills/me-spec/SKILL.md" `
        "codex-plugins/me-spec/skills/me-spec/SKILL.md" `
        $meSpecModes
    $checks += [ordered]@{ name = "me-mode-parity"; verdict = $meResult.verdict; details = $meResult }
    $checks += [ordered]@{ name = "me-spec-mode-parity"; verdict = $specResult.verdict; details = $specResult }
    if ($meResult.verdict -ne "pass") { $failed += "me-mode-parity" }
    if ($specResult.verdict -ne "pass") { $failed += "me-spec-mode-parity" }

    $canonicalChecks = @(
        @{ name = "claude-me-canon"; path = "claude-plugins/me-dev/skills/me/SKILL.md"; needle = "docs/agentic/PIPELINE.md" },
        @{ name = "codex-me-canon"; path = "codex-plugins/me-dev/skills/me-dev/SKILL.md"; needle = "docs/agentic/PIPELINE.md" },
        @{ name = "claude-me-spec-canon"; path = "claude-plugins/me-spec/skills/me-spec/SKILL.md"; needle = "docs/GAME_SPEC_PIPELINE.md" },
        @{ name = "codex-me-spec-canon"; path = "codex-plugins/me-spec/skills/me-spec/SKILL.md"; needle = "docs/GAME_SPEC_PIPELINE.md" }
    )
    foreach ($check in $canonicalChecks) {
        $text = Read-RepoText $check.path
        $ok = $text -match [regex]::Escape($check.needle)
        $checks += [ordered]@{ name = $check.name; verdict = if ($ok) { "pass" } else { "fail" } }
        if (-not $ok) { $failed += $check.name }
    }

    $manifestChecks = @(
        @{ name = "codex-me-dev-manifest"; path = "codex-plugins/me-dev/.codex-plugin/plugin.json"; expected = "me-dev" },
        @{ name = "codex-me-spec-manifest"; path = "codex-plugins/me-spec/.codex-plugin/plugin.json"; expected = "me-spec" }
    )
    foreach ($check in $manifestChecks) {
        $manifestText = Read-RepoText $check.path
        $manifest = $manifestText | ConvertFrom-Json -ErrorAction Stop
        $ok = $manifest.name -eq $check.expected -and $manifest.version -match '^\d+\.\d+\.\d+$'
        $checks += [ordered]@{ name = $check.name; verdict = if ($ok) { "pass" } else { "fail" }; version = $manifest.version }
        if (-not $ok) { $failed += $check.name }
    }

    $registrationChecks = @(
        @{ name = "codex-skills-registration"; path = ".codex/skills/README.md"; needles = @("codex-plugins/me-dev/skills/me-dev", "codex-plugins/me-spec/skills/me-spec") },
        @{ name = "codex-agents-registration"; path = ".codex/agents/README.md"; needles = @("AGENT_CONTRACTS", "codex-plugins/me-dev", "codex-plugins/me-spec") }
    )
    foreach ($check in $registrationChecks) {
        $text = Read-RepoText $check.path
        $missing = @($check.needles | Where-Object { $text -notmatch [regex]::Escape($_) })
        $ok = $missing.Count -eq 0
        $checks += [ordered]@{ name = $check.name; verdict = if ($ok) { "pass" } else { "fail" }; missing = @($missing) }
        if (-not $ok) { $failed += $check.name }
    }

    $exitCode = if ($failed.Count -eq 0) { 0 } else { 1 }
    [ordered]@{
        agent = "me-adapter-parity-test"
        verdict = if ($exitCode -eq 0) { "pass" } else { "fail" }
        summary = if ($exitCode -eq 0) { "Claude/Codex mode parity, canonical references, Codex manifests, and .codex registration are aligned." } else { "Adapter parity failures: $($failed -join ', ')." }
        checks = @($checks)
    } | ConvertTo-Json -Compress -Depth 8
    exit $exitCode
} catch {
    [ordered]@{
        agent = "me-adapter-parity-test"
        verdict = "fail"
        summary = $_.Exception.Message
        checks = @($checks)
    } | ConvertTo-Json -Compress -Depth 8
    exit 1
}
