# ==============================================================================
# ReleaseTools.ps1 — Ely-Tesia
# Ecosistema Biglex - Documentación Core (Core-Docs)
# 
# Descripción: Funciones utilitarias en PowerShell para validación de versiones,
# entorno de desarrollo, preflight de Git/GitHub CLI y verificación de firmas.
# ==============================================================================

Set-StrictMode -Version Latest

<#
.SYNOPSIS
Ejecuta un ejecutable externo y verifica que finalice con código de retorno 0.
.PARAMETER Executable
Ruta o nombre del ejecutable a ejecutar.
.PARAMETER ArgumentList
Lista de argumentos a pasar al ejecutable.
#>
function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [string]$Executable,
        [Parameter(Mandatory)] [AllowEmptyString()] [string[]]$ArgumentList
    )

    & $Executable @ArgumentList
    if ($LASTEXITCODE -ne 0) {
        throw "El comando '$Executable' terminó con código de error $LASTEXITCODE."
    }
}

<#
.SYNOPSIS
Busca y retorna la ruta a un JDK completo que incluya 'jpackage.exe'.
#>
function Get-FullJdk {
    $candidates = @(
        "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot",
        $env:JAVA_HOME,
        "C:\Program Files\Android\Android Studio\jbr"
    ) | Where-Object {
        $_ -and (Test-Path -LiteralPath (Join-Path $_ "bin\jpackage.exe"))
    }

    $jdk = $candidates | Select-Object -First 1
    if (-not $jdk) {
        throw "No se encontró un JDK completo con jpackage.exe. Configura JAVA_HOME o instala un JDK 17+ completo."
    }
    return $jdk
}

<#
.SYNOPSIS
Busca la versión más reciente de una herramienta en el Windows SDK (ej. signtool.exe).
.PARAMETER Name
Nombre del ejecutable (ej. 'signtool.exe').
#>
function Get-WindowsSdkTool {
    param([Parameter(Mandatory)] [string]$Name)

    $sdkRoot = "C:\Program Files (x86)\Windows Kits\10\bin"
    if (-not (Test-Path -LiteralPath $sdkRoot)) {
        throw "No se encontró el directorio base de Windows Kits en '$sdkRoot'."
    }

    $sdk = Get-ChildItem -LiteralPath $sdkRoot -Directory |
        Where-Object Name -Match '^10\.' |
        Sort-Object { [version]$_.Name } -Descending |
        Select-Object -First 1
    if (-not $sdk) {
        throw "No se encontró ningún SDK de Windows instalado en '$sdkRoot'."
    }

    $tool = Join-Path $sdk.FullName "x64\$Name"
    if (-not (Test-Path -LiteralPath $tool)) {
        throw "No se encontró la herramienta '$Name' en Windows SDK ('$tool')."
    }
    return $tool
}

<#
.SYNOPSIS
Valida que una cadena cumpla la especificación de versión semántica (X.Y.Z).
.PARAMETER Version
Cadena de versión a validar (ej. '1.0.9').
#>
function Assert-SemanticVersion {
    param([Parameter(Mandatory)] [string]$Version)

    if ($Version -notmatch '^(\d+)\.(\d+)\.(\d+)$') {
        throw "La versión '$Version' no cumple el formato semántico estricto mayor.menor.parche (ej. 1.0.9)."
    }
}

<#
.SYNOPSIS
Verifica los requisitos previos de Git y GitHub CLI antes de publicar un release remoto.
.PARAMETER Root
Ruta raíz del proyecto.
.PARAMETER Repository
Nombre del repositorio en GitHub en formato 'owner/repo' (ej. 'biglexj/Ely-Tesia').
.PARAMETER Tag
Tag de versión que se intentará publicar (ej. 'v1.0.9').
#>
function Assert-PublishPreflight {
    param(
        [Parameter(Mandatory)] [string]$Root,
        [Parameter(Mandatory)] [string]$Repository,
        [Parameter(Mandatory)] [string]$Tag
    )

    Push-Location $Root
    try {
        # 1. Verificar rama activa main
        $branch = (& git branch --show-current).Trim()
        if ($LASTEXITCODE -ne 0 -or $branch -ne "main") {
            throw "La publicación oficial exige estar en la rama 'main' (rama actual: '$branch')."
        }

        # 2. Verificar que el remoto origin coincida con el repositorio esperado
        $origin = (& git remote get-url origin).Trim()
        if ($LASTEXITCODE -ne 0 -or $origin -notmatch "github\.com[/:]$([regex]::Escape($Repository))(\.git)?$") {
            throw "El remoto origin ('$origin') no corresponde al repositorio esperado '${Repository}'."
        }

        # 3. Sincronización con origin/main
        Invoke-Checked git @("fetch", "origin", "main", "--tags")
        if ((& git rev-parse HEAD).Trim() -ne (& git rev-parse origin/main).Trim()) {
            throw "La rama 'main' local no está sincronizada con 'origin/main'. Realiza git pull/push antes de publicar."
        }

        # 4. Autenticación en GitHub CLI
        Invoke-Checked gh @("auth", "status")

        # 5. Comprobar existencia de Tag o Release remoto previo (evitar sobrescrituras)
        $oldPreference = $ErrorActionPreference
        $ErrorActionPreference = "SilentlyContinue"

        & git rev-parse --quiet --verify "refs/tags/$Tag" *> $null
        $tagExists = ($LASTEXITCODE -eq 0)

        & gh release view $Tag --repo $Repository *> $null
        $releaseExists = ($LASTEXITCODE -eq 0)

        $ErrorActionPreference = $oldPreference

        if ($tagExists) {
            throw "El tag '$Tag' ya existe local o remotamente. No se permite re-etiquetar versiones ya creadas."
        }
        if ($releaseExists) {
            throw "El GitHub Release '$Tag' ya existe en '${Repository}'. Incrementa la versión antes de publicar."
        }
    } finally {
        Pop-Location
    }
}

<#
.SYNOPSIS
Verifica que un archivo binario posea una firma digital Authenticode válida y emitida por el Publisher esperado.
.PARAMETER Path
Ruta al binario empaquetado (.exe, .msi, etc.).
.PARAMETER Publisher
Sujeto esperado del certificado (ej. 'CN=biglexj').
#>
function Assert-SignedArtifact {
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [string]$Publisher
    )

    $signature = Get-AuthenticodeSignature -LiteralPath $Path
    if (-not $signature.SignerCertificate -or $signature.SignerCertificate.Subject -ne $Publisher) {
        throw "La firma digital de '$Path' no es válida o no corresponde al firmante esperado '$Publisher'."
    }
}
