param(
    [string]$KeystorePropertiesPath = "keystore.properties",
    [string]$ApplicationId = "dev.myengine.android",
    [string]$VersionCode = "1",
    [string]$VersionName = "0.0.1",
    [string]$SdkRoot = "",
    [string]$Serial = "",
    [string]$AvdName = "Pixel_5",
    [int]$BootTimeoutSeconds = 180,
    [ValidateRange(1, 10)]
    [int]$ColdStartRuns = 3,
    [string]$ReportPath = "reports\android-release\proc-014-release.json",
    [switch]$PreflightOnly,
    [switch]$SkipBuild,
    [switch]$SkipDevice
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$root = Split-Path -Parent $PSScriptRoot
$command = "powershell.exe -File scripts/me-android-release.ps1"

function Write-Result {
    param([object]$Result, [int]$ExitCode)

    $Result | ConvertTo-Json -Compress -Depth 20
    exit $ExitCode
}

function Resolve-WorkspacePath {
    param([string]$PathValue)

    if ([System.IO.Path]::IsPathRooted($PathValue)) {
        return [System.IO.Path]::GetFullPath($PathValue)
    }
    return [System.IO.Path]::GetFullPath((Join-Path $root $PathValue))
}

function Resolve-SdkRoot {
    if (-not [string]::IsNullOrWhiteSpace($SdkRoot)) { return [System.IO.Path]::GetFullPath($SdkRoot) }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_HOME)) { return [System.IO.Path]::GetFullPath($env:ANDROID_HOME) }
    if (-not [string]::IsNullOrWhiteSpace($env:ANDROID_SDK_ROOT)) { return [System.IO.Path]::GetFullPath($env:ANDROID_SDK_ROOT) }
    return [System.IO.Path]::GetFullPath((Join-Path ([Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)) "Android\Sdk"))
}

function Resolve-JavaHome {
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME) -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return [System.IO.Path]::GetFullPath($env:JAVA_HOME)
    }
    $candidate = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path -LiteralPath (Join-Path $candidate "bin\java.exe")) { return $candidate }
    return $null
}

function Read-Properties {
    param([string]$PathValue)

    $values = @{}
    if (-not (Test-Path -LiteralPath $PathValue -PathType Leaf)) { return $values }
    foreach ($line in Get-Content -LiteralPath $PathValue) {
        $trimmed = $line.Trim()
        if ($trimmed.Length -eq 0 -or $trimmed.StartsWith("#") -or $trimmed.StartsWith("!")) { continue }
        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) { continue }
        $key = $trimmed.Substring(0, $separator).Trim()
        $values[$key] = $trimmed.Substring($separator + 1).Trim()
    }
    return $values
}

function Resolve-KeystoreFile {
    param([string]$PropertiesFile, [string]$StoreFile)

    if ([string]::IsNullOrWhiteSpace($StoreFile)) { return $null }
    if ([System.IO.Path]::IsPathRooted($StoreFile)) { return [System.IO.Path]::GetFullPath($StoreFile) }
    return [System.IO.Path]::GetFullPath((Join-Path (Split-Path -Parent $PropertiesFile) $StoreFile))
}

function Invoke-NativeText {
    param([string]$FilePath, [string[]]$Arguments)

    $previous = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $output = & $FilePath @Arguments 2>&1 | Out-String
        return [ordered]@{ exit_code = $LASTEXITCODE; output = $output.Trim() }
    } finally {
        $ErrorActionPreference = $previous
    }
}

function Get-ExistingAvdNames {
    param([string]$EmulatorPath)

    if (-not (Test-Path -LiteralPath $EmulatorPath -PathType Leaf)) { return @() }
    $result = Invoke-NativeText $EmulatorPath @("-list-avds")
    if ($result.exit_code -ne 0) { return @() }
    return @($result.output -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Sort-Object -Unique)
}

function Get-Preflight {
    param([string]$ResolvedSdkRoot, [string]$ResolvedKeystorePath)

    $keystore = Read-Properties $ResolvedKeystorePath
    $storeFile = Resolve-KeystoreFile $ResolvedKeystorePath $keystore["storeFile"]
    $requiredKeys = @("storeFile", "storePassword", "keyAlias", "keyPassword")
    $missingKeys = @($requiredKeys | Where-Object { [string]::IsNullOrWhiteSpace($keystore[$_]) })
    if ($null -ne $storeFile -and -not (Test-Path -LiteralPath $storeFile -PathType Leaf)) { $missingKeys += "storeFile_exists" }
    $adbPath = Join-Path $ResolvedSdkRoot "platform-tools\adb.exe"
    $emulatorPath = Join-Path $ResolvedSdkRoot "emulator\emulator.exe"
    $checks = [ordered]@{
        gradle_wrapper = [ordered]@{ path = (Join-Path $root "gradlew.bat"); present = Test-Path -LiteralPath (Join-Path $root "gradlew.bat") -PathType Leaf }
        android_build = [ordered]@{ path = (Join-Path $root "android\build.gradle.kts"); present = Test-Path -LiteralPath (Join-Path $root "android\build.gradle.kts") -PathType Leaf }
        proguard_rules = [ordered]@{ path = (Join-Path $root "android\proguard-rules.pro"); present = Test-Path -LiteralPath (Join-Path $root "android\proguard-rules.pro") -PathType Leaf }
        keystore_properties = [ordered]@{ path = $ResolvedKeystorePath; present = Test-Path -LiteralPath $ResolvedKeystorePath -PathType Leaf; missing_keys = $missingKeys }
        keystore_file = [ordered]@{ path = $storeFile; present = $null -ne $storeFile -and (Test-Path -LiteralPath $storeFile -PathType Leaf) }
        sdk_root = [ordered]@{ path = $ResolvedSdkRoot; present = Test-Path -LiteralPath $ResolvedSdkRoot -PathType Container }
        adb = [ordered]@{ path = $adbPath; present = Test-Path -LiteralPath $adbPath -PathType Leaf }
        emulator = [ordered]@{ path = $emulatorPath; present = Test-Path -LiteralPath $emulatorPath -PathType Leaf }
        avd = [ordered]@{ requested = $AvdName; available = @(Get-ExistingAvdNames $emulatorPath); present = $false }
    }
    $checks.avd.present = @($checks.avd.available) -contains $AvdName
    $missing = @($checks.Keys | Where-Object {
        $_ -in @("gradle_wrapper", "android_build", "proguard_rules", "keystore_properties", "keystore_file") -and -not $checks[$_].present
    })
    if ($checks.keystore_properties.missing_keys.Count -gt 0 -and $missing -notcontains "keystore_properties") { $missing += "keystore_properties_keys" }
    if (-not $SkipDevice) {
        foreach ($name in @("sdk_root", "adb", "emulator", "avd")) { if (-not $checks[$name].present) { $missing += $name } }
    }
    return [ordered]@{ checks = $checks; missing = @($missing | Sort-Object -Unique); pass = @($missing).Count -eq 0 }
}

function Get-DeviceSerial {
    param([string]$AdbPath)

    $result = Invoke-NativeText $AdbPath @("devices")
    if ($result.exit_code -ne 0) { return $null }
    foreach ($line in ($result.output -split "`r?`n")) {
        if ($line -match "^(emulator-\d+)\s+device$") { return $Matches[1] }
    }
    return $null
}

function Wait-ForDevice {
    param([string]$AdbPath, [string]$DeviceSerial)

    for ($second = 0; $second -lt $BootTimeoutSeconds; $second++) {
        $state = (Invoke-NativeText $AdbPath @("-s", $DeviceSerial, "get-state")).output
        if ($state -eq "device") {
            $boot = (Invoke-NativeText $AdbPath @("-s", $DeviceSerial, "shell", "getprop", "sys.boot_completed")).output
            if ($boot -eq "1") { return $true }
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Read-AabMetrics {
    param([string]$AabPath)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [System.IO.Compression.ZipFile]::OpenRead($AabPath)
    try {
        $requiredEntries = @("base/assets/sandbox/manifest.properties", "base/assets/sandbox/maps.json")
        $entries = @($archive.Entries | ForEach-Object FullName)
        $missing = @($requiredEntries | Where-Object { $entries -notcontains $_ })
        return [ordered]@{
            path = $AabPath
            size_bytes = (Get-Item -LiteralPath $AabPath).Length
            required_content_entries = $requiredEntries
            missing_content_entries = $missing
            content_pack_present = $missing.Count -eq 0
        }
    } finally {
        $archive.Dispose()
    }
}

function Read-ColdStartMetrics {
    param([string]$AdbPath, [string]$DeviceSerial)

    $samples = @()
    $activity = "$ApplicationId/.MyEngineActivity"
    for ($index = 0; $index -lt $ColdStartRuns; $index++) {
        $null = Invoke-NativeText $AdbPath @("-s", $DeviceSerial, "shell", "am", "force-stop", $ApplicationId)
        $start = Invoke-NativeText $AdbPath @("-s", $DeviceSerial, "shell", "am", "start", "-W", "-n", $activity)
        if ($start.exit_code -ne 0 -or $start.output -match "Error:") { throw "Release activity failed to start: $($start.output)" }
        $match = [regex]::Match($start.output, "(?m)^ThisTime:\s*(\d+)")
        if (-not $match.Success) { $match = [regex]::Match($start.output, "(?m)^TotalTime:\s*(\d+)") }
        if (-not $match.Success) { throw "Release activity start output did not contain ThisTime/TotalTime." }
        $samples += [int]$match.Groups[1].Value
        $uiDump = Invoke-NativeText $AdbPath @("-s", $DeviceSerial, "shell", "uiautomator", "dump", "/sdcard/me_proc014_release_ui.xml")
        $ui = Invoke-NativeText $AdbPath @("-s", $DeviceSerial, "shell", "cat", "/sdcard/me_proc014_release_ui.xml")
        if ($uiDump.exit_code -ne 0 -or $ui.output -notmatch 'class="android\.view\.SurfaceView"') {
            throw "Release variant did not expose the running SurfaceView/content-loaded shell."
        }
    }
    return [ordered]@{
        runs = $samples.Count
        samples_ms = @($samples)
        min_ms = ($samples | Measure-Object -Minimum).Minimum
        median_ms = @($samples | Sort-Object)[[int][math]::Floor($samples.Count / 2)]
        max_ms = ($samples | Measure-Object -Maximum).Maximum
        content_load_smoke = $true
    }
}

$resolvedKeystorePath = Resolve-WorkspacePath $KeystorePropertiesPath
$resolvedReportPath = Resolve-WorkspacePath $ReportPath
$resolvedSdkRoot = Resolve-SdkRoot
$preflight = Get-Preflight $resolvedSdkRoot $resolvedKeystorePath
if (-not $preflight.pass) {
    Write-Result ([ordered]@{
        agent = "me-android-release"
        status = "blocked"
        verdict = "blocked"
        summary = "Android release lane preflight is incomplete."
        reason = if ($preflight.missing -contains "keystore_properties") { "keystore_properties_missing" } else { "preflight_failed" }
        command = $command
        exit_code = 2
        application_id = $ApplicationId
        version_code = $VersionCode
        version_name = $VersionName
        preflight = $preflight
        fallback = "Provide an untracked keystore properties file and rerun; device checks may be skipped only for a build-only report."
    }) 2
}

$javaHome = Resolve-JavaHome
if ([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -and $null -ne $javaHome) { $env:JAVA_HOME = $javaHome }
$startedEmulator = $false
$activeSerial = $Serial
Push-Location $root
try {
    $gradleProperties = @(
        "-Pmyengine.keystore.properties=$resolvedKeystorePath",
        "-Pmyengine.applicationId=$ApplicationId",
        "-Pmyengine.versionCode=$VersionCode",
        "-Pmyengine.versionName=$VersionName"
    )
    if (-not $SkipBuild) {
        $build = Invoke-NativeText (Join-Path $root "gradlew.bat") (@("--quiet", ":android:validateReleaseSigning", ":android:bundleRelease", ":android:assembleRelease") + $gradleProperties)
        if ($build.exit_code -ne 0) {
            Write-Result ([ordered]@{
                agent = "me-android-release"
                status = "fail"
                verdict = "fail"
                summary = "Android release artifacts failed to build."
                reason = "release_build_failed"
                command = "$command :android:validateReleaseSigning :android:bundleRelease :android:assembleRelease"
                exit_code = $build.exit_code
                output_tail = @($build.output -split "`r?`n" | Where-Object { $_ } | Select-Object -Last 20)
            }) 1
        }
    }

    $aab = Get-ChildItem -LiteralPath (Join-Path $root "android\build\outputs\bundle\release") -Filter "*.aab" -File -ErrorAction SilentlyContinue | Select-Object -First 1
    $apk = Get-ChildItem -LiteralPath (Join-Path $root "android\build\outputs\apk\release") -Filter "*.apk" -File -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $aab -or $null -eq $apk) {
        Write-Result ([ordered]@{
            agent = "me-android-release"
            status = "fail"
            verdict = "fail"
            summary = "Release AAB/APK outputs were not found."
            reason = "release_artifact_missing"
            command = $command
            exit_code = 1
        }) 1
    }
    $aabMetrics = Read-AabMetrics $aab.FullName
    if (-not $aabMetrics.content_pack_present) {
        Write-Result ([ordered]@{
            agent = "me-android-release"
            status = "fail"
            verdict = "fail"
            summary = "Release AAB is missing required sandbox content assets."
            reason = "release_content_missing"
            command = $command
            exit_code = 1
            aab = $aabMetrics
        }) 1
    }
    if ($PreflightOnly) {
        Write-Result ([ordered]@{
            agent = "me-android-release"
            status = "pass"
            verdict = "pass"
            summary = "Android release preflight and existing artifact inspection passed."
            reason = "preflight_only"
            command = $command
            exit_code = 0
            application_id = $ApplicationId
            version_code = [int]$VersionCode
            version_name = $VersionName
            aab = $aabMetrics
            apk_path = $apk.FullName
            device = "not_run"
        }) 0
    }

    if (-not $SkipDevice) {
        $adbPath = Join-Path $resolvedSdkRoot "platform-tools\adb.exe"
        if ([string]::IsNullOrWhiteSpace($activeSerial)) { $activeSerial = Get-DeviceSerial $adbPath }
        if ([string]::IsNullOrWhiteSpace($activeSerial)) {
            $activeSerial = "emulator-5554"
            $emulatorPath = Join-Path $resolvedSdkRoot "emulator\emulator.exe"
            $null = Start-Process -FilePath $emulatorPath -ArgumentList @("-avd", $AvdName, "-port", "5554", "-no-window", "-no-audio", "-no-boot-anim", "-no-snapshot", "-gpu", "swiftshader_indirect") -WindowStyle Hidden -PassThru
            $startedEmulator = $true
        }
        if (-not (Wait-ForDevice $adbPath $activeSerial)) {
            Write-Result ([ordered]@{
                agent = "me-android-release"
                status = "blocked"
                verdict = "blocked"
                summary = "Android release artifact exists, but the device did not become ready."
                reason = "device_boot_timeout"
                command = $command
                exit_code = 2
                serial = $activeSerial
                started_emulator = $startedEmulator
                aab = $aabMetrics
            }) 2
        }
        $install = Invoke-NativeText $adbPath @("-s", $activeSerial, "install", "-r", $apk.FullName)
        if ($install.exit_code -ne 0) { throw "Release APK install failed: $($install.output)" }
        $coldStart = Read-ColdStartMetrics $adbPath $activeSerial
    } else {
        $coldStart = [ordered]@{ status = "skipped"; samples_ms = @(); content_load_smoke = $false }
    }
    $report = [ordered]@{
        schema = "proc-014-android-release-v1"
        agent = "me-android-release"
        status = "pass"
        verdict = "pass"
        command = $command
        application_id = $ApplicationId
        version_code = [int]$VersionCode
        version_name = $VersionName
        signing = [ordered]@{ source = "untracked_keystore_properties"; properties_path = $resolvedKeystorePath; secrets_emitted = $false }
        aab = $aabMetrics
        apk_path = $apk.FullName
        device = [ordered]@{ avd = $AvdName; serial = $activeSerial; started_emulator = $startedEmulator }
        cold_start = $coldStart
    }
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $resolvedReportPath) | Out-Null
    $report | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $resolvedReportPath -Encoding utf8
    Write-Result $report 0
} catch {
    Write-Result ([ordered]@{
        agent = "me-android-release"
        status = "fail"
        verdict = "fail"
        summary = "Android release lane encountered an unexpected failure."
        reason = "unexpected_failure"
        command = $command
        exit_code = 1
        error = $_.Exception.Message
    }) 1
} finally {
    if ($startedEmulator -and -not [string]::IsNullOrWhiteSpace($activeSerial)) {
        $adbPath = Join-Path $resolvedSdkRoot "platform-tools\adb.exe"
        if (Test-Path -LiteralPath $adbPath -PathType Leaf) { $null = & $adbPath -s $activeSerial emu kill 2>$null }
    }
    Pop-Location
}
