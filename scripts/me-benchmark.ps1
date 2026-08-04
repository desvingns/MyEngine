param(
    [string]$ReportPath = "",
    [string]$BudgetPath = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$command = "engine-devtools:run benchmark"

function Write-Result {
    param(
        [hashtable]$Result,
        [int]$ExitCode = 0
    )

    $Result | ConvertTo-Json -Compress -Depth 12
    exit $ExitCode
}

function Get-LastJsonObject {
    param([string]$Raw)

    try {
        return ($Raw | ConvertFrom-Json -ErrorAction Stop)
    } catch { }
    foreach ($line in ($Raw -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Last 12)) {
        try {
            return ($line.Trim() | ConvertFrom-Json -ErrorAction Stop)
        } catch { }
    }
    return $null
}

function Get-RequiredNumber {
    param(
        [object]$Object,
        [string]$Property,
        [string]$Context
    )

    if ($null -eq $Object -or $null -eq $Object.PSObject.Properties[$Property]) {
        throw "Missing numeric benchmark metric '$Context.$Property'."
    }
    $value = $Object.PSObject.Properties[$Property].Value
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
        throw "Missing numeric benchmark metric '$Context.$Property'."
    }
    try {
        return [double]$value
    } catch {
        throw "Benchmark metric '$Context.$Property' is not numeric."
    }
}

function Get-Measurement {
    param(
        [object]$Report,
        [string]$Scenario,
        [string]$Metric
    )

    $source = $null
    switch ($Scenario) {
        "canonical" { $source = @($Report.scenarios | Where-Object { $_.scenario -eq "canonical" }) | Select-Object -First 1 }
        "kill" { $source = @($Report.scenarios | Where-Object { $_.scenario -eq "kill" }) | Select-Object -First 1 }
        "goal-field-rebuild" { $source = $Report.goal_field_rebuild }
        "spatial-index-1k" { $source = $Report.spatial_index }
        "belt-transport-100" { $source = $Report.belt_transport }
        default { throw "Unknown configured benchmark scenario '$Scenario'." }
    }

    if ($Metric -eq "rebuild_ms") {
        return (Get-RequiredNumber $source "rebuild_ns" $Scenario) / 1000000.0
    }
    if ($Metric -eq "sim_ms") {
        return Get-RequiredNumber $source "sim_ms" $Scenario
    }
    if ($Metric -eq "sim_ms_per_tick") {
        $simMs = Get-RequiredNumber $source "sim_ms" $Scenario
        $ticks = Get-RequiredNumber $source "ticks" $Scenario
        if ($ticks -le 0) { throw "Benchmark metric '$Scenario.ticks' must be positive." }
        return $simMs / $ticks
    }
    throw "Unknown configured benchmark metric '$Metric'."
}

function Get-FrameMeasurement {
    param([object]$Report)

    if ($null -eq $Report -or $null -eq $Report.PSObject.Properties["frame_ms"]) {
        return $null
    }
    $value = $Report.PSObject.Properties["frame_ms"].Value
    if ($null -eq $value -or [string]::IsNullOrWhiteSpace([string]$value)) {
        return $null
    }
    try {
        return [double]$value
    } catch {
        throw "Benchmark metric 'frame_ms' is not numeric."
    }
}

Push-Location $root
try {
    if ([string]::IsNullOrWhiteSpace($BudgetPath)) {
        $BudgetPath = Join-Path $root "config\performance-budgets.v1.json"
    } elseif (-not [System.IO.Path]::IsPathRooted($BudgetPath)) {
        $BudgetPath = Join-Path $root $BudgetPath
    }
    if (-not (Test-Path -LiteralPath $BudgetPath -PathType Leaf)) {
        Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = 1; error = "Budget config not found: $BudgetPath" } 1
    }

    try {
        $budget = Get-Content -Raw -LiteralPath $BudgetPath | ConvertFrom-Json -ErrorAction Stop
        if ($budget.schema_version -ne "performance-budgets-v1") {
            throw "Unsupported performance budget schema '$($budget.schema_version)'."
        }
        if ($null -eq $budget.checks -or @($budget.checks).Count -eq 0) {
            throw "Performance budget config must define at least one check."
        }
        $frameBudget = [double]$budget.frame_ms.max
        if ($frameBudget -le 0) { throw "Frame budget must be positive." }
    } catch {
        Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = 1; error = $_.Exception.Message } 1
    }

    $report = $null
    if (-not [string]::IsNullOrWhiteSpace($ReportPath)) {
        if (-not [System.IO.Path]::IsPathRooted($ReportPath)) { $ReportPath = Join-Path $root $ReportPath }
        if (-not (Test-Path -LiteralPath $ReportPath -PathType Leaf)) {
            Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = 1; error = "Benchmark report not found: $ReportPath" } 1
        }
        try {
            $reportRaw = Get-Content -Raw -LiteralPath $ReportPath
            $report = Get-LastJsonObject $reportRaw
        } catch {
            Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = 1; error = $_.Exception.Message } 1
        }
    } else {
        $env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Android\Android Studio\jbr" }
        $env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
        $raw = & .\gradlew.bat --quiet :engine-devtools:run --args "benchmark" 2>&1 | Out-String
        $gradleExitCode = $LASTEXITCODE
        if ($gradleExitCode -ne 0) {
            Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = $gradleExitCode; error = "Benchmark command failed." } 1
        }
        $report = Get-LastJsonObject $raw
    }

    if ($null -eq $report) {
        Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = 1; error = "Benchmark did not return a JSON report." } 1
    }

    try {
        $checks = @()
        foreach ($configured in @($budget.checks)) {
            $scenario = [string]$configured.scenario
            $metric = [string]$configured.metric
            $maximum = [double]$configured.max
            if ($maximum -le 0) { throw "Budget '$scenario/$metric' must be positive." }
            $actual = Get-Measurement $report $scenario $metric
            $delta = $actual - $maximum
            $checks += [ordered]@{
                scenario = $scenario
                metric = $metric
                actual = $actual
                budget = $maximum
                delta = $delta
                status = if ($actual -le $maximum) { "pass" } else { "fail" }
            }
        }

        $frameActual = Get-FrameMeasurement $report
        $frameStatus = if ($null -eq $frameActual) { "not_measured" } elseif ($frameActual -le $frameBudget) { "pass" } else { "fail" }
        $frameDelta = if ($null -eq $frameActual) { $null } else { $frameActual - $frameBudget }
        $checks += [ordered]@{
            scenario = "frame"
            metric = "frame_ms"
            actual = $frameActual
            budget = $frameBudget
            delta = $frameDelta
            status = $frameStatus
        }

        $failed = @($checks | Where-Object { $_.status -eq "fail" })
        $canonical = @($report.scenarios | Where-Object { $_.scenario -eq "canonical" }) | Select-Object -First 1
        $canonicalMs = if ($null -ne $canonical) { Get-RequiredNumber $canonical "sim_ms" "canonical" } else { $null }
        $verdict = if ($failed.Count -eq 0) { "pass" } else { "fail" }
        $result = [ordered]@{
            status = $verdict
            verdict = $verdict
            command = $command
            exit_code = if ($verdict -eq "pass") { 0 } else { 1 }
            budget_version = $budget.schema_version
            checks = @($checks)
            metrics = [ordered]@{ sim_ms = $canonicalMs; frame_ms = $frameActual }
            report = $report
        }
        $result | ConvertTo-Json -Compress -Depth 12
        exit $(if ($verdict -eq "pass") { 0 } else { 1 })
    } catch {
        Write-Result @{ status = "fail"; verdict = "fail"; command = $command; exit_code = 1; error = $_.Exception.Message } 1
    }
} finally {
    Pop-Location
}
