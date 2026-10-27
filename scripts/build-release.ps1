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
    # Mensaje del commit de release (opcional). La primera linea es el
    # asunto; el resto se anade como cuerpo.
    [string]$CommitMessage = "",
    # Mensaje explícito del release en GitHub (opcional, sobrescribe ReleaseNotesFile)
    [string]$ReleaseNotes,
    # Omitir plataformas específicas
    [switch]$SkipWindows,
    [switch]$SkipAndroid,
    [switch]$SkipMsix,
    [switch]$SkipTests,
    # Generar tambien el instalador EXE de Inno Setup (plantilla Biglex Modern 2.0)
    [switch]$BuildInno,
    # Firmar el instalador EXE con un certificado Authenticode (opcional)
    [string]$InstallerCertificate,
    [securestring]$InstallerCertificatePassword,
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
        # Si la version actual AUN NO se publico, se conserva: una entrega que
        # nunca llego a publicarse no debe generar un salto de version. Solo
        # se incrementa cuando la version vigente ya existe como tag o como
        # GitHub Release.
        $currentTag = "v$currentVersion"
        $alreadyPublished = $false
        $oldErrorAction = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"
        try {
            & git rev-parse --quiet --verify "refs/tags/$currentTag" *> $null
            if ($LASTEXITCODE -eq 0) {
                $alreadyPublished = $true
            } else {
                $remoteTags = & git ls-remote --tags origin "refs/tags/$currentTag" 2>$null
                if ($LASTEXITCODE -eq 0 -and $remoteTags) {
                    $alreadyPublished = $true
                } else {
                    & gh release view $currentTag --repo $repository *> $null
                    if ($LASTEXITCODE -eq 0) { $alreadyPublished = $true }
                }
            }
        } finally {
            $ErrorActionPreference = $oldErrorAction
        }

        if ($alreadyPublished) {
            $parts = $currentVersion -split '\.'
            $parts[-1] = [int]$parts[-1] + 1
            $Version = $parts -join '.'
            Write-Host "La versión $currentVersion ya está publicada: se incrementa a $Version" -ForegroundColor Cyan
        } else {
            $Version = $currentVersion
            Write-Host "La versión $currentVersion todavía no se publicó: se conserva" -ForegroundColor Cyan
        }
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
# Solo se escribe si el contenido cambia: reescribirlo sin cambios actualiza su
# fecha y el control de frescura da por obsoleta la imagen de la app compilada.
$versionChanged = Set-FileContentIfChanged -Path $versionFile -Content $versionContent -Encoding $utf8NoBom

# Sincronizar UpdateChecker en desktopMain y androidMain
$desktopChecker = Join-Path $root "composeApp\src\desktopMain\kotlin\com\biglexj\elytesia\update\UpdateChecker.kt"
if (Test-Path -LiteralPath $desktopChecker) {
    $dcContent = Get-Content -LiteralPath $desktopChecker -Raw -Encoding UTF8
    $dcContent = $dcContent -replace '(?m)actual val currentVersion: String = "[\d.]+"', "actual val currentVersion: String = `"$Version`""
    $versionChanged = (Set-FileContentIfChanged -Path $desktopChecker -Content $dcContent -Encoding $utf8NoBom) -or $versionChanged
}

$androidChecker = Join-Path $root "composeApp\src\androidMain\kotlin\com\biglexj\elytesia\update\UpdateChecker.kt"
if (Test-Path -LiteralPath $androidChecker) {
    $acContent = Get-Content -LiteralPath $androidChecker -Raw -Encoding UTF8
    $acContent = $acContent -replace '(?m)actual val currentVersion: String = "[\d.]+"', "actual val currentVersion: String = `"$Version`""
    $versionChanged = (Set-FileContentIfChanged -Path $androidChecker -Content $acContent -Encoding $utf8NoBom) -or $versionChanged
}

if ($versionChanged) {
    Write-Host "  Ficheros de version actualizados." -ForegroundColor Gray
} else {
    Write-Host "  La version ya estaba sincronizada: no se tocan los ficheros de origen." -ForegroundColor DarkGray
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
    # El EXE de jpackage es el formato legacy. Cuando el instalador oficial de
    # Inno Setup esta activo no se copia a release/: la distribucion oficial de
    # 1.0.9 en adelante es EXCLUSIVAMENTE el instalador unificado + el APK, de
    # modo que release/ contenga solo los artefactos que se publican y el
    # SHA256SUMS.txt cubra exactamente ese conjunto.
    if (-not $BuildInno) {
        Copy-Item -LiteralPath $exeSrc -Destination "$releaseDir\ElyTesia-Windows-$Version.exe"
    }
}

# Instalador EXE de Inno Setup (plantilla compartida Biglex Modern 2.0).
# Se genera aparte del EXE de jpackage: el artefacto tiene otro nombre
# (ElyTesia-Setup-X.Y.Z.exe) para no colisionar con ElyTesia-Windows-X.Y.Z.exe.
# Genera ademas el manifiesto de winget con el InstallerSha256 REAL del EXE
# compilado: asi el hash nunca queda desfasado respecto al artefacto publicado.
if ($BuildInno -and -not $SkipWindows) {
    Write-Host ""
    Write-Host "[3b/7] Generando el instalador EXE de Inno Setup..." -ForegroundColor Yellow
    $installerArgs = @(
        "-NoProfile",
        "-File", (Join-Path $root "scripts\packaging\Build-Installer.ps1"),
        "-Version", $Version,
        "-SkipBuild",
        "-OutputDirectory", $releaseDir,
        "-EmitWingetManifest"
    )
    if ($InstallerCertificate) {
        $installerArgs += @("-CertificatePath", $InstallerCertificate)
    }
    # La contrasena viaja por variable de entorno: no queda expuesta en la
    # linea de ordenes del proceso hijo.
    $previousCertPassword = $env:ELYTESIA_INSTALLER_CERT_PASSWORD
    try {
        if ($InstallerCertificatePassword) {
            $env:ELYTESIA_INSTALLER_CERT_PASSWORD = [System.Net.NetworkCredential]::new('', $InstallerCertificatePassword).Password
        }
        Invoke-Checked "powershell" $installerArgs
    }
    finally {
        $env:ELYTESIA_INSTALLER_CERT_PASSWORD = $previousCertPassword
    }
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
# Solo sobre los artefactos previstos para publicacion, con formato UTF-8 sin
# BOM y saltos LF: es el unico formato que `sha256sum -c` acepta en Linux y
# macOS. Con CRLF + BOM (lo que produce Set-Content en Windows) la
# verificacion falla en esos sistemas.
$artifacts = Get-ChildItem -LiteralPath $releaseDir -File | Where-Object Name -ne "SHA256SUMS.txt" | Sort-Object Name
$checksumLines = $artifacts | ForEach-Object {
    $sha256 = Get-FileSha256 -Path $_.FullName
    "{0}  {1}" -f $sha256, $_.Name
}
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    "$releaseDir\SHA256SUMS.txt",
    (($checksumLines -join "`n") + "`n"),
    $utf8NoBom)

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
# Mensaje del commit de release. El cuerpo puede pasarse con -CommitMessage;
# si no, se usa un resumen del contenido real de la entrega. El trailer de
# atribucion se anade siempre que el commit lo crea este pipeline.
$commitSubject = if ($CommitMessage) { ($CommitMessage -split "`n")[0].Trim() } else { "release: preparar la versión $Version" }
$commitBody = if ($CommitMessage) { ($CommitMessage -split "`n" | Select-Object -Skip 1) -join "`n" } else { "" }
$commitText = $commitSubject
if ($commitBody.Trim()) { $commitText = $commitSubject + "`n`n" + $commitBody.Trim() }
$commitText = $commitText + "`n`nCo-Authored-By: Claude Code <noreply@anthropic.com>"
& git commit -m $commitText
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
