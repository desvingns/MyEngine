param(
    [string]$AvdName = "Pixel_5",
    [string]$Serial = "",
    [string]$SdkRoot = "",
    [int]$BootTimeoutSeconds = 180,
    [int]$ActivityReadyTimeoutSeconds = 15,
    [switch]$SkipBuild,
    [switch]$PreflightOnly,
    [string]$GoldenPath = "artifacts\android-visual-smoke\golden.png",
    [string]$CapturePath = "artifacts\android-visual-smoke\capture.png",
    [switch]$UpdateGolden,
    [string]$GoldenUpdateReason = "",
    [int]$ChannelTolerance = 8,
    [double]$AllowedDifferenceRatio = 0.005,
    [int]$IgnoreTopPixels = 80,
    [int]$IgnoreBottomPixels = 120
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$root = Split-Path -Parent $PSScriptRoot
$command = "powershell.exe -File scripts/me-android-visual-smoke.ps1"

function Write-Result {
    param(
        [object]$Result,
        [int]$ExitCode
    )

    $Result | ConvertTo-Json -Compress -Depth 16
    exit $ExitCode
}

function Resolve-SdkRoot {
    if (-not [string]::IsNullOrWhiteSpace($SdkRoot)) {
        return [System.IO.Path]::GetFullPath($SdkRoot)
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) {
        return [System.IO.Path]::GetFullPath($env:ANDROID_HOME)
    }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) {
        return [System.IO.Path]::GetFullPath($env:ANDROID_SDK_ROOT)
    }
    $localAppData = [Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)
    return [System.IO.Path]::GetFullPath((Join-Path $localAppData "Android\Sdk"))
}

function Resolve-WorkspacePath {
    param([string]$PathValue)

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }
    return [System.IO.Path]::GetFullPath((Join-Path $root $PathValue))
}

function Get-ExistingAvdNames {
    param([string]$EmulatorPath)

    if (-not (Test-Path -LiteralPath $EmulatorPath -PathType Leaf)) { return @() }
    $raw = & $EmulatorPath -list-avds 2>$null | Out-String
    if ($LASTEXITCODE -ne 0) { return @() }
    return @($raw -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Sort-Object -Unique)
}

function Test-ImageDecoder {
    try {
        Add-Type -AssemblyName System.Drawing -ErrorAction Stop
        $bitmap = [System.Drawing.Bitmap]::new(1, 1)
        $bitmap.Dispose()
        return [ordered]@{ present = $true; reason = "available" }
    } catch {
        return [ordered]@{ present = $false; reason = "system_drawing_unavailable" }
    }
}

function Get-Preflight {
    param([string]$ResolvedSdkRoot)

    $adbPath = Join-Path $ResolvedSdkRoot "platform-tools\adb.exe"
    $emulatorPath = Join-Path $ResolvedSdkRoot "emulator\emulator.exe"
    $gradlePath = Join-Path $root "gradlew.bat"
    $avds = Get-ExistingAvdNames $emulatorPath
    $decoder = Test-ImageDecoder
    $checks = [ordered]@{
        sdk_root = [ordered]@{ path = $ResolvedSdkRoot; present = Test-Path -LiteralPath $ResolvedSdkRoot -PathType Container }
        adb = [ordered]@{ path = $adbPath; present = Test-Path -LiteralPath $adbPath -PathType Leaf }
        emulator = [ordered]@{ path = $emulatorPath; present = Test-Path -LiteralPath $emulatorPath -PathType Leaf }
        gradle_wrapper = [ordered]@{ path = $gradlePath; present = Test-Path -LiteralPath $gradlePath -PathType Leaf }
        avd = [ordered]@{ requested = $AvdName; available = @($avds); present = @($avds) -contains $AvdName }
        image_decoder = $decoder
    }
    $missing = @()
    foreach ($name in @("sdk_root", "adb", "emulator", "gradle_wrapper", "image_decoder")) {
        if (-not $checks[$name].present) { $missing += $name }
    }
    if (-not $checks.avd.present) { $missing += "avd" }
    return [ordered]@{
        checks = $checks
        missing = $missing
        pass = $missing.Count -eq 0
    }
}

function Invoke-AdbText {
    param(
        [string]$AdbPath,
        [string[]]$Arguments
    )

    # Windows PowerShell promotes native stderr (including successful adb pull progress) to
    # NativeCommandError when ErrorActionPreference is Stop. Capture both streams while keeping
    # the lane strict on the process exit code, then restore the caller's preference.
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & $AdbPath @Arguments 2>&1 | Out-String
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    return [ordered]@{
        exit_code = $exitCode
        output = $output.Trim()
    }
}

function Get-DeviceSerial {
    param([string]$AdbPath)

    $result = Invoke-AdbText $AdbPath @("devices")
    if ($result.exit_code -ne 0) { return $null }
    foreach ($line in ($result.output -split "`r?`n" | Sort-Object)) {
        if ($line -match "^(emulator-\d+)\s+device$") {
            return $Matches[1]
        }
    }
    return $null
}

function Test-VisualActivityFocused {
    param(
        [string]$AdbPath,
        [string]$DeviceSerial
    )

    $activities = Invoke-AdbText $AdbPath @(
        "-s", $DeviceSerial,
        "shell", "dumpsys", "activity", "activities"
    )
    return $activities.exit_code -eq 0 -and
        $activities.output -match "dev\.myengine\.android/\.MyEngineActivity"
}

function Test-VisualSurfaceReady {
    param(
        [string]$AdbPath,
        [string]$DeviceSerial
    )

    $dump = Invoke-AdbText $AdbPath @(
        "-s", $DeviceSerial,
        "shell", "uiautomator", "dump", "/sdcard/me_android_visual_smoke_ui.xml"
    )
    if ($dump.exit_code -ne 0) { return $false }
    $xml = Invoke-AdbText $AdbPath @(
        "-s", $DeviceSerial,
        "shell", "cat", "/sdcard/me_android_visual_smoke_ui.xml"
    )
    return $xml.exit_code -eq 0 -and $xml.output -match 'class="android\.view\.SurfaceView"'
}

function Compare-Images {
    param(
        [string]$ActualPath,
        [string]$ExpectedPath
    )

    $actual = $null
    $expected = $null
    try {
        $actual = [System.Drawing.Bitmap]::new($ActualPath)
        $expected = [System.Drawing.Bitmap]::new($ExpectedPath)
        if ($actual.Width -ne $expected.Width -or $actual.Height -ne $expected.Height) {
            return [ordered]@{
                pass = $false
                reason = "dimension_mismatch"
                actual_width = $actual.Width
                actual_height = $actual.Height
                golden_width = $expected.Width
                golden_height = $expected.Height
            }
        }

        $top = [Math]::Max(0, $IgnoreTopPixels)
        $bottom = [Math]::Max(0, $IgnoreBottomPixels)
        $firstRow = $top
        $lastRow = $actual.Height - $bottom - 1
        if ($firstRow -gt $lastRow) {
            return [ordered]@{
                pass = $false
                reason = "ignored_chrome_exceeds_image"
                compared_pixels = 0
                different_pixels = 0
                difference_ratio = 1.0
            }
        }

        [long]$differentPixels = 0
        [long]$comparedPixels = 0
        for ($y = $firstRow; $y -le $lastRow; $y++) {
            for ($x = 0; $x -lt $actual.Width; $x++) {
                $actualPixel = $actual.GetPixel($x, $y)
                $expectedPixel = $expected.GetPixel($x, $y)
                $different =
                    ([Math]::Abs($actualPixel.R - $expectedPixel.R) -gt $ChannelTolerance) -or
                    ([Math]::Abs($actualPixel.G - $expectedPixel.G) -gt $ChannelTolerance) -or
                    ([Math]::Abs($actualPixel.B - $expectedPixel.B) -gt $ChannelTolerance)
                if ($different) { $differentPixels++ }
                $comparedPixels++
            }
        }
        $ratio = if ($comparedPixels -eq 0) { 1.0 } else { [double]$differentPixels / [double]$comparedPixels }
        return [ordered]@{
            pass = $ratio -le $AllowedDifferenceRatio
            reason = if ($ratio -le $AllowedDifferenceRatio) { "within_tolerance" } else { "difference_ratio_exceeded" }
            width = $actual.Width
            height = $actual.Height
            ignored_top_pixels = $top
            ignored_bottom_pixels = $bottom
            compared_pixels = $comparedPixels
            different_pixels = $differentPixels
            difference_ratio = $ratio
            channel_tolerance = $ChannelTolerance
            allowed_difference_ratio = $AllowedDifferenceRatio
        }
    } catch {
        return [ordered]@{
            pass = $false
            reason = "image_decode_failed"
        }
    } finally {
        if ($null -ne $actual) { $actual.Dispose() }
        if ($null -ne $expected) { $expected.Dispose() }
    }
}

if ($UpdateGolden -and [string]::IsNullOrWhiteSpace($GoldenUpdateReason)) {
    Write-Result ([ordered]@{
        agent = "me-android-visual-smoke"
        status = "fail"
        verdict = "fail"
        summary = "Golden updates require a nonblank reason."
        reason = "golden_update_reason_required"
        command = $command
        exit_code = 1
    }) 1
}

if ($BootTimeoutSeconds -le 0 -or $ActivityReadyTimeoutSeconds -le 0 -or
    $ChannelTolerance -lt 0 -or $ChannelTolerance -gt 255 -or
    $AllowedDifferenceRatio -lt 0.0 -or $AllowedDifferenceRatio -gt 1.0 -or
    [double]::IsNaN($AllowedDifferenceRatio) -or [double]::IsInfinity($AllowedDifferenceRatio) -or
    $IgnoreTopPixels -lt 0 -or $IgnoreBottomPixels -lt 0) {
    Write-Result ([ordered]@{
        agent = "me-android-visual-smoke"
        status = "fail"
        verdict = "fail"
        summary = "Visual smoke parameters are invalid."
        reason = "invalid_parameters"
        command = $command
        exit_code = 1
    }) 1
}

$resolvedSdkRoot = Resolve-SdkRoot
$goldenAbsolutePath = Resolve-WorkspacePath $GoldenPath
$captureAbsolutePath = Resolve-WorkspacePath $CapturePath
$preflight = Get-Preflight $resolvedSdkRoot
$startedEmulator = $false
$activeSerial = $Serial
$adbPath = Join-Path $resolvedSdkRoot "platform-tools\adb.exe"

Push-Location $root
try {
    if (-not $preflight.pass) {
        $reason = if ($preflight.missing -contains "image_decoder") { "image_decoder_unavailable" } else { "preflight_failed" }
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "blocked"
            verdict = "blocked"
            summary = "Android visual smoke lane is unavailable in this environment."
            reason = $reason
            command = $command
            exit_code = 2
            avd = $AvdName
            preflight = $preflight
            fallback = "Install the Android SDK/emulator and Pixel_5 system image, or run the contract lane without a device."
        }) 2
    }

    if ($PreflightOnly) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "pass"
            verdict = "pass"
            summary = "Android visual smoke preflight passed; execution was not requested."
            command = $command
            exit_code = 0
            avd = $AvdName
            preflight = $preflight
            execution = "not_run"
        }) 0
    }

    if ([string]::IsNullOrWhiteSpace($activeSerial)) {
        $activeSerial = Get-DeviceSerial $adbPath
    }
    if ([string]::IsNullOrWhiteSpace($activeSerial)) {
        $activeSerial = "emulator-5554"
        try {
            $null = Start-Process -FilePath (Join-Path $resolvedSdkRoot "emulator\emulator.exe") -ArgumentList @(
                "-avd", $AvdName,
                "-port", "5554",
                "-no-window",
                "-no-audio",
                "-no-boot-anim",
                "-no-snapshot",
                "-gpu", "swiftshader_indirect"
            ) -WindowStyle Hidden -PassThru
            $startedEmulator = $true
        } catch {
            Write-Result ([ordered]@{
                agent = "me-android-visual-smoke"
                status = "blocked"
                verdict = "blocked"
                summary = "The Pixel_5 emulator could not be started."
                reason = "emulator_start_failed"
                command = $command
                exit_code = 2
                avd = $AvdName
                preflight = $preflight
            }) 2
        }
    }

    $bootReady = $false
    for ($second = 0; $second -lt $BootTimeoutSeconds; $second++) {
        $deviceState = (Invoke-AdbText $adbPath @("-s", $activeSerial, "get-state")).output
        if ($deviceState -eq "device") {
            $bootState = (Invoke-AdbText $adbPath @("-s", $activeSerial, "shell", "getprop", "sys.boot_completed")).output
            if ($bootState -eq "1") {
                $bootReady = $true
                break
            }
        }
        if ($second + 1 -lt $BootTimeoutSeconds) { Start-Sleep -Seconds 1 }
    }
    if (-not $bootReady) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "blocked"
            verdict = "blocked"
            summary = "The emulator did not reach sys.boot_completed before the timeout."
            reason = "boot_timeout"
            command = $command
            exit_code = 2
            avd = $AvdName
            serial = $activeSerial
            started_emulator = $startedEmulator
            preflight = $preflight
        }) 2
    }

    if (-not $SkipBuild) {
        if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or -not (Test-Path -LiteralPath $env:JAVA_HOME -PathType Container)) {
            $androidStudioJbr = Join-Path ${env:ProgramFiles} "Android\Android Studio\jbr"
            if (Test-Path -LiteralPath $androidStudioJbr -PathType Container) {
                $env:JAVA_HOME = $androidStudioJbr
            }
        }
        $previousErrorActionPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = "Continue"
            $buildOutput = & .\gradlew.bat --quiet :android:assembleDebug 2>&1 | Out-String
            $buildExitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        if ($buildExitCode -ne 0) {
            Write-Result ([ordered]@{
                agent = "me-android-visual-smoke"
                status = "fail"
                verdict = "fail"
                summary = "The Android debug APK build failed."
                reason = "build_failed"
                command = "$command :android:assembleDebug"
                exit_code = 1
                build_exit_code = $buildExitCode
                build_output_tail = (($buildOutput -split "`r?`n" | Where-Object { $_ }) | Select-Object -Last 12)
            }) 1
        }
    }

    $apkDirectory = Join-Path $root "android\build\outputs\apk\debug"
    $appApk = Get-ChildItem -LiteralPath $apkDirectory -Filter "*.apk" -File -ErrorAction SilentlyContinue | Sort-Object FullName | Select-Object -First 1
    if ($null -eq $appApk) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "The Android debug APK output was not found."
            reason = "apk_missing"
            command = $command
            exit_code = 1
        }) 1
    }

    $install = Invoke-AdbText $adbPath @("-s", $activeSerial, "install", "-r", $appApk.FullName)
    if ($install.exit_code -ne 0) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "Installing the Android debug APK failed."
            reason = "install_failed"
            command = $command
            exit_code = 1
            serial = $activeSerial
            install_exit_code = $install.exit_code
        }) 1
    }

    $forceStop = Invoke-AdbText $adbPath @("-s", $activeSerial, "shell", "am", "force-stop", "dev.myengine.android")
    $launch = Invoke-AdbText $adbPath @(
        "-s", $activeSerial,
        "shell", "am", "start", "-W",
        "-n", "dev.myengine.android/.MyEngineActivity",
        "--ez", "me_visual_smoke", "true"
    )
    if ($forceStop.exit_code -ne 0 -or $launch.exit_code -ne 0 -or $launch.output -notmatch "Status: ok") {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "Launching the visual smoke Activity failed."
            reason = "launch_failed"
            command = $command
            exit_code = 1
            serial = $activeSerial
            force_stop_exit_code = $forceStop.exit_code
            launch_exit_code = $launch.exit_code
            launch_output_tail = (($launch.output -split "`r?`n" | Where-Object { $_ }) | Select-Object -Last 8)
        }) 1
    }

    $activityReady = $false
    for ($second = 0; $second -lt $ActivityReadyTimeoutSeconds; $second++) {
        if ((Test-VisualActivityFocused $adbPath $activeSerial) -and
            (Test-VisualSurfaceReady $adbPath $activeSerial)) {
            $activityReady = $true
            break
        }
        if ($second + 1 -lt $ActivityReadyTimeoutSeconds) { Start-Sleep -Seconds 1 }
    }
    if (-not $activityReady) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "The visual smoke Activity did not publish a focused SurfaceView."
            reason = "surface_not_ready"
            command = $command
            exit_code = 1
            serial = $activeSerial
            activity_ready_timeout_seconds = $ActivityReadyTimeoutSeconds
        }) 1
    }

    # SurfaceView can report its hierarchy before the first hardware buffer is visible.
    Start-Sleep -Milliseconds 2000
    $remoteCapturePath = "/sdcard/me_android_visual_smoke.png"
    $screencap = Invoke-AdbText $adbPath @("-s", $activeSerial, "shell", "screencap", "-p", $remoteCapturePath)
    if ($screencap.exit_code -ne 0) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "Capturing the Android screenshot failed."
            reason = "capture_failed"
            command = $command
            exit_code = 1
            serial = $activeSerial
            screencap_exit_code = $screencap.exit_code
        }) 1
    }

    $captureParent = Split-Path -Parent $captureAbsolutePath
    if (-not [string]::IsNullOrWhiteSpace($captureParent)) { $null = New-Item -ItemType Directory -Path $captureParent -Force }
    $pull = Invoke-AdbText $adbPath @("-s", $activeSerial, "pull", $remoteCapturePath, $captureAbsolutePath)
    if ($pull.exit_code -ne 0 -or -not (Test-Path -LiteralPath $captureAbsolutePath -PathType Leaf)) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "The Android screenshot could not be pulled as a PNG."
            reason = "capture_pull_failed"
            command = $command
            exit_code = 1
            serial = $activeSerial
            pull_exit_code = $pull.exit_code
            capture_path = $captureAbsolutePath
        }) 1
    }

    $goldenUpdated = $false
    if ($UpdateGolden) {
        $goldenParent = Split-Path -Parent $goldenAbsolutePath
        if (-not [string]::IsNullOrWhiteSpace($goldenParent)) { $null = New-Item -ItemType Directory -Path $goldenParent -Force }
        try {
            Copy-Item -LiteralPath $captureAbsolutePath -Destination $goldenAbsolutePath -Force -ErrorAction Stop
            $goldenUpdated = $true
        } catch {
            Write-Result ([ordered]@{
                agent = "me-android-visual-smoke"
                status = "fail"
                verdict = "fail"
                summary = "Updating the Android visual golden failed."
                reason = "golden_update_failed"
                command = $command
                exit_code = 1
                golden_path = $goldenAbsolutePath
            }) 1
        }
    }

    if (-not (Test-Path -LiteralPath $goldenAbsolutePath -PathType Leaf)) {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "The Android visual golden image was not found."
            reason = "golden_missing"
            command = $command
            exit_code = 1
            golden_path = $goldenAbsolutePath
        }) 1
    }

    $comparison = Compare-Images $captureAbsolutePath $goldenAbsolutePath
    if ($comparison.reason -eq "image_decode_failed") {
        Write-Result ([ordered]@{
            agent = "me-android-visual-smoke"
            status = "fail"
            verdict = "fail"
            summary = "The Android capture or golden image could not be decoded."
            reason = "image_decode_failed"
            command = $command
            exit_code = 1
            capture_path = $captureAbsolutePath
            golden_path = $goldenAbsolutePath
        }) 1
    }

    $passed = [bool]$comparison.pass
    Write-Result ([ordered]@{
        agent = "me-android-visual-smoke"
        status = if ($passed) { "pass" } else { "fail" }
        verdict = if ($passed) { "pass" } else { "fail" }
        summary = if ($passed) { "Android visual smoke passed." } else { "Android visual golden comparison failed." }
        reason = $comparison.reason
        command = $command
        exit_code = if ($passed) { 0 } else { 1 }
        avd = $AvdName
        serial = $activeSerial
        started_emulator = $startedEmulator
        capture_path = $captureAbsolutePath
        golden_path = $goldenAbsolutePath
        golden_updated = $goldenUpdated
        golden_update_reason = if ($goldenUpdated) { $GoldenUpdateReason.Trim() } else { "" }
        comparison = $comparison
    }) $(if ($passed) { 0 } else { 1 })
} catch {
    Write-Result ([ordered]@{
        agent = "me-android-visual-smoke"
        status = "fail"
        verdict = "fail"
        summary = "The Android visual smoke lane encountered an unexpected failure."
        reason = "unexpected_failure"
        command = $command
        exit_code = 1
        error = $_.Exception.Message
        error_type = $_.Exception.GetType().FullName
    }) 1
} finally {
    if ($startedEmulator -and -not [string]::IsNullOrWhiteSpace($activeSerial) -and (Test-Path -LiteralPath $adbPath -PathType Leaf)) {
        $null = & $adbPath -s $activeSerial emu kill 2>$null
    }
    Pop-Location
}
