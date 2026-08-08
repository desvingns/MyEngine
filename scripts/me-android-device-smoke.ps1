param(
    [string]$AvdName = "Pixel_5",
    [string]$Serial = "",
    [int]$BootTimeoutSeconds = 180,
    [string]$SdkRoot = "",
    [switch]$PreflightOnly,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

function Write-Result {
    param(
        [hashtable]$Result,
        [int]$ExitCode
    )

    $Result | ConvertTo-Json -Compress -Depth 12
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
    return [System.IO.Path]::GetFullPath((Join-Path $env:LOCALAPPDATA "Android\Sdk"))
}

function Get-ExistingAvdNames {
    param([string]$EmulatorPath)

    $raw = & $EmulatorPath -list-avds 2>$null | Out-String
    if ($LASTEXITCODE -ne 0) { return @() }
    return @($raw -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

function Get-DeviceSerial {
    param([string]$AdbPath)

    $raw = & $AdbPath devices 2>$null | Out-String
    foreach ($line in ($raw -split "`r?`n")) {
        if ($line -match "^(emulator-\d+)\s+device$") {
            return $Matches[1]
        }
    }
    return $null
}

function Get-Preflight {
    param([string]$ResolvedSdkRoot)

    $adbPath = Join-Path $ResolvedSdkRoot "platform-tools\adb.exe"
    $emulatorPath = Join-Path $ResolvedSdkRoot "emulator\emulator.exe"
    $gradlePath = Join-Path $root "gradlew.bat"
    $testManifest = Join-Path $root "android\src\androidTest\AndroidManifest.xml"
    $testSource = Join-Path $root "android\src\androidTest\kotlin\dev\myengine\android\AndroidSmokeInstrumentationTest.kt"
    $avds = if (Test-Path -LiteralPath $emulatorPath -PathType Leaf) { Get-ExistingAvdNames $emulatorPath } else { @() }
    $checks = [ordered]@{
        sdk_root = [ordered]@{ path = $ResolvedSdkRoot; present = Test-Path -LiteralPath $ResolvedSdkRoot -PathType Container }
        adb = [ordered]@{ path = $adbPath; present = Test-Path -LiteralPath $adbPath -PathType Leaf }
        emulator = [ordered]@{ path = $emulatorPath; present = Test-Path -LiteralPath $emulatorPath -PathType Leaf }
        gradle_wrapper = [ordered]@{ path = $gradlePath; present = Test-Path -LiteralPath $gradlePath -PathType Leaf }
        instrumentation_manifest = [ordered]@{ path = $testManifest; present = Test-Path -LiteralPath $testManifest -PathType Leaf }
        instrumentation_source = [ordered]@{ path = $testSource; present = Test-Path -LiteralPath $testSource -PathType Leaf }
        avd = [ordered]@{ requested = $AvdName; available = $avds; present = $avds -contains $AvdName }
    }
    $missing = @($checks.Keys | Where-Object { $_ -ne "avd" -and -not $checks[$_].present })
    if (-not $checks.avd.present) { $missing += "avd" }
    return [ordered]@{
        checks = $checks
        missing = $missing
        pass = $missing.Count -eq 0
    }
}

$resolvedSdkRoot = Resolve-SdkRoot
$preflight = Get-Preflight $resolvedSdkRoot
$command = "powershell.exe -File scripts/me-android-device-smoke.ps1"

Push-Location $root
$startedEmulator = $false
$activeSerial = $Serial
try {
    if (-not $preflight.pass) {
        Write-Result @{
            agent = "me-android-device-smoke"
            status = "blocked"
            verdict = "blocked"
            summary = "Android device lane is unavailable in this environment."
            reason = "preflight_failed"
            command = $command
            exit_code = 2
            avd = $AvdName
            preflight = $preflight
            fallback = "Install the Android Emulator/system image or enable virtualization; the JVM assemble and contract lanes remain runnable."
        } 2
    }

    if ($PreflightOnly) {
        Write-Result @{
            agent = "me-android-device-smoke"
            status = "pass"
            verdict = "pass"
            summary = "Android device lane preflight passed; execution was not requested."
            command = $command
            exit_code = 0
            avd = $AvdName
            preflight = $preflight
            execution = "not_run"
        } 0
    }

    $adbPath = Join-Path $resolvedSdkRoot "platform-tools\adb.exe"
    $emulatorPath = Join-Path $resolvedSdkRoot "emulator\emulator.exe"
    if ([string]::IsNullOrWhiteSpace($activeSerial)) {
        $activeSerial = Get-DeviceSerial $adbPath
    }
    if ([string]::IsNullOrWhiteSpace($activeSerial)) {
        $activeSerial = "emulator-5554"
        $emulator = Start-Process -FilePath $emulatorPath -ArgumentList @(
            "-avd", $AvdName,
            "-port", "5554",
            "-no-window",
            "-no-audio",
            "-no-boot-anim",
            "-no-snapshot",
            "-gpu", "swiftshader_indirect"
        ) -WindowStyle Hidden -PassThru
        $startedEmulator = $true
    }

    for ($second = 0; $second -lt $BootTimeoutSeconds; $second++) {
        $deviceState = (& $adbPath -s $activeSerial get-state 2>$null | Out-String).Trim()
        if ($deviceState -eq "device") {
            $bootState = (& $adbPath -s $activeSerial shell getprop sys.boot_completed 2>$null | Out-String).Trim()
            if ($bootState -eq "1") { break }
        }
        if ($second -eq ($BootTimeoutSeconds - 1)) {
            Write-Result @{
                agent = "me-android-device-smoke"
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
                fallback = "Check virtualization/Hyper-V availability and rerun the same command with a longer -BootTimeoutSeconds."
            } 2
        }
        Start-Sleep -Seconds 1
    }

    if (-not $SkipBuild) {
        $buildRaw = & .\gradlew.bat --quiet :android:assembleDebug :android:assembleDebugAndroidTest 2>&1 | Out-String
        $buildExitCode = $LASTEXITCODE
        if ($buildExitCode -ne 0) {
            Write-Result @{
                agent = "me-android-device-smoke"
                status = "fail"
                verdict = "fail"
                summary = "Android debug and instrumentation APK build failed."
                reason = "build_failed"
                command = "$command :android:assembleDebug :android:assembleDebugAndroidTest"
                exit_code = $buildExitCode
                serial = $activeSerial
                build_output_tail = (($buildRaw -split "`r?`n" | Where-Object { $_ }) | Select-Object -Last 12)
            } 1
        }
    }

    $appApk = Get-ChildItem -LiteralPath (Join-Path $root "android\build\outputs\apk\debug") -Filter "*.apk" -File | Select-Object -First 1
    $testApk = Get-ChildItem -LiteralPath (Join-Path $root "android\build\outputs\apk\androidTest\debug") -Filter "*.apk" -File | Select-Object -First 1
    if ($null -eq $appApk -or $null -eq $testApk) {
        Write-Result @{
            agent = "me-android-device-smoke"
            status = "fail"
            verdict = "fail"
            summary = "The Android and instrumentation APK outputs were not found."
            reason = "apk_missing"
            command = $command
            exit_code = 1
            serial = $activeSerial
        } 1
    }

    & $adbPath -s $activeSerial install -r $appApk.FullName 2>&1 | Out-Null
    $installAppExitCode = $LASTEXITCODE
    & $adbPath -s $activeSerial install -r $testApk.FullName 2>&1 | Out-Null
    $installTestExitCode = $LASTEXITCODE
    if ($installAppExitCode -ne 0 -or $installTestExitCode -ne 0) {
        Write-Result @{
            agent = "me-android-device-smoke"
            status = "fail"
            verdict = "fail"
            summary = "Installing the Android smoke APKs failed."
            reason = "install_failed"
            command = $command
            exit_code = 1
            serial = $activeSerial
            install_app_exit_code = $installAppExitCode
            install_test_exit_code = $installTestExitCode
        } 1
    }

    $instrumentation = & $adbPath -s $activeSerial shell am instrument -w -r dev.myengine.android.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | Out-String
    $instrumentationExitCode = $LASTEXITCODE
    $passed = $instrumentationExitCode -eq 0 -and $instrumentation -match "OK \(1 test\)"
    $verdict = if ($passed) { "pass" } else { "fail" }
    $status = $verdict
    $resultExitCode = if ($passed) { 0 } else { 1 }
    Write-Result @{
        agent = "me-android-device-smoke"
        status = $status
        verdict = $verdict
        summary = if ($passed) { "Android instrumentation smoke passed." } else { "Android instrumentation smoke failed." }
        command = $command
        exit_code = $resultExitCode
        avd = $AvdName
        serial = $activeSerial
        started_emulator = $startedEmulator
        instrumentation_exit_code = $instrumentationExitCode
        instrumentation_output_tail = (($instrumentation -split "`r?`n" | Where-Object { $_ }) | Select-Object -Last 16)
    } $resultExitCode
} finally {
    if ($startedEmulator -and -not [string]::IsNullOrWhiteSpace($activeSerial)) {
        $adbPath = Join-Path $resolvedSdkRoot "platform-tools\adb.exe"
        if (Test-Path -LiteralPath $adbPath -PathType Leaf) {
            & $adbPath -s $activeSerial emu kill 2>$null | Out-Null
        }
    }
    Pop-Location
}
