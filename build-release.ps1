param(
    # Versión a publicar. Si se omite, se lee la versión actual de build.gradle.kts y se incrementa el patch.
    [string]$Version,
    # Mensaje del release en GitHub. Si se omite, se genera uno por defecto.
    [string]$ReleaseNotes,
    # Omitir builds
    [switch]$SkipWindows,
    [switch]$SkipAndroid,
    [switch]$SkipMsix,
    # Solo build local, sin git ni GitHub
    [switch]$LocalOnly,
    # Certificado MSIX opcional
    [string]$MsixCertificate,
    [securestring]$MsixCertificatePassword
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$releaseDir = Join-Path $root "release"
$gradleFile = Join-Path $root "composeApp\build.gradle.kts"

# ─────────────────────────────────────────────
# 1. Leer / determinar versión
# ─────────────────────────────────────────────
$gradleContent = Get-Content $gradleFile -Raw -Encoding UTF8

if (-not $Version) {
    if ($gradleContent -match 'versionName\s*=\s*"([\d.]+)"') {
        $parts = $Matches[1] -split '\.'
        $parts[-1] = [int]$parts[-1] + 1
        $Version = $parts -join '.'
        Write-Host "Versión detectada automáticamente: $Version" -ForegroundColor Cyan
    } else {
        throw "No se pudo leer la versión actual de build.gradle.kts. Pasa -Version manualmente."
    }
}

if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $newCode = [int]$Matches[1] + 1
} else {
    throw "No se encontró versionCode en build.gradle.kts"
}

Write-Host ""
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  Ely-Tesia — Release v$Version" -ForegroundColor Magenta
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

# ─────────────────────────────────────────────
# 2. Actualizar versión en build.gradle.kts
# ─────────────────────────────────────────────
Write-Host "[1/7] Actualizando versión en build.gradle.kts..." -ForegroundColor Yellow
$gradleContent = $gradleContent `
    -replace '(versionCode\s*=\s*)\d+', "`${1}$newCode" `
    -replace '(versionName\s*=\s*")[^"]*"', "`${1}$Version`"" `
    -replace '(msiPackageVersion\s*=\s*")[^"]*"', "`${1}$Version`"" `
    -replace '(exePackageVersion\s*=\s*")[^"]*"', "`${1}$Version`"" `
    -replace '(packageVersion\s*=\s*")[^"]*"', "`${1}$Version`""
Set-Content $gradleFile -Value $gradleContent -Encoding UTF8 -NoNewline

# Actualizar también la variable $version del propio script para builds futuros
$selfContent = Get-Content $PSCommandPath -Raw -Encoding UTF8
$selfContent = $selfContent -replace '(\$version\s*=\s*")[^"]*"', "`${1}$Version`""
Set-Content $PSCommandPath -Value $selfContent -Encoding UTF8 -NoNewline

Write-Host "  versionCode = $newCode  |  versionName = $Version" -ForegroundColor Gray

# ─────────────────────────────────────────────
# 3. Detectar Java Home
# ─────────────────────────────────────────────
function Find-JavaHome {
    $candidates = @(
        "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot",
        $env:JAVA_HOME,
        "C:\Program Files\Android\Android Studio\jbr"
    ) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) }

    $fullJdk = $candidates | Where-Object { Test-Path (Join-Path $_ "bin\jpackage.exe") } | Select-Object -First 1
    if (-not $SkipWindows -and -not $fullJdk) {
        throw "No se encontró un JDK completo con jpackage.exe. Instala Microsoft.OpenJDK.17."
    }
    if ($fullJdk) { return $fullJdk }
    return $candidates | Select-Object -First 1
}

$env:JAVA_HOME = Find-JavaHome
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not $SkipAndroid -and -not (Test-Path $androidSdk)) {
    throw "No se encontró Android SDK en: $androidSdk"
}
$env:ANDROID_HOME = $androidSdk

# ─────────────────────────────────────────────
# 4. Build
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[2/7] Compilando artefactos..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force $releaseDir | Out-Null
Get-ChildItem $releaseDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

$tasks = @()
if (-not $SkipWindows) { $tasks += ":composeApp:packageMsi"; $tasks += ":composeApp:packageExe" }
if (-not $SkipAndroid) { $tasks += ":composeApp:assembleRelease" }

Push-Location $root
try {
    & "$root\gradlew.bat" "-Dorg.gradle.java.home=$env:JAVA_HOME" @tasks
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
} finally {
    Pop-Location
}

# ─────────────────────────────────────────────
# 5. Copiar artefactos al directorio release/
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[3/7] Copiando artefactos a release/..." -ForegroundColor Yellow
if (-not $SkipWindows) {
    Copy-Item "$root\composeApp\build\compose\binaries\main\msi\ElyTesia-$Version.msi" `
        "$releaseDir\ElyTesia-Windows-$Version.msi"
    Copy-Item "$root\composeApp\build\compose\binaries\main\exe\ElyTesia-$Version.exe" `
        "$releaseDir\ElyTesia-Windows-$Version.exe"

    if (-not $SkipMsix) {
        $msixArgs = @{ Version = "$Version.0"; OutputPath = "$releaseDir\ElyTesia-Windows-$Version.msix" }
        if ($MsixCertificate) { $msixArgs.CertificatePath = $MsixCertificate }
        if ($MsixCertificatePassword) { $msixArgs.CertificatePassword = $MsixCertificatePassword }
        try { & "$root\scripts\build-msix.ps1" @msixArgs }
        catch { Write-Warning "MSIX omitido: $($_.Exception.Message)" }
    }
}
if (-not $SkipAndroid) {
    $apkSrc = "$root\composeApp\build\outputs\apk\release\composeApp-release-unsigned.apk"
    if (-not (Test-Path $apkSrc)) {
        $apkSrc = "$root\composeApp\build\outputs\apk\debug\composeApp-debug.apk"
    }
    Copy-Item $apkSrc "$releaseDir\ElyTesia-Android-$Version.apk"
}

# SHA256
$artifacts = Get-ChildItem $releaseDir -File
$artifacts | Get-FileHash -Algorithm SHA256 | ForEach-Object {
    "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf)
} | Set-Content "$releaseDir\SHA256SUMS.txt"

Write-Host "  Artefactos listos:" -ForegroundColor Gray
Get-ChildItem $releaseDir -File | ForEach-Object { Write-Host "    $($_.Name)" -ForegroundColor Gray }

if ($LocalOnly) {
    Write-Host ""
    Write-Host "Build local completado (-LocalOnly). Git y GitHub omitidos." -ForegroundColor Green
    exit 0
}

# ─────────────────────────────────────────────
# 6. Git: commit + tag + push
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[4/7] Commit de los cambios..." -ForegroundColor Yellow
git add -A
git commit -m "chore: bump version to $Version"
if ($LASTEXITCODE -ne 0) { throw "git commit falló" }

Write-Host ""
Write-Host "[5/7] Creando tag v$Version..." -ForegroundColor Yellow
git tag -a "v$Version" -m "Ely-Tesia v$Version"
if ($LASTEXITCODE -ne 0) { throw "git tag falló" }

Write-Host ""
Write-Host "[6/7] Push de commits y tags..." -ForegroundColor Yellow
git push origin HEAD
git push origin "v$Version"
if ($LASTEXITCODE -ne 0) { throw "git push falló" }

# ─────────────────────────────────────────────
# 7. GitHub Release
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[7/7] Creando GitHub Release v$Version..." -ForegroundColor Yellow

if (-not $ReleaseNotes) {
    $ReleaseNotes = @"
## Ely-Tesia $Version

### Novedades y correcciones

<!-- Edita estas notas antes de ejecutar el script, o pasa -ReleaseNotes "..." -->

### Descargas

- **Windows**: instalador \`.exe\` o \`.msi\`.
- **Windows moderno/Store**: paquete \`.msix\`.
- **Android**: archivo \`.apk\`.
- **Verificación**: hashes incluidos en \`SHA256SUMS.txt\`.

---
*Proyecto abierto bajo licencia MIT. 💜*
"@
}

$ghArgs = @(
    "release", "create", "v$Version",
    "--title", "Ely-Tesia $Version",
    "--notes", $ReleaseNotes
)

Get-ChildItem $releaseDir -File | ForEach-Object { $ghArgs += $_.FullName }

gh @ghArgs
if ($LASTEXITCODE -ne 0) { throw "gh release create falló" }

Write-Host ""
Write-Host "══════════════════════════════════════════" -ForegroundColor Green
Write-Host "  ¡Release v$Version publicado con éxito!" -ForegroundColor Green
Write-Host "══════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "  https://github.com/biglexj/Ely-Tesia/releases/tag/v$Version" -ForegroundColor Cyan
Write-Host ""


