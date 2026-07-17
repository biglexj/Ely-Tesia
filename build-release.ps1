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
$versionFile = Join-Path $root "gradle.properties"

# ─────────────────────────────────────────────
# 1. Leer / determinar versión
# ─────────────────────────────────────────────
$versionContent = Get-Content $versionFile -Raw -Encoding UTF8
$currentVersion = if ($versionContent -match '(?m)^elytesia\.versionName=([\d.]+)$') {
    $Matches[1]
} else {
    throw "No se pudo leer elytesia.versionName en gradle.properties."
}

if (-not $Version) {
    $parts = $currentVersion -split '\.'
    $parts[-1] = [int]$parts[-1] + 1
    $Version = $parts -join '.'
    Write-Host "Versión detectada automáticamente: $Version" -ForegroundColor Cyan
}

if ($versionContent -match '(?m)^elytesia\.versionCode=(\d+)$') {
    $currentCode = [int]$Matches[1]
    $newCode = if ($Version -eq $currentVersion) { $currentCode } else { $currentCode + 1 }
} else {
    throw "No se encontró elytesia.versionCode en gradle.properties"
}

Write-Host ""
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host "  Ely-Tesia — Release v$Version" -ForegroundColor Magenta
Write-Host "══════════════════════════════════════════" -ForegroundColor Magenta
Write-Host ""

# ─────────────────────────────────────────────
# 2. Actualizar la fuente única de versión
# ─────────────────────────────────────────────
Write-Host "[1/7] Actualizando versión centralizada..." -ForegroundColor Yellow
$versionContent = $versionContent `
    -replace '(?m)^(elytesia\.versionCode=)\d+$', "`${1}$newCode" `
    -replace '(?m)^(elytesia\.versionName=)[\d.]+$', "`${1}$Version"
Set-Content $versionFile -Value $versionContent -Encoding UTF8 -NoNewline

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

if (-not $SkipAndroid) {
    $hasSigningFile = Test-Path (Join-Path $root "keystore.properties")
    $hasSigningEnvironment = $env:ELYTESIA_KEYSTORE_FILE -and `
        $env:ELYTESIA_KEYSTORE_PASSWORD -and `
        $env:ELYTESIA_KEY_ALIAS -and `
        $env:ELYTESIA_KEY_PASSWORD
    if (-not $hasSigningFile -and -not $hasSigningEnvironment) {
        throw "Falta la firma Android permanente. Configura keystore.properties o las variables ELYTESIA_KEYSTORE_*."
    }
}

# ─────────────────────────────────────────────
# 4. Build
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "[2/7] Compilando artefactos..." -ForegroundColor Yellow
New-Item -ItemType Directory -Force $releaseDir | Out-Null
Get-ChildItem $releaseDir -File -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue

$tasks = @()
if (-not $SkipWindows) { $tasks += ":composeApp:packageMsi"; $tasks += ":composeApp:packageExe" }
if (-not $SkipAndroid) {
    $tasks += ":composeApp:assembleRelease"
    $tasks += ":composeApp:bundleRelease"
}

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
    $apkSrc = "$root\composeApp\build\outputs\apk\release\composeApp-release.apk"
    $aabSrc = "$root\composeApp\build\outputs\bundle\release\composeApp-release.aab"
    if (-not (Test-Path $apkSrc)) {
        throw "No se generó el APK release firmado esperado: $apkSrc"
    }
    if (-not (Test-Path $aabSrc)) {
        throw "No se generó el AAB release firmado esperado: $aabSrc"
    }

    $buildTools = Get-ChildItem (Join-Path $androidSdk "build-tools") -Directory |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $buildTools) { throw "No se encontraron Android build-tools para validar el APK." }
    $apkSigner = Join-Path $buildTools.FullName "apksigner.bat"
    $aapt = Join-Path $buildTools.FullName "aapt.exe"

    & $apkSigner verify --verbose --print-certs $apkSrc
    if ($LASTEXITCODE -ne 0) { throw "El APK release no tiene una firma válida." }

    $badging = (& $aapt dump badging $apkSrc | Select-Object -First 1)
    if ($LASTEXITCODE -ne 0) { throw "No se pudo inspeccionar la identidad del APK." }
    if ($badging -notmatch "name='com\.biglexj\.elytesia'") {
        throw "El applicationId del APK no corresponde a Ely-Tesia: $badging"
    }
    if ($badging -notmatch "versionCode='$newCode'" -or $badging -notmatch "versionName='$([regex]::Escape($Version))'") {
        throw "La versión interna del APK no coincide con $Version ($newCode): $badging"
    }

    Copy-Item $apkSrc "$releaseDir\ElyTesia-Android-$Version.apk"
    Copy-Item $aabSrc "$releaseDir\ElyTesia-Android-$Version.aab"
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
- **Android**: archivos \`.apk\` y \`.aab\` firmados.
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


