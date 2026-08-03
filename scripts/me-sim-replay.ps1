$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
    $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
    $commands = @(
        [ordered]@{ task = ":engine-devtools:run"; arguments = @("--args", "replay-inspect"); label = "engine-devtools:run replay-inspect" }
    )
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
        & .\gradlew.bat --quiet $entry.task @($entry.arguments) 2>&1 | Out-Null
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
    }
    [ordered]@{
        status = "pass"
        command = "replay-inspect plus generated canonical scenarios"
        exit_code = 0
        scenarios = $results
    } | ConvertTo-Json -Compress
} catch {
    [ordered]@{ status = "fail"; command = "replay discovery"; exit_code = 1; error = $_.Exception.Message } | ConvertTo-Json -Compress
    exit 1
} finally {
    Pop-Location
}
