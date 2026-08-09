$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$scriptPath = Join-Path $root "scripts\me-android-visual-smoke.ps1"
$activityPath = Join-Path $root "android\src\main\kotlin\dev\myengine\android\MyEngineActivity.kt"
$source = Get-Content -Raw -LiteralPath $scriptPath
$activitySource = Get-Content -Raw -LiteralPath $activityPath

function Invoke-Lane {
    param([string[]]$Arguments)

    $raw = & powershell.exe -NoProfile -File $scriptPath @Arguments 2>&1 | Out-String
    $exitCode = $LASTEXITCODE
    $lines = @($raw -split "`r?`n" | Where-Object { $_.Trim() })
    $json = $null
    if ($lines.Count -eq 1) {
        try { $json = $lines[0].Trim() | ConvertFrom-Json -ErrorAction Stop } catch { }
    }
    return [ordered]@{
        exit_code = $exitCode
        lines = $lines
        json = $json
        raw = if ($lines.Count -eq 1) { $lines[0].Trim() } else { $raw.Trim() }
    }
}

$missingSdk = Join-Path $root "__proc009_missing_sdk_contract__"
$firstRun = Invoke-Lane @("-SdkRoot", $missingSdk, "-PreflightOnly")
$secondRun = Invoke-Lane @("-SdkRoot", $missingSdk, "-PreflightOnly")
$reasonGuard = Invoke-Lane @("-SdkRoot", $missingSdk, "-UpdateGolden")
$whitespaceReasonGuard = Invoke-Lane @(
    "-SdkRoot", $missingSdk,
    "-UpdateGolden",
    "-GoldenUpdateReason", "   "
)
$invalidParameters = Invoke-Lane @(
    "-SdkRoot", $missingSdk,
    "-ChannelTolerance", "256"
)

$failures = @()
if ($firstRun.lines.Count -ne 1 -or $secondRun.lines.Count -ne 1) { $failures += "expected_one_json_line_per_blocked_run" }
if ($firstRun.exit_code -ne 2 -or $secondRun.exit_code -ne 2) { $failures += "expected_blocked_exit_code_2" }
if ($null -eq $firstRun.json -or $null -eq $secondRun.json) { $failures += "expected_parseable_blocked_json" }
if ($null -ne $firstRun.json -and $firstRun.json.verdict -ne "blocked") { $failures += "expected_blocked_verdict" }
if ($null -ne $firstRun.json -and $firstRun.json.status -ne "blocked") { $failures += "expected_blocked_status" }
if ($null -ne $firstRun.json -and @("preflight_failed", "image_decoder_unavailable") -notcontains $firstRun.json.reason) { $failures += "expected_preflight_reason" }
if ($firstRun.raw -ne $secondRun.raw) { $failures += "nondeterministic_blocked_output" }
if ($null -ne $firstRun.json -and $firstRun.json.command -ne "powershell.exe -File scripts/me-android-visual-smoke.ps1") { $failures += "missing_blocked_command" }
if ($null -ne $firstRun.json -and $null -eq $firstRun.json.preflight) { $failures += "missing_blocked_preflight_payload" }
if ($null -ne $firstRun.json -and $null -eq $firstRun.json.preflight.missing) { $failures += "missing_blocked_preflight_missing_list" }
if ($reasonGuard.lines.Count -ne 1 -or $reasonGuard.exit_code -ne 1) { $failures += "expected_empty_reason_guard_failure" }
if ($whitespaceReasonGuard.lines.Count -ne 1 -or $whitespaceReasonGuard.exit_code -ne 1) { $failures += "expected_whitespace_reason_guard_failure" }
if ($null -eq $reasonGuard.json -or $reasonGuard.json.reason -ne "golden_update_reason_required") { $failures += "missing_empty_reason_guard" }
if ($null -eq $whitespaceReasonGuard.json -or $whitespaceReasonGuard.json.reason -ne "golden_update_reason_required") { $failures += "missing_whitespace_reason_guard" }
if ($reasonGuard.raw -ne $whitespaceReasonGuard.raw) { $failures += "nondeterministic_reason_guard_output" }
if ($invalidParameters.lines.Count -ne 1 -or $invalidParameters.exit_code -ne 1) { $failures += "expected_invalid_parameter_failure" }
if ($null -eq $invalidParameters.json -or $invalidParameters.json.reason -ne "invalid_parameters") { $failures += "missing_invalid_parameter_reason" }

$requiredTokens = [ordered]@{
    activity_extra = $source -match '--ez",\s*"me_visual_smoke",\s*"true"'
    activity_hook = $activitySource -match 'getBooleanExtra\(VISUAL_SMOKE_EXTRA,\s*false\)' -and
        $activitySource -match 'if \(!visualSmokeMode\) \{\s*val ticks = fixedTickLoop\.advance' -and
        $activitySource -match 'activeSession\.step\(\)'
    launch_activity = $source -match '"shell",\s*"am",\s*"start",\s*"-W"' -and
        $source -match '"-n",\s*"dev\.myengine\.android/\.MyEngineActivity"'
    surface_ready = $source -match 'uiautomator' -and $source -match 'SurfaceView'
    screenshot_png = $source -match '"shell",\s*"screencap",\s*"-p",\s*\$remoteCapturePath'
    binary_safe_pull = $source -match '"pull",\s*\$remoteCapturePath,\s*\$captureAbsolutePath' -and
        $source -match 'Test-Path -LiteralPath \$captureAbsolutePath -PathType Leaf'
    image_compare = $source -match 'Compare-Images \$captureAbsolutePath \$goldenAbsolutePath' -and
        $source -match 'Test-Path -LiteralPath \$goldenAbsolutePath -PathType Leaf'
    png_decode = $source -match '\[System\.Drawing\.Bitmap\]::new\(\$ActualPath\)' -and
        $source -match '\[System\.Drawing\.Bitmap\]::new\(\$ExpectedPath\)'
    dimension_guard = $source -match '\$actual\.Width -ne \$expected\.Width' -and
        $source -match 'reason = "dimension_mismatch"'
    chrome_top = $source -match '\$IgnoreTopPixels' -and $source -match 'ignored_top_pixels'
    chrome_bottom = $source -match '\$IgnoreBottomPixels' -and $source -match 'ignored_bottom_pixels'
    channel_tolerance = $source -match '\$ChannelTolerance' -and $source -match 'channel_tolerance'
    allowed_ratio = $source -match '\$AllowedDifferenceRatio' -and $source -match 'allowed_difference_ratio'
    golden_update = $source -match '\$UpdateGolden' -and $source -match '\$GoldenUpdateReason' -and
        $source -match 'golden_update_reason_required'
}
foreach ($name in $requiredTokens.Keys) {
    if (-not $requiredTokens[$name]) { $failures += "missing_token_$name" }
}

$defaultParameters = [ordered]@{
    avd_name = $source -match '\[string\]\$AvdName\s*=\s*"Pixel_5"'
    boot_timeout_seconds = $source -match '\[int\]\$BootTimeoutSeconds\s*=\s*180'
    golden_path = $source -match '\[string\]\$GoldenPath\s*=\s*"artifacts\\android-visual-smoke\\golden\.png"'
    capture_path = $source -match '\[string\]\$CapturePath\s*=\s*"artifacts\\android-visual-smoke\\capture\.png"'
    channel_tolerance = $source -match '\[int\]\$ChannelTolerance\s*=\s*8'
    allowed_difference_ratio = $source -match '\[double\]\$AllowedDifferenceRatio\s*=\s*0\.005'
    ignore_top_pixels = $source -match '\[int\]\$IgnoreTopPixels\s*=\s*80'
    ignore_bottom_pixels = $source -match '\[int\]\$IgnoreBottomPixels\s*=\s*120'
}
foreach ($name in $defaultParameters.Keys) {
    if (-not $defaultParameters[$name]) { $failures += "missing_default_$name" }
}

$result = [ordered]@{
    agent = "me-android-visual-smoke-test"
    verdict = if ($failures.Count -eq 0) { "pass" } else { "fail" }
    summary = if ($failures.Count -eq 0) { "PROC-009 deterministic visual smoke contracts passed." } else { "PROC-009 visual smoke contract failed." }
    failures = $failures
    blocked_exit_code = $firstRun.exit_code
    reason_guard_exit_code = $reasonGuard.exit_code
    required_tokens = $requiredTokens
    default_parameters = $defaultParameters
}
$result | ConvertTo-Json -Compress -Depth 12
exit $(if ($failures.Count -eq 0) { 0 } else { 1 })
