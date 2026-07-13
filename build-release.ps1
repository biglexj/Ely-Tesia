param(
    [switch]$SkipWindows,
    [switch]$SkipAndroid,
    [switch]$SkipMsix,
    [string]$MsixCertificate,
    [securestring]$MsixCertificatePassword
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$releaseDir = Join-Path $root "release"
$version = "1.0.2"
$windowsPackageVersion = "1.0.2"

function Find-JavaHome {
    $candidates = @(
        $env:JAVA_HOME,
        "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot",
        "C:\Program Files\Android\Android Studio\jbr"
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

    $fullJdk = $candidates | Where-Object { Test-Path (Join-Path $_ "bin\jpackage.exe") } | Select-Object -First 1
    if (-not $SkipWindows -and -not $fullJdk) {
        throw "No se encontró un JDK 17 completo con jpackage.exe. Instala Microsoft.OpenJDK.17."
    }
    if ($fullJdk) { return $fullJdk }
    return $candidates | Select-Object -First 1
}

$env:JAVA_HOME = Find-JavaHome
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not $SkipAndroid) {
    if (-not (Test-Path $androidSdk)) { throw "No se encontró Android SDK en: $androidSdk" }
    $env:ANDROID_HOME = $androidSdk
}

New-Item -ItemType Directory -Force $releaseDir | Out-Null
Get-ChildItem $releaseDir -File -ErrorAction SilentlyContinue | Remove-Item -Force

$tasks = @()
if (-not $SkipWindows) {
    $tasks += ":composeApp:packageMsi"
    $tasks += ":composeApp:packageExe"
}
if (-not $SkipAndroid) { $tasks += ":composeApp:assembleDebug" }

Push-Location $root
try {
    & "$root\gradlew.bat" @tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
} finally {
    Pop-Location
}

if (-not $SkipWindows) {
    Copy-Item "$root\composeApp\build\compose\binaries\main\msi\ElyTesia-$windowsPackageVersion.msi" `
        "$releaseDir\ElyTesia-Windows-$version.msi"
    Copy-Item "$root\composeApp\build\compose\binaries\main\exe\ElyTesia-$windowsPackageVersion.exe" `
        "$releaseDir\ElyTesia-Windows-$version.exe"

    if (-not $SkipMsix) {
        $msixArgs = @{
            Version = "$version.0"
            OutputPath = "$releaseDir\ElyTesia-Windows-$version.msix"
        }
        if ($MsixCertificate) { $msixArgs.CertificatePath = $MsixCertificate }
        if ($MsixCertificatePassword) { $msixArgs.CertificatePassword = $MsixCertificatePassword }
        try {
            & "$root\scripts\build-msix.ps1" @msixArgs
        } catch {
            Write-Warning "MSIX omitido: $($_.Exception.Message)"
            Write-Warning "Los paquetes EXE, MSI y APK continuarán generándose normalmente."
        }
    }
}
if (-not $SkipAndroid) {
    Copy-Item "$root\composeApp\build\outputs\apk\debug\composeApp-debug.apk" `
        "$releaseDir\ElyTesia-Android-$version-debug.apk"
}

$artifacts = Get-ChildItem $releaseDir -File
$artifacts | Get-FileHash -Algorithm SHA256 | ForEach-Object {
    "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf)
} | Set-Content "$releaseDir\SHA256SUMS.txt"

Write-Host ""
Write-Host "Build terminado. Archivos disponibles en:" -ForegroundColor Green
Get-ChildItem $releaseDir -File | ForEach-Object {
    Write-Host " - $($_.FullName)"
}
