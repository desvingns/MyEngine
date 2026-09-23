param(
    [Parameter(Mandatory = $true)][string]$BaselineRoot,
    [string]$CandidateRoot = (Split-Path -Parent $PSScriptRoot),
    [Parameter(Mandatory = $true)][string]$ReportDirectory
)

$ErrorActionPreference = 'Stop'
$benchmarkSource = Join-Path $PSScriptRoot 'perf/PairedRuntimeSessionBenchmark.java'
$env:JAVA_HOME = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Android\Android Studio\jbr' }
$javaExecutable = Join-Path $env:JAVA_HOME 'bin/java.exe'
$compilerExecutable = Join-Path $env:JAVA_HOME 'bin/javac.exe'

function Get-DistributionManifest([string]$RepositoryRoot) {
    $files = @(
        Get-ChildItem -LiteralPath (Join-Path $RepositoryRoot 'engine-devtools/build/install/engine-devtools/lib') -Filter '*.jar' -File
        Get-ChildItem -LiteralPath (Join-Path $RepositoryRoot 'games/sandbox/content/sandbox') -Recurse -File
    ) | Sort-Object FullName
    return @($files | ForEach-Object {
        [ordered]@{
            path = $_.FullName.Substring($RepositoryRoot.Length).TrimStart([char[]]'\/').Replace('\', '/')
            bytes = $_.Length
            sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    })
}

function Invoke-ControlledJvm([string]$Mode, [string]$LeftRoot, [string]$RightRoot) {
    $stdout = Join-Path $reportPath "$Mode.stdout.json"
    $stderr = Join-Path $reportPath "$Mode.stderr.log"
    $arguments = @('-Xms512m', '-Xmx512m', '-XX:+UseG1GC', '-XX:ActiveProcessorCount=4', '-cp', ('"' + $classPath + '"'), 'PairedRuntimeSessionBenchmark')
    if ($Mode -eq 'self-test') { $arguments += '--self-test' }
    else { $arguments += @($Mode, ('"' + $LeftRoot + '"'), ('"' + $RightRoot + '"')) }
    $process = Start-Process -FilePath $javaExecutable -ArgumentList $arguments -WorkingDirectory $candidatePath -WindowStyle Hidden -PassThru -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    # Windows PowerShell can lose ExitCode for a quickly exiting Start-Process child unless the
    # process handle is retained before waiting. This also keeps startup failures observable.
    $null = $process.Handle
    $process.ProcessorAffinity = [IntPtr]15
    $process.PriorityClass = [System.Diagnostics.ProcessPriorityClass]::Normal
    $process.WaitForExit()
    $exitCode = $process.ExitCode
    if ($null -eq $exitCode -or $exitCode -ne 0) { throw "$Mode JVM failed with exit '$exitCode': $(Get-Content -LiteralPath $stderr -Raw)" }
    if ((Get-Item -LiteralPath $stderr).Length -gt 0) { throw "$Mode JVM emitted diagnostics requiring inspection: $stderr" }
    return (Get-Content -LiteralPath $stdout -Raw | ConvertFrom-Json)
}

try {
    $baselinePath = (Resolve-Path -LiteralPath $BaselineRoot).Path
    $candidatePath = (Resolve-Path -LiteralPath $CandidateRoot).Path
    if ($baselinePath -eq $candidatePath) { throw 'The comparison needs distinct baseline and candidate checkouts.' }
    if (Test-Path -LiteralPath $ReportDirectory) { throw 'Use a new report directory; earlier evidence must not be overwritten.' }
    $reportPath = (New-Item -ItemType Directory -Path $ReportDirectory).FullName
    $classPath = (New-Item -ItemType Directory -Path (Join-Path $reportPath 'harness-classes')).FullName
    foreach ($repository in @($baselinePath, $candidatePath)) {
        if (-not (Test-Path -LiteralPath (Join-Path $repository 'engine-devtools/build/install/engine-devtools/lib'))) {
            throw "Build :engine-devtools:installDist before reserving the quiet window: $repository"
        }
    }
    # Bind evidence to the exact loaded distribution/content, not only a checkout directory name.
    $distributionManifest = [ordered]@{
        baseline = @(Get-DistributionManifest $baselinePath)
        candidate = @(Get-DistributionManifest $candidatePath)
    }
    $distributionManifest | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $reportPath 'distribution-manifest.json') -Encoding UTF8
    $candidateDiff = Join-Path $reportPath 'candidate-tracked.diff'
    & git -C $candidatePath diff --binary "--output=$candidateDiff" HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Could not retain the candidate tracked diff.' }
    $candidateHead = (& git -C $candidatePath rev-parse HEAD).Trim()
    $untracked = @(& git -C $candidatePath ls-files --others --exclude-standard)
    $sourceIdentity = [ordered]@{
        baseline_commit = '30f4eb17aff0ea2fe6cf80aef970a1e7746dbcbb'
        baseline_origin = 'Root-created archive of this exact commit; distribution fingerprint recorded separately.'
        candidate_head = $candidateHead
        candidate_tracked_diff_sha256 = (Get-FileHash -LiteralPath $candidateDiff -Algorithm SHA256).Hash.ToLowerInvariant()
        candidate_untracked_files = @($untracked | ForEach-Object {
            [ordered]@{ path = $_; sha256 = (Get-FileHash -LiteralPath (Join-Path $candidatePath $_) -Algorithm SHA256).Hash.ToLowerInvariant() }
        })
        benchmark_sources = @('perf/RuntimeSessionBenchmark.java', 'perf/PairedRuntimeSessionBenchmark.java', 'me-runtime-benchmark.ps1', 'me-runtime-paired-benchmark.ps1' | ForEach-Object {
            [ordered]@{ path = $_; sha256 = (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot $_) -Algorithm SHA256).Hash.ToLowerInvariant() }
        })
    }
    $sourceIdentity | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $reportPath 'source-identity.json') -Encoding UTF8
    # Compile before the measured JVM starts: source-file compilation/JIT cannot pollute warmup.
    $compileOutput = & $compilerExecutable --release 17 -d $classPath $benchmarkSource 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Harness compilation failed: $($compileOutput -join [Environment]::NewLine)" }
    $null = Invoke-ControlledJvm 'self-test' $baselinePath $baselinePath
    $calibration = Invoke-ControlledJvm 'calibration' $baselinePath $baselinePath
    # Always retain both prescribed runs; never select/discard samples or retry to obtain a pass.
    $comparison = Invoke-ControlledJvm 'comparison' $baselinePath $candidatePath
    $status = if ($calibration.status -ne 'pass') { 'inconclusive' } elseif ($comparison.status -eq 'pass') { 'pass' } else { 'fail' }
    $report = [ordered]@{
        status = $status
        command = 'ENG-036 controlled paired calibration and comparison'
        methodology = 'alternating adjacent pairs in one JVM with isolated engine classloaders'
        affinity_mask = '0xF'
        priority = 'Normal'
        heap_mb = 512
        collector = 'G1'
        active_processor_count = 4
        warmups = 128
        samples = 101
        sessions_per_sample = 8
        maximum_regression_percent = 5
        java = $javaExecutable
        harness_sha256 = (Get-FileHash -LiteralPath $benchmarkSource -Algorithm SHA256).Hash.ToLowerInvariant()
        report_directory = $reportPath
        distribution_manifest_sha256 = (Get-FileHash -LiteralPath (Join-Path $reportPath 'distribution-manifest.json') -Algorithm SHA256).Hash.ToLowerInvariant()
        calibration = $calibration
        comparison = $comparison
    }
    $json = $report | ConvertTo-Json -Depth 16 -Compress
    $json | Set-Content -LiteralPath (Join-Path $reportPath 'report.json') -Encoding UTF8
    $json
    if ($status -ne 'pass') { exit 1 }
} catch {
    @{ status = 'fail'; command = 'ENG-036 controlled paired calibration and comparison'; reason = $_.Exception.Message } | ConvertTo-Json -Compress
    exit 1
}
