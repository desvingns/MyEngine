$ErrorActionPreference = "Stop"

try {
    $repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
    $generator = Join-Path $repoRoot "scripts\me-new-game.ps1"
    $fixtureRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("myengine-dx001-test-" + [guid]::NewGuid().ToString("N"))
    $fixtureGames = Join-Path $fixtureRoot "games"
    New-Item -ItemType Directory -Force -Path $fixtureGames | Out-Null
    [System.IO.File]::WriteAllText(
        (Join-Path $fixtureRoot "settings.gradle.kts"),
        "rootProject.name = ""fixture""" + [Environment]::NewLine,
        (New-Object System.Text.UTF8Encoding($false))
    )

    function Invoke-Generator {
        param([string]$Slug)
        $output = & powershell.exe -NoProfile -File $generator -Slug $Slug -Root $fixtureRoot 2>&1 | Out-String
        [pscustomobject]@{
            exit_code = $LASTEXITCODE
            output = $output
        }
    }

    $first = Invoke-Generator "test-game"
    if ($first.exit_code -ne 0) {
        throw "Initial generation failed: $($first.output)"
    }

    $gameRoot = Join-Path $fixtureGames "test-game"
    $settings = Get-Content -Raw -LiteralPath (Join-Path $fixtureRoot "settings.gradle.kts")
    if (-not $settings.Contains('include(":games:test-game")')) {
        throw "Generated module include is missing."
    }
    if (-not $settings.Contains('project(":games:test-game").projectDir = file("games/test-game")')) {
        throw "Generated projectDir wiring is missing."
    }

    $required = @(
        "build.gradle.kts",
        "README.md",
        "replay-scenario.properties",
        "src/main/kotlin/dev/myengine/games/test_game/CanonicalScenario.kt",
        "src/test/kotlin/dev/myengine/games/test_game/CanonicalScenarioTest.kt",
        "content/test-game/manifest.properties",
        "content/test-game/maps.json",
        "content/test-game/tiles.properties",
        "content/test-game/resources.properties",
        "content/test-game/recipes.properties",
        "content/test-game/towers.properties",
        "content/test-game/enemies.properties",
        "content/test-game/waves.properties",
        "content/test-game/incidents.properties",
        "content/test-game/strings.properties",
        "spec/00_manifest.yaml",
        "spec/product-brief.md",
        "spec/requirements.md",
        "spec/user-stories.md",
        "spec/acceptance/AC-001.feature",
        "spec/design.md",
        "spec/content-plan.md",
        "spec/engine-gap-analysis.md",
        "spec/balance-plan.md",
        "spec/android-ux.md",
        "spec/nfr.md",
        "spec/risks.md",
        "spec/traceability.csv"
    )
    foreach ($relative in $required) {
        if (-not (Test-Path -LiteralPath (Join-Path $gameRoot $relative) -PathType Leaf)) {
            throw "Required generated file is missing: $relative"
        }
    }

    $second = Invoke-Generator "test-game"
    if ($second.exit_code -eq 0 -or $second.output -notmatch "existing game") {
        throw "Existing slug was not refused: $($second.output)"
    }

    $invalid = Invoke-Generator "Test_Game"
    if ($invalid.exit_code -eq 0 -or $invalid.output -notmatch "lower-kebab-case") {
        throw "Invalid slug was not refused: $($invalid.output)"
    }

    [ordered]@{
        status = "pass"
        fixture = $fixtureRoot
        checked_files = $required.Count
    } | ConvertTo-Json -Compress
}
catch {
    [ordered]@{
        status = "fail"
        error = $_.Exception.Message
    } | ConvertTo-Json -Compress
    exit 1
}
