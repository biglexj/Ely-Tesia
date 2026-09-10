# ==============================================================================
# build-release.ps1 — Ely-Tesia
# Ecosistema Biglex - Documentación Core (Core-Docs)
# 
# Descripción: Script estándar de compilación, empaquetado, firma, checksums SHA256,
# etiquetado Git y publicación en GitHub Releases para Ely-Tesia.
# ==============================================================================

param(
    # Versión a publicar (ej. '1.0.9'). Si se omite, se incrementa el parche automáticamente.
    [string]$Version,
    # Archivo de notas de lanzamiento (por defecto RELEASE_MESSAGE.md)
    [string]$ReleaseNotesFile = "RELEASE_MESSAGE.md",
    # Mensaje explícito del release en GitHub (opcional, sobrescribe ReleaseNotesFile)
    [string]$ReleaseNotes,
    # Omitir plataformas específicas
    [switch]$SkipWindows,
    [switch]$SkipAndroid,
    [switch]$SkipMsix,
    [switch]$SkipTests,
    # Solo build local, sin git ni GitHub
    [switch]$LocalOnly,
    # Certificado MSIX opcional
    [string]$MsixCertificate,
    [securestring]$MsixCertificatePassword
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
. (Join-Path $PSScriptRoot "ReleaseTools.ps1")

$repository = "biglexj/Ely-Tesia"
$appName = "ElyTesia"
$releaseDir = Join-Path $root "release"
$versionFile = Join-Path $root "gradle.properties"
$releaseNotesPath = Join-Path $root $ReleaseNotesFile

# ─────────────────────────────────────────────
# 1. Leer y validar versión
# ─────────────────────────────────────────────
$versionContent = Get-Content -LiteralPath $versionFile -Raw -Encoding UTF8
$currentVersion = if ($versionContent -match '(?m)^elytesia\.versionName=([\d.]+)\r?$') {
    $Matches[1]
} else {
    throw "No se pudo leer elytesia.versionName en gradle.properties."
}

if ($Version -match '^(?i)local|-local|-localonly$') {
    $Version = $currentVersion
    $LocalOnly = $true
}

if (-not $Version) {
    if ($LocalOnly) {
        $Version = $currentVersion
        Write-Host "Modo local: compilando versión actual: $Version" -ForegroundColor Cyan
    } else {
        $parts = $currentVersion -split '\.'
        $parts[-1] = [int]$parts[-1] + 1
        $Version = $parts -join '.'
        Write-Host "Versión detectada automáticamente: $Version" -ForegroundColor Cyan
    }
}

Assert-SemanticVersion $Version

if ($versionContent -match '(?m)^elytesia\.versionCode=(\d+)\r?$') {
    $currentCode = [int]$Matches[1]
    $newCode = if ($Version -eq $currentVersion) { $currentCode } else { $currentCode + 1 }
} else {
    throw "No se encontró elytesia.versionCode en gradle.properties"
}

$tag = "v$Version"

# ─────────────────────────────────────────────
# 2. Preflight de Git / GitHub (si no es -LocalOnly)
# ─────────────────────────────────────────────
if (-not $LocalOnly) {
    Write-Host "[0/7] Comprobando preflight de Git y GitHub..." -ForegroundColor Yellow
    Assert-PublishPreflight -Root $root -Repository $repository -Tag $tag
}

Write-Host ""
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  Ely-Tesia — Release $tag" -ForegroundColor Magenta
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

# ─────────────────────────────────────────────
# 3. Actualizar la fuente única de versión
# ─────────────────────────────────────────────
Write-Host "[1/7] Actualizando versión centralizada..." -ForegroundColor Yellow
$versionContent = $versionContent `
    -replace '(?m)^(elytesia\.versionCode=)\d+\r?$', "`${1}$newCode" `
    -replace '(?m)^(elytesia\.versionName=)[\d.]+\r?$', "`${1}$Version"
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($versionFile, $versionContent, $utf8NoBom)

# Sincronizar UpdateChecker en desktopMain y androidMain
$desktopChecker = Join-Path $root "composeApp\src\desktopMain\kotlin\com\biglexj\elytesia\update\UpdateChecker.kt"
if (Test-Path -LiteralPath $desktopChecker) {
    $dcContent = Get-Content -LiteralPath $desktopChecker -Raw -Encoding UTF8
    $dcContent = $dcContent -replace '(?m)actual val currentVersion: String = "[\d.]+"', "actual val currentVersion: String = `"$Version`""
    [System.IO.File]::WriteAllText($desktopChecker, $dcContent, $utf8NoBom)
}

$androidChecker = Join-Path $root "composeApp\src\androidMain\kotlin\com\biglexj\elytesia\update\UpdateChecker.kt"
if (Test-Path -LiteralPath $androidChecker) {
    $acContent = Get-Content -LiteralPath $androidChecker -Raw -Encoding UTF8
    $acContent = $acContent -replace '(?m)actual val currentVersion: String = "[\d.]+"', "actual val currentVersion: String = `"$Version`""
    [System.IO.File]::WriteAllText($androidChecker, $acContent, $utf8NoBom)
}

Write-Host "  versionCode = $newCode  |  versionName = $Version" -ForegroundColor Gray

# ─────────────────────────────────────────────
# 4. Entorno de compilación (JDK y Android SDK)
# ─────────────────────────────────────────────
$env:JAVA_HOME = Get-FullJdk
$androidSdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
if (-not $SkipAndroid -and -not (Test-Path -LiteralPath $androidSdk)) {
    throw "No se encontró Android SDK en: $androidSdk"
}
$env:ANDROID_HOME = $androidSdk

if (-not $SkipAndroid) {
    $hasSigningFile = Test-Path -LiteralPath (Join-Path $root "keystore.properties")
    $hasSigningEnvironment = $env:ELYTESIA_KEYSTORE_FILE -and `
        $env:ELYTESIA_KEYSTORE_PASSWORD -and `
        $env:ELYTESIA_KEY_ALIAS -and `
        $env:ELYTESIA_KEY_PASSWORD
    if (-not $hasSigningFile -and -not $hasSigningEnvironment) {
        throw "Falta la firma Android permanente. Configura keystore.properties o las variables ELYTESIA_KEYSTORE_*."
    }
}

# ─────────────────────────────────────────────
# 5. Build
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[2/7] Compilando artefactos con Gradle..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force -Path $releaseDir | Out-Null
Get-ChildItem -LiteralPath $releaseDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

$tasks = @()
if (-not $SkipTests) { $tasks += ":composeApp:allTests" }
if (-not $SkipWindows) { $tasks += ":composeApp:packageExe" }
if (-not $SkipAndroid) {
    $tasks += ":composeApp:assembleRelease"
}

Push-Location $root
try {
    Invoke-Checked "$root\gradlew.bat" (@("-Dorg.gradle.java.home=$env:JAVA_HOME") + $tasks)
} finally {
    Pop-Location
}

# ─────────────────────────────────────────────
# 6. Copiar y verificar artefactos en release/
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[3/7] Copiando y verificando artefactos en release/..." -ForegroundColor Yellow
if (-not $SkipWindows) {
    $exeSrc = "$root\composeApp\build\compose\binaries\main\exe\ElyTesia-$Version.exe"
    
    if (-not (Test-Path -LiteralPath $exeSrc)) {
        throw "No se generó el instalador ejecutable de Windows esperado: $exeSrc"
    }
    Copy-Item -LiteralPath $exeSrc -Destination "$releaseDir\ElyTesia-Windows-$Version.exe"
}

if (-not $SkipAndroid) {
    $apkSrc = "$root\composeApp\build\outputs\apk\release\composeApp-release.apk"
    if (-not (Test-Path -LiteralPath $apkSrc)) {
        throw "No se generó el APK release firmado esperado: $apkSrc"
    }

    $buildTools = Get-ChildItem -LiteralPath (Join-Path $androidSdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $buildTools) { throw "No se encontraron Android build-tools para validar el APK." }
    $apkSigner = Join-Path $buildTools.FullName "apksigner.bat"
    $aapt = Join-Path $buildTools.FullName "aapt.exe"

    Invoke-Checked $apkSigner @("verify", "--verbose", "--print-certs", $apkSrc)

    $badgingRaw = & $aapt dump badging $apkSrc
    if ($LASTEXITCODE -ne 0) { throw "No se pudo inspeccionar la identidad del APK." }
    $badging = $badgingRaw | Select-Object -First 1
    if ($badging -notmatch "name='com\.biglexj\.elytesia'") {
        throw "El applicationId del APK no corresponde a Ely-Tesia: $badging"
    }
    if ($badging -notmatch "versionCode='$newCode'" -or $badging -notmatch "versionName='$([regex]::Escape($Version))'") {
        throw "La versión interna del APK no coincide con $Version ($newCode): $badging"
    }

    Copy-Item -LiteralPath $apkSrc -Destination "$releaseDir\ElyTesia-Android-$Version.apk"
}

# Generar Checksums SHA-256
$artifacts = Get-ChildItem -LiteralPath $releaseDir -File | Where-Object Name -ne "SHA256SUMS.txt"
$artifacts | Get-FileHash -Algorithm SHA256 | ForEach-Object {
    "{0}  {1}" -f $_.Hash.ToLowerInvariant(), (Split-Path $_.Path -Leaf)
} | Set-Content -LiteralPath "$releaseDir\SHA256SUMS.txt" -Encoding UTF8

Write-Host "  Artefactos generados:" -ForegroundColor Gray
Get-ChildItem -LiteralPath $releaseDir -File | ForEach-Object { Write-Host "    $($_.Name)" -ForegroundColor Gray }

if ($LocalOnly) {
    Write-Host ""
    Write-Host "Build local completado exitosamente (-LocalOnly). Git y GitHub omitidos." -ForegroundColor Green
    exit 0
}

# ─────────────────────────────────────────────
# 7. Git: commit + tag + push
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[4/7] Creando commit de release..." -ForegroundColor Yellow
Invoke-Checked git @("add", "-A")
& git commit -m "chore: bump version to $Version"
if ($LASTEXITCODE -gt 1) { throw "git commit falló (exit $LASTEXITCODE)" }

Write-Host ""
Write-Host "[5/7] Creando tag $tag..." -ForegroundColor Yellow
Invoke-Checked git @("tag", "-a", $tag, "-m", "Ely-Tesia $tag")

Write-Host ""
Write-Host "[6/7] Enviando commits y tags a origin..." -ForegroundColor Yellow
Invoke-Checked git @("push", "origin", "main")
Invoke-Checked git @("push", "origin", $tag)

# ─────────────────────────────────────────────
# 8. GitHub Release
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[7/7] Creando GitHub Release $tag..." -ForegroundColor Yellow

if (-not $ReleaseNotes) {
    if (Test-Path -LiteralPath $releaseNotesPath) {
        $ReleaseNotes = Get-Content -LiteralPath $releaseNotesPath -Raw -Encoding UTF8
    } else {
        $ReleaseNotes = @"
## Ely-Tesia $Version

### Novedades y correcciones
- Actualización de rendimiento y mejoras generales de reproducción.

### Descargas
- **Windows**: instalador unificado \`.exe\`.
- **Android**: archivo \`.apk\` firmado.
- **Verificación**: hashes incluidos en \`SHA256SUMS.txt\`.

---
*Proyecto abierto bajo licencia MIT. 💜*
"@
    }
}

$ghArgs = @(
    "release", "create", $tag,
    "--title", "Ely-Tesia $Version",
    "--notes", $ReleaseNotes
)

Get-ChildItem -LiteralPath $releaseDir -File | ForEach-Object { $ghArgs += $_.FullName }

Invoke-Checked gh $ghArgs

Write-Host ""
Write-Host "══════════════════════════════════════════" -ForegroundColor Green
Write-Host "  ¡Release $tag publicado con éxito!" -ForegroundColor Green
Write-Host "══════════════════════════════════════════" -ForegroundColor Green
Write-Host ""
Write-Host "  https://github.com/$repository/releases/tag/$tag" -ForegroundColor Cyan
Write-Host ""
