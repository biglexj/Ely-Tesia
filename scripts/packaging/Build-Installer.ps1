<#
.SYNOPSIS
    Genera el instalador EXE de Ely-Tesia para Windows con Inno Setup.

.DESCRIPTION
    Flujo:

      1. Resuelve la version del proyecto (gradle.properties -> elytesia.versionName).
      2. Reescribe packaging\windows\version.iss con esa version.
      3. Genera la distribucion de la app (imagen de aplicacion de jpackage)
         y la copia a build\inno-staging\app, que es la que empaqueta el .iss.
      4. Compila packaging\windows\setup.iss con ISCC.exe y deja el
         instalador en la carpeta de release.

    La identidad de actualizacion (AppId) vive en packaging\windows\identity.iss
    y es estable entre versiones: una version nueva actualiza la anterior en el
    mismo directorio en vez de instalar en paralelo.

    No borra instalaciones anteriores ni datos del usuario: este script solo
    escribe en build\ y release\.

.PARAMETER Version
    Version concreta a empaquetar. Por defecto, la de gradle.properties.

.PARAMETER SkipBuild
    No ejecuta Gradle: reutiliza la distribucion ya generada.

.PARAMETER OutputDirectory
    Carpeta donde se deja el instalador. Por defecto, la carpeta release del
    proyecto.

.PARAMETER IsccPath
    Ruta a ISCC.exe. Por defecto se busca subiendo desde este script hasta
    <workspace>\Scripts\tools\InnoSetup\ISCC.exe y, si no, en el PATH.

.PARAMETER ForceStale
    Empaqueta aunque los artefactos sean mas viejos que el codigo fuente.
    Solo para depurar: publicar asi reintroduce el build obsoleto.

.PARAMETER EmitWingetManifest
    Ademas genera el manifiesto de winget de tipo "exe" para esta version, en
    packaging\winget\manifests. No publica nada: solo deja el manifiesto listo
    para el PR en microsoft/winget-pkgs.

.EXAMPLE
    .\scripts\packaging\Build-Installer.ps1

.EXAMPLE
    .\scripts\packaging\Build-Installer.ps1 -SkipBuild -EmitWingetManifest
#>
param(
    [string]$Version,
    [switch]$SkipBuild,
    [string]$OutputDirectory,
    [string]$IsccPath = '',

    # Empaqueta aunque los artefactos sean mas viejos que el codigo fuente.
    # Solo para depurar: publicar asi reintroduce el build obsoleto.
    [switch]$ForceStale,

    [switch]$EmitWingetManifest,

    # Firma Authenticode opcional del instalador final (.pfx / .p12).
    # Sin certificado el instalador se genera sin firmar, igual que hasta ahora.
    # La contrasena puede llegar por parametro o por la variable de entorno
    # ELYTESIA_INSTALLER_CERT_PASSWORD (recomendado: no queda expuesta en la
    # linea de ordenes del proceso).
    [string]$CertificatePath = '',
    [securestring]$CertificatePassword
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$root = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
. (Join-Path $root "scripts\ReleaseTools.ps1")

$packagingDir = Join-Path $root "packaging\windows"
$setupScript = Join-Path $packagingDir "setup.iss"
$versionScript = Join-Path $packagingDir "version.iss"
$stagingDir = Join-Path $root "build\inno-staging\app"
$appImageDir = Join-Path $root "composeApp\build\compose\binaries\main\app\ElyTesia"
if (-not $OutputDirectory) { $OutputDirectory = Join-Path $root "release" }

# Contrasena del certificado: parametro o variable de entorno.
if (-not $CertificatePassword -and $env:ELYTESIA_INSTALLER_CERT_PASSWORD) {
    $CertificatePassword = ConvertTo-SecureString -String $env:ELYTESIA_INSTALLER_CERT_PASSWORD -AsPlainText -Force
}

# --- Localizador de ISCC -----------------------------------------------------
# Localiza ISCC.exe sin rutas absolutas: primero la copia del workspace,
# subiendo desde este script; despues el PATH del sistema.
function Resolve-Iscc {
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 8; $i++) {
        $candidate = Join-Path $dir 'Scripts\tools\InnoSetup\ISCC.exe'
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $parent = Split-Path -Parent $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    $onPath = Get-Command ISCC.exe -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    return $null
}

function Write-Step {
    param([string]$Message)
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Write-Fail {
    param([string]$Message)
    Write-Host "ERROR: $Message" -ForegroundColor Red
    throw $Message
}

# --- Invariantes de empaquetado (validador compartido) ---------------------
# La plantilla visual y el arranque post-instalacion son fuente unica del
# workspace. Si un .iss editado a mano o copiado de otra app los rompe, el
# instalador sale mal, asi que el validador compartido aborta el build.
function Resolve-SharedInnoTool {
    param([string]$Relative)
    $dir = $PSScriptRoot
    for ($i = 0; $i -lt 8; $i++) {
        $candidate = Join-Path $dir $Relative
        if (Test-Path -LiteralPath $candidate) { return $candidate }
        $parent = Split-Path -Parent $dir
        if (-not $parent -or $parent -eq $dir) { break }
        $dir = $parent
    }
    return $null
}

function Assert-PackagingInvariants {
    param([string]$SetupIss, [string]$PackagingDir)

    $sharedDir = Resolve-SharedInnoTool 'Scripts\tools\inno'
    if (-not $sharedDir) {
        Write-Host "AVISO: no se localizo Scripts\tools\inno en este workspace: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }

    # Plantilla visual con fuente unica: el repo guarda lo que se compilo.
    $sharedUi = Join-Path $sharedDir 'BiglexModernUI.iss'
    $localUi = Join-Path $PackagingDir 'BiglexModernUI.iss'
    if ((Test-Path -LiteralPath $sharedUi) -and (Test-Path -LiteralPath $localUi)) {
        Copy-Item -LiteralPath $sharedUi -Destination $localUi -Force
        Write-Host "Plantilla visual sincronizada con la copia compartida del workspace." -ForegroundColor Gray
    }

    $checker = Join-Path $sharedDir 'Check-Packaging.ps1'
    if (-not (Test-Path -LiteralPath $checker)) {
        Write-Host "AVISO: no se localizo Check-Packaging.ps1: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }
    $shell = Get-Command pwsh.exe -ErrorAction SilentlyContinue
    if (-not $shell) { $shell = Get-Command powershell.exe -ErrorAction SilentlyContinue }
    if (-not $shell) {
        Write-Host "AVISO: no hay pwsh.exe ni powershell.exe: se omite la verificacion de empaquetado." -ForegroundColor Yellow
        return
    }

    Write-Host "Verificando las invariantes de empaquetado (Check-Packaging.ps1)..." -ForegroundColor Cyan
    & $shell.Source -NoProfile -File $checker -SetupIss $SetupIss -Quiet
    if ($LASTEXITCODE -ne 0) {
        Write-Fail ("Las invariantes de empaquetado no se cumplen (Check-Packaging.ps1 devolvio $LASTEXITCODE). " +
                    "Revisa el mensaje del validador de arriba y corrige el setup.iss antes de compilar.")
    }
    Write-Host "Invariantes de empaquetado correctas." -ForegroundColor Green
}

# --- Candado anti-build-obsoleto -------------------------------------------
# Si el artefacto que se empaqueta es mas viejo que el codigo fuente, el
# instalador queda "recien compilado" pero lleva dentro una app vieja.
# Regla de Core: Docs/global/architecture/command-scripts-standard.md,
# principio 2.
function Get-NewestWriteTime {
    param([string[]]$Paths)
    $best = $null
    $bestPath = $null
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $item = if (Test-Path -LiteralPath $path -PathType Container) {
            Get-ChildItem -LiteralPath $path -Recurse -File -Force -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1
        }
        else {
            Get-Item -LiteralPath $path -Force -ErrorAction SilentlyContinue
        }
        if ($item -and (-not $best -or $item.LastWriteTime -gt $best)) {
            $best = $item.LastWriteTime
            $bestPath = $item.FullName
        }
    }
    return [pscustomobject]@{ Time = $best; Path = $bestPath }
}

function Assert-BuildFresh {
    param([string[]]$Artifacts, [string]$AppRoot, [string[]]$SourceRoots, [switch]$Force)
    $exeItem = $null
    foreach ($a in $Artifacts) {
        if (Test-Path -LiteralPath $a) {
            $i = Get-Item -LiteralPath $a
            if (-not $exeItem -or $i.LastWriteTime -gt $exeItem.LastWriteTime) { $exeItem = $i }
        }
    }
    if (-not $exeItem) {
        Write-Fail "No se encontro ningun artefacto compilado de: $($Artifacts -join ', ')`n  Compila la app sin el flag de omision."
    }
    $watch = @($SourceRoots)
    $watch += @(
        (Join-Path $AppRoot 'gradle.properties'),
        (Join-Path $AppRoot 'build.gradle.kts'),
        (Join-Path $AppRoot 'settings.gradle.kts')
    )
    $newest = Get-NewestWriteTime -Paths $watch
    if (-not $newest.Time) { return }
    if ($exeItem.LastWriteTime -ge $newest.Time) {
        Write-Step "Artefacto al dia: $($exeItem.LastWriteTime.ToString('dd/MM HH:mm:ss'))  $($exeItem.Name)"
        return
    }
    $rel = if ($newest.Path) { $newest.Path.Substring($AppRoot.Length).TrimStart('\') } else { '(desconocido)' }
    if ($Force) {
        Write-Host "AVISO: se empaqueta un build OBSOLETO por -ForceStale." -ForegroundColor Yellow
        Write-Host "       artefacto $($exeItem.LastWriteTime.ToString('dd/MM HH:mm:ss')) < fuente $($newest.Time.ToString('dd/MM HH:mm:ss')) $rel" -ForegroundColor Yellow
        return
    }
    Write-Fail ("El artefacto a empaquetar es MAS VIEJO que el codigo fuente:`n" +
                "  artefacto : $($exeItem.LastWriteTime.ToString('dd/MM HH:mm:ss'))  $($exeItem.FullName)`n" +
                "  fuente    : $($newest.Time.ToString('dd/MM HH:mm:ss'))  $rel`n`n" +
                "Vuelve a compilar la app (sin el flag de omision), o usa -ForceStale solo para depurar.")
}

# --- 1. Version -------------------------------------------------------------
$propertiesPath = Join-Path $root "gradle.properties"
$properties = Get-Content -LiteralPath $propertiesPath -Raw -Encoding UTF8
$versionMatch = [regex]::Match($properties, '(?m)^elytesia\.versionName=(\d+\.\d+\.\d+)')
if (-not $versionMatch.Success) {
    throw "No se pudo leer elytesia.versionName de $propertiesPath."
}
if (-not $Version) { $Version = $versionMatch.Groups[1].Value }
Assert-SemanticVersion $Version

Write-Host "[1/5] Version del instalador: $Version" -ForegroundColor Yellow

# --- 2. version.iss ---------------------------------------------------------
$versionContent = @(
    "; ---------------------------------------------------------------------------",
    "; version.iss - generada por scripts\packaging\Build-Installer.ps1 con la",
    "; version real del proyecto (gradle.properties -> elytesia.versionName).",
    "; No editar a mano: se reescribe en cada build.",
    "; ---------------------------------------------------------------------------",
    "#define MyAppVersion `"$Version`""
) -join "`n"
[System.IO.File]::WriteAllText($versionScript, $versionContent, (New-Object System.Text.UTF8Encoding($true)))

# --- 3. Distribucion de la app ---------------------------------------------
if (-not $SkipBuild) {
    Write-Host "[2/5] Generando la distribucion de la app (Gradle)..." -ForegroundColor Yellow
    $jdk = Get-FullJdk
    Write-Host "      JDK: $jdk" -ForegroundColor Gray

    # Los limites de memoria del daemon se imponen en la linea de comandos: el
    # gradle.properties global de la maquina declara -Xmx8g, que en este equipo
    # hacia que el daemon no arrancara ("insufficient memory for the Java
    # Runtime Environment to continue"). Aqui manda el valor del proyecto.
    $gradleArguments = @(
        "-Dorg.gradle.java.home=$jdk"
        "-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8"
        ":composeApp:createDistributable"
    )
    Invoke-Checked -Executable (Join-Path $root "gradlew.bat") -ArgumentList $gradleArguments
} else {
    Write-Host "[2/5] Build omitido: se reutiliza la distribucion existente." -ForegroundColor DarkYellow
}

$sourceRoots = @(Join-Path $root "composeApp\src")
Assert-BuildFresh -Artifacts @((Join-Path $appImageDir "ElyTesia.exe")) -AppRoot $root -SourceRoots $sourceRoots -Force:$ForceStale

Write-Host "[3/5] Preparando el contenido del instalador..." -ForegroundColor Yellow
if (Test-Path -LiteralPath $stagingDir) {
    Remove-Item -LiteralPath $stagingDir -Recurse -Force
}
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null

# .jpackage.xml es metadato del empaquetador de jpackage: el MSI lo guardaba
# como .package y la app no lo necesita en tiempo de ejecucion.
$excluded = @(".jpackage.xml")
Get-ChildItem -LiteralPath $appImageDir -Recurse -File | ForEach-Object {
    $relative = $_.FullName.Substring($appImageDir.Length).TrimStart('\')
    if ($excluded -contains $_.Name) { return }
    $target = Join-Path $stagingDir $relative
    New-Item -ItemType Directory -Path (Split-Path $target -Parent) -Force | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $target -Force
}

$stagedFiles = (Get-ChildItem -LiteralPath $stagingDir -Recurse -File).Count
Write-Host "      $stagedFiles ficheros preparados en $stagingDir" -ForegroundColor Gray

# --- 4. Compilacion del instalador -----------------------------------------
if (-not $IsccPath) { $IsccPath = Resolve-Iscc }
if (-not $IsccPath) { Write-Fail 'No se localizo ISCC.exe en Scripts\tools\InnoSetup ni en el PATH.' }
$iscc = $IsccPath
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
Write-Host "[4/5] Compilando el instalador con Inno Setup..." -ForegroundColor Yellow
Assert-PackagingInvariants -SetupIss $setupScript -PackagingDir $packagingDir
Invoke-Checked -Executable $iscc -ArgumentList @($setupScript, "/O$OutputDirectory")

$installer = Join-Path $OutputDirectory "ElyTesia-Setup-$Version.exe"
if (-not (Test-Path -LiteralPath $installer)) {
    throw "ISCC termino sin error pero no se encontro el instalador esperado: $installer"
}

Write-Host "      Instalador: $installer ($([math]::Round((Get-Item -LiteralPath $installer).Length / 1MB, 2)) MB)" -ForegroundColor Green

# --- Firma Authenticode opcional --------------------------------------------
# El instalador se firma DESPUES de compilarlo: Inno Setup reescribe el
# ejecutable al final, asi que firmarlo antes invalidaria la firma.
# Sin certificado el artefacto queda sin firmar (comportamiento por defecto).
if ($CertificatePath) {
    if (-not (Test-Path -LiteralPath $CertificatePath)) {
        throw "No se encontro el certificado de firma: $CertificatePath"
    }
    Write-Host "      Firmando el instalador (Authenticode)..." -ForegroundColor Cyan
    $signTool = Get-WindowsSdkTool 'signtool.exe'
    $passwordArgs = @()
    if ($CertificatePassword) {
        $plainPassword = [System.Net.NetworkCredential]::new('', $CertificatePassword).Password
        $passwordArgs = @('/p', $plainPassword)
    }
    try {
        & $signTool sign /fd SHA256 /a /f $CertificatePath @passwordArgs $installer
        if ($LASTEXITCODE -ne 0) { throw "SignTool termino con codigo $LASTEXITCODE" }
    }
    finally {
        $plainPassword = $null
    }
    # Verifica que la firma es valida con signtool: no depende del modulo
    # Microsoft.PowerShell.Security, que no esta disponible en todos los
    # entornos de compilacion.
    & $signTool verify /pa $installer *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "SignTool no pudo verificar la firma Authenticode del instalador (codigo $LASTEXITCODE)."
    }
    Write-Host "      Instalador firmado correctamente." -ForegroundColor Green
}
else {
    Write-Host "      AVISO: el instalador se genera SIN firma Authenticode (no se indico certificado)." -ForegroundColor Yellow
}

# Sincronizar copia en '12. Releases\Ely-Tesia'
$releasesGlobalDir = 'D:\Proyectos\biglexj\12. Releases\Ely-Tesia'
if (Test-Path -LiteralPath (Split-Path $releasesGlobalDir -Parent)) {
    if (-not (Test-Path -LiteralPath $releasesGlobalDir)) {
        New-Item -ItemType Directory -Path $releasesGlobalDir -Force | Out-Null
    }
    $installerLeaf = Split-Path $installer -Leaf
    Get-ChildItem -LiteralPath $releasesGlobalDir -Filter "ElyTesia-Setup-*.exe" -File |
        Where-Object { $_.Name -ne $installerLeaf } |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
    Copy-Item -LiteralPath $installer -Destination (Join-Path $releasesGlobalDir $installerLeaf) -Force
    Write-Host "      Copia sincronizada en 12. Releases\Ely-Tesia\$installerLeaf" -ForegroundColor Green
}

# --- 5. Manifiesto de winget (opcional, sin publicar) -----------------------
if ($EmitWingetManifest) {
    Write-Host "[5/5] Preparando el manifiesto de winget (sin publicar)..." -ForegroundColor Yellow
    $sha256 = Get-FileSha256 -Path $installer
    $manifestDir = Join-Path $root "packaging\winget\manifests\b\biglexj\ElyTesia\$Version"
    New-Item -ItemType Directory -Path $manifestDir -Force | Out-Null

    $versionYaml = @(
        "# yaml-language-server: `$schema=https://aka.ms/winget-manifest.version.1.10.0.schema.json",
        "",
        "PackageIdentifier: biglexj.ElyTesia",
        "PackageVersion: $Version",
        "DefaultLocale: es-PE",
        "ManifestType: version",
        "ManifestVersion: 1.10.0"
    ) -join "`n"
    [System.IO.File]::WriteAllText((Join-Path $manifestDir "biglexj.ElyTesia.yaml"), ($versionYaml + "`n"), (New-Object System.Text.UTF8Encoding($false)))

    $installerYaml = @(
        "# yaml-language-server: `$schema=https://aka.ms/winget-manifest.installer.1.10.0.schema.json",
        "",
        "PackageIdentifier: biglexj.ElyTesia",
        "PackageVersion: $Version",
        "InstallerType: exe",
        "Scope: user",
        "InstallModes:",
        "  - interactive",
        "  - silent",
        "  - silentWithProgress",
        "UpgradeBehavior: install",
        # El instalador es Inno Setup: declara los interruptores de instalacion
        # silenciosa. Sin ellos, winget avisa de que el instalador podria no
        # ejecutarse sin supervision y la instalacion automatica falla.
        "InstallerSwitches:",
        "  Silent: /VERYSILENT /SUPPRESSMSGBOXES /NORESTART",
        "  SilentWithProgress: /SILENT /SUPPRESSMSGBOXES /NORESTART",
        "ReleaseDate: $(Get-Date -Format 'yyyy-MM-dd')",
        "Installers:",
        "  - Architecture: x64",
        "    InstallerUrl: https://github.com/biglexj/Ely-Tesia/releases/download/v$Version/ElyTesia-Setup-$Version.exe",
        "    InstallerSha256: $sha256",
        "ManifestType: installer",
        "ManifestVersion: 1.10.0"
    ) -join "`n"
    [System.IO.File]::WriteAllText((Join-Path $manifestDir "biglexj.ElyTesia.installer.yaml"), ($installerYaml + "`n"), (New-Object System.Text.UTF8Encoding($false)))

    Write-Host "      Manifiesto listo en $manifestDir (InstallerType: exe)" -ForegroundColor Green

    # El manifiesto de locale (defaultLocale) NO se genera: lleva descripcion y
    # notas de release en prosa. Si falta para esta version, el manifiesto esta
    # incompleto y el PR a microsoft/winget-pkgs sera rechazado.
    $localePath = Join-Path $manifestDir "biglexj.ElyTesia.locale.es-PE.yaml"
    if (-not (Test-Path -LiteralPath $localePath)) {
        Write-Host "AVISO: falta el manifiesto de locale: $localePath" -ForegroundColor Yellow
        Write-Host "       Copia el locale de la version anterior y actualiza PackageVersion, ReleaseNotes y ReleaseNotesUrl." -ForegroundColor Yellow
    }

    # Validacion de esquema con el cliente de winget, si esta disponible.
    $winget = Get-Command winget.exe -ErrorAction SilentlyContinue
    if ($winget) {
        & $winget.Source validate --manifest $manifestDir *> $null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "      Manifiesto de winget validado correctamente." -ForegroundColor Green
        } else {
            Write-Host "AVISO: 'winget validate' rechazo el manifiesto generado (codigo $LASTEXITCODE)." -ForegroundColor Yellow
        }
    }
} else {
    Write-Host "[5/5] Manifiesto de winget no generado (usa -EmitWingetManifest)." -ForegroundColor DarkYellow
}

Write-Output $installer
