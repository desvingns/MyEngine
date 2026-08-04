$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $commands = @(
        [ordered]@{ task = ":engine-devtools:run"; arguments = @("--args", "replay-inspect"); label = "engine-devtools:run replay-inspect" }
    )
    $goldenRoot = Join-Path $root "games\sandbox\src\test\resources\golden"
    $goldenFiles = [ordered]@{
        canonical = Join-Path $goldenRoot "canonical.hash"
        kill = Join-Path $goldenRoot "kill.hash"
        resist = Join-Path $goldenRoot "resist.hash"
    }
    $goldens = [ordered]@{}
    foreach ($scenario in $goldenFiles.Keys) {
        $path = $goldenFiles[$scenario]
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
            throw "Missing replay golden file '$path'."
        }
        $hash = (Get-Content -Raw -LiteralPath $path).Trim()
        if ($hash -notmatch '^[0-9a-f]{16}$') {
            throw "Invalid replay golden hash in '$path'."
        }
        $goldens[$scenario] = $hash
    }
    $generatedGames = Get-ChildItem -LiteralPath (Join-Path $root "games") -Directory |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName "replay-scenario.properties") } |
        Sort-Object Name
    foreach ($game in $generatedGames) {
        $metadata = ConvertFrom-StringData (Get-Content -Raw -LiteralPath (Join-Path $game.FullName "replay-scenario.properties"))
        if ($metadata.module -notmatch '^:games:[a-z][a-z0-9]*(?:-[a-z0-9]+)*$' -or
            $metadata.testTask -notmatch '^:games:[a-z][a-z0-9]*(?:-[a-z0-9]+)*:test$' -or
            $metadata.testClass -notmatch '^dev\.myengine\.games\.[a-z][a-z0-9_]*\.CanonicalScenarioTest$') {
            throw "Invalid replay metadata in '$($game.Name)'."
        }
        $commands += [ordered]@{
            task = $metadata.testTask
            arguments = @("--tests", $metadata.testClass)
            label = "$($metadata.testTask) --tests $($metadata.testClass)"
        }
    }

    $results = @()
    foreach ($entry in $commands) {
        $replayOutput = $null
        if ($entry.task -eq ":engine-devtools:run" -and $entry.arguments -contains "replay-inspect") {
            $replayOutput = (& .\gradlew.bat --quiet $entry.task @($entry.arguments) 2>&1 | Out-String).Trim()
        } else {
            & .\gradlew.bat --quiet $entry.task @($entry.arguments) 2>&1 | Out-Null
        }
        $exitCode = $LASTEXITCODE
        $results += [ordered]@{ command = $entry.label; exit_code = $exitCode }
        if ($exitCode -ne 0) {
            [ordered]@{
                status = "fail"
                command = $entry.label
                exit_code = $exitCode
                scenarios = $results
            } | ConvertTo-Json -Compress
            exit $exitCode
        }
        if ($replayOutput -ne $null) {
            try {
                $report = $replayOutput | ConvertFrom-Json
                $comparisons = @()
                foreach ($scenario in $goldenFiles.Keys) {
                    $actualScenario = @($report.scenarios | Where-Object { $_.scenario -eq $scenario }) | Select-Object -First 1
                    $actual = if ($null -eq $actualScenario) { $null } else { [string]$actualScenario.final_hash }
                    $expected = [string]$goldens[$scenario]
                    $comparisons += [ordered]@{
                        scenario = $scenario
                        expected = $expected
                        actual = $actual
                        match = ($null -ne $actual -and $actual -eq $expected)
                    }
                }
                $mismatches = @($comparisons | Where-Object { -not $_.match })
                if ($mismatches.Count -gt 0) {
                    [ordered]@{
                        status = "fail"
                        command = "replay-inspect golden comparison"
                        exit_code = 1
                        mismatches = $mismatches
                        scenarios = $results
                    } | ConvertTo-Json -Compress
                    exit 1
                }
            } catch {
                [ordered]@{
                    status = "fail"
                    command = "replay-inspect golden comparison"
                    exit_code = 1
                    error = $_.Exception.Message
                    scenarios = $results
                } | ConvertTo-Json -Compress
                exit 1
            }
        }
    }
    [ordered]@{
        status = "pass"
        command = "replay-inspect plus golden comparison and generated canonical scenarios"
        exit_code = 0
        scenarios = $results
    } | ConvertTo-Json -Compress
} catch {
    [ordered]@{ status = "fail"; command = "replay discovery"; exit_code = 1; error = $_.Exception.Message } | ConvertTo-Json -Compress
    exit 1
} finally {
    Pop-Location
}
