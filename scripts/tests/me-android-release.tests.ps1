$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$scriptPath = Join-Path $root "scripts\me-android-release.ps1"
$buildPath = Join-Path $root "android\build.gradle.kts"
$rulesPath = Join-Path $root "android\proguard-rules.pro"
$policyPath = Join-Path $root "docs\ANDROID_RELEASE.md"
$source = Get-Content -Raw -LiteralPath $scriptPath
$build = Get-Content -Raw -LiteralPath $buildPath
$rules = Get-Content -Raw -LiteralPath $rulesPath
$policy = Get-Content -Raw -LiteralPath $policyPath

function Invoke-Lane {
    param([string[]]$Arguments)

    $raw = & powershell.exe -NoProfile -File $scriptPath @Arguments 2>&1 | Out-String
    $exitCode = $LASTEXITCODE
    $lines = @($raw -split "`r?`n" | Where-Object { $_.Trim() })
    $json = $null
    if ($lines.Count -eq 1) { try { $json = $lines[0].Trim() | ConvertFrom-Json -ErrorAction Stop } catch { } }
    return [ordered]@{ exit_code = $exitCode; lines = $lines; json = $json; raw = $raw.Trim() }
}

$missing = Join-Path $root "__proc014_missing_keystore_contract__"
$first = Invoke-Lane @("-KeystorePropertiesPath", $missing, "-SkipDevice", "-PreflightOnly")
$second = Invoke-Lane @("-KeystorePropertiesPath", $missing, "-SkipDevice", "-PreflightOnly")
$failures = @()
if ($first.lines.Count -ne 1 -or $second.lines.Count -ne 1) { $failures += "expected_one_json_line" }
if ($first.exit_code -ne 2 -or $second.exit_code -ne 2) { $failures += "expected_blocked_exit_code_2" }
if ($null -eq $first.json -or $first.json.verdict -ne "blocked" -or $first.json.reason -ne "keystore_properties_missing") { $failures += "expected_keystore_block" }
if ($first.raw -ne $second.raw) { $failures += "nondeterministic_preflight_output" }

$requiredTokens = [ordered]@{
    release_tasks = $source -match ':android:validateReleaseSigning' -and $source -match ':android:bundleRelease' -and $source -match ':android:assembleRelease'
    signing_preflight = $source -match 'untracked_keystore_properties' -and $source -match 'keystore_properties_missing'
    aab_size = $source -match 'size_bytes' -and $source -match 'Read-AabMetrics'
    content_entries = $source -match 'base/assets/sandbox/manifest\.properties' -and $source -match 'base/assets/sandbox/maps\.json'
    cold_start = $source -match 'am.*start.*-W' -and $source -match 'ThisTime' -and $source -match 'cold_start'
    content_smoke = $source -match 'uiautomator' -and $source -match 'android\\\.view\\\.SurfaceView'
    report = $source -match 'proc-014-android-release-v1' -and $source -match 'ReportPath'
    version_policy = $build -match 'myengine\.versionCode' -and $build -match 'myengine\.versionName'
    application_id_policy = $build -match 'myengine\.applicationId' -and $policy -match 'dev\.myengine\.<game>'
    r8_enabled = $build -match 'isMinifyEnabled = true' -and $build -match 'proguard-rules\.pro'
    r8_keeps_content = $rules -match 'dev\.myengine\.content' -and $rules -match 'dev\.myengine\.games\.sandbox'
    no_secret_output = $source -match 'secrets_emitted = \$false'
}
foreach ($name in $requiredTokens.Keys) { if (-not $requiredTokens[$name]) { $failures += "missing_token_$name" } }

[ordered]@{
    agent = "me-android-release-test"
    verdict = if ($failures.Count -eq 0) { "pass" } else { "fail" }
    summary = if ($failures.Count -eq 0) { "PROC-014 release lane contract passed." } else { "PROC-014 release lane contract failed." }
    failures = $failures
    required_tokens = $requiredTokens
    blocked_exit_code = $first.exit_code
} | ConvertTo-Json -Compress -Depth 12
exit $(if ($failures.Count -eq 0) { 0 } else { 1 })
