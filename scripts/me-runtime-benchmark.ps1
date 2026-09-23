param(
    [Parameter(Mandatory = $true)][string]$BaselineRoot,
    [string]$CandidateRoot = (Split-Path -Parent $PSScriptRoot),
    [ValidateRange(1, 1000)][int]$Warmups = 12,
    [ValidateRange(3, 1001)][int]$Samples = 21,
    [ValidateRange(1, 100)][int]$SessionsPerSample = 8,
    [ValidateRange(1, 11)][int]$Forks = 3,
    [ValidateRange(0.0, 100.0)][double]$MaximumRegressionPercent = 5.0
)

$ErrorActionPreference = 'Stop'
$benchmarkSource = Join-Path $PSScriptRoot 'perf/RuntimeSessionBenchmark.java'
$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$env:ANDROID_HOME = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
$javaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe'

function Invoke-DistributionBuild([string]$RepositoryRoot) {
    Push-Location -LiteralPath $RepositoryRoot
    try {
        $buildOutput = & .\gradlew.bat --quiet :engine-devtools:installDist 2>&1
        if ($LASTEXITCODE -ne 0) { throw "Distribution build failed for '$RepositoryRoot': $($buildOutput -join [Environment]::NewLine)" }
        $libPath = Join-Path $RepositoryRoot 'engine-devtools/build/install/engine-devtools/lib'
        if (-not (Test-Path -LiteralPath $libPath -PathType Container)) { throw "Missing distribution libraries: $libPath" }
    } finally { Pop-Location }
}

function Invoke-SampleFork([string]$RepositoryRoot, [switch]$SelfTest) {
    Push-Location -LiteralPath $RepositoryRoot
    try {
        $classpath = Join-Path $RepositoryRoot 'engine-devtools/build/install/engine-devtools/lib/*'
        $arguments = @('-Xms512m', '-Xmx512m', '-cp', $classpath, $benchmarkSource)
        if ($SelfTest) { $arguments += '--self-test' }
        else { $arguments += @("$Warmups", "$Samples", "$SessionsPerSample") }
        $runOutput = & $javaExecutable @arguments 2>&1
        if ($LASTEXITCODE -ne 0) { throw "Benchmark failed for '$RepositoryRoot': $($runOutput -join [Environment]::NewLine)" }
        return ($runOutput -join [Environment]::NewLine | ConvertFrom-Json)
    } finally { Pop-Location }
}

function Get-Median([double[]]$Values) {
    $ordered = @($Values | Sort-Object)
    $middle = [int][Math]::Floor($ordered.Count / 2)
    if ($ordered.Count % 2 -eq 1) { return $ordered[$middle] }
    return ($ordered[$middle - 1] + $ordered[$middle]) / 2.0
}

try {
    $baselinePath = (Resolve-Path -LiteralPath $BaselineRoot).Path
    $candidatePath = (Resolve-Path -LiteralPath $CandidateRoot).Path
    if ($baselinePath -eq $candidatePath) { throw 'Baseline and candidate must be distinct checkouts.' }
    Invoke-DistributionBuild $baselinePath
    Invoke-DistributionBuild $candidatePath
    $null = Invoke-SampleFork $baselinePath -SelfTest
    $null = Invoke-SampleFork $candidatePath -SelfTest
    $baselineRuns = @()
    $candidateRuns = @()
    for ($fork = 0; $fork -lt $Forks; $fork++) {
        # Alternate order to reduce systematic thermal/background-load bias.
        if ($fork % 2 -eq 0) {
            $baselineRuns += Invoke-SampleFork $baselinePath
            $candidateRuns += Invoke-SampleFork $candidatePath
        } else {
            $candidateRuns += Invoke-SampleFork $candidatePath
            $baselineRuns += Invoke-SampleFork $baselinePath
        }
    }
    $comparisons = foreach ($scenario in @('canonical', 'kill')) {
        $baselineMedian = Get-Median -Values @($baselineRuns | ForEach-Object { ($_.scenarios | Where-Object scenario -eq $scenario).median_session_ns })
        $candidateMedian = Get-Median -Values @($candidateRuns | ForEach-Object { ($_.scenarios | Where-Object scenario -eq $scenario).median_session_ns })
        if ($baselineMedian -le 0) { throw "Non-positive baseline timing: $scenario" }
        $regression = (($candidateMedian / $baselineMedian) - 1.0) * 100.0
        [ordered]@{
            scenario = $scenario
            baseline_median_session_ns = $baselineMedian
            candidate_median_session_ns = $candidateMedian
            regression_percent = $regression
            within_budget = ($regression -le $MaximumRegressionPercent)
        }
    }
    $passed = @($comparisons | Where-Object { -not $_.within_budget }).Count -eq 0
    [ordered]@{
        status = $(if ($passed) { 'pass' } else { 'fail' })
        command = 'ENG-036 warmed session comparison'
        baseline_root = $baselinePath
        candidate_root = $candidatePath
        maximum_regression_percent = $MaximumRegressionPercent
        harness_sha256 = (Get-FileHash -LiteralPath $benchmarkSource -Algorithm SHA256).Hash.ToLowerInvariant()
        jvm = $javaExecutable
        heap_mb = 512
        forks = $Forks
        comparisons = @($comparisons)
        baseline_runs = @($baselineRuns)
        candidate_runs = @($candidateRuns)
    } | ConvertTo-Json -Depth 12 -Compress
    if (-not $passed) { exit 1 }
} catch {
    @{ status = 'fail'; command = 'ENG-036 warmed session comparison'; reason = $_.Exception.Message } | ConvertTo-Json -Compress
    exit 1
}
