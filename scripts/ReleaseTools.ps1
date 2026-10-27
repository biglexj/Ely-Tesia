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
Busca y retorna la ruta a un JDK 17 completo (javac.exe + jpackage.exe).

.DESCRIPTION
El proyecto compila con source/target compatibility 17, así que se exige un
JDK 17. Antes se fijaba a una versión exacta (jdk-17.0.19.10-hotspot) que ya no
está en la máquina: al no encontrarla, la función caía en $env:JAVA_HOME, que
apunta a un JDK 25 y hacía fallar Gradle con
"java.lang.IllegalArgumentException: 25.0.4.1". Ahora se busca cualquier
JDK 17 instalado (Microsoft, Adoptium o el JBR de Android Studio) y se valida
la versión mayor antes de usarlo.
#>
function Get-FullJdk {
    $candidates = New-Object System.Collections.Generic.List[string]

    # 1) Microsoft JDK 17 (cualquier parche), del más nuevo al más viejo.
    Get-ChildItem -LiteralPath 'C:\Program Files\Microsoft' -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^jdk-17(\.|$)' } |
        Sort-Object Name -Descending |
        ForEach-Object { $candidates.Add($_.FullName) }

    # 2) Otros JDK 17 habituales, por si no hay uno de Microsoft.
    Get-ChildItem -LiteralPath 'C:\Program Files\Eclipse Adoptium' -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -match '^jdk-17(\.|$)' } |
        Sort-Object Name -Descending |
        ForEach-Object { $candidates.Add($_.FullName) }

    # 3) JAVA_HOME, por si el usuario lo apunta a un JDK 17.
    if ($env:JAVA_HOME) { $candidates.Add($env:JAVA_HOME) }

    # 4) JBR de Android Studio.
    $candidates.Add('C:\Program Files\Android\Android Studio\jbr')

    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $javac = Join-Path $candidate 'bin\javac.exe'
        $jpackage = Join-Path $candidate 'bin\jpackage.exe'
        if (-not (Test-Path -LiteralPath $javac)) { continue }
        if (-not (Test-Path -LiteralPath $jpackage)) { continue }

        $version = ''
        try { $version = (Get-Item -LiteralPath $javac).VersionInfo.ProductVersion } catch { }
        $major = 0
        if ($version -match '^(\d+)') { $major = [int]$Matches[1] }
        if ($major -ne 17) { continue }

        return $candidate
    }

    throw "No se encontró un JDK 17 completo (javac.exe y jpackage.exe). Instala un JDK 17 o configura JAVA_HOME con uno."
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

<#
.SYNOPSIS
Escribe un archivo SOLO cuando su contenido cambia de verdad.

.DESCRIPTION
Reescribir un archivo con el mismo contenido actualiza su fecha de modificacion
y hace que el control de frescura (Assert-BuildFresh) considere obsoletos los
artefactos que se compilaron a partir de el. Como el pipeline de release
sincroniza la version en varios ficheros de origen, esa reescritura inutil
rompia el empaquetado en cada publicacion. Con esta funcion solo se toca el
disco cuando hay un cambio real.

.PARAMETER Path
Ruta del archivo.
.PARAMETER Content
Contenido completo a escribir.
.PARAMETER Encoding
Codificacion de destino. Por defecto UTF-8 sin BOM.

.OUTPUTS
$true si el archivo se ha escrito; $false si ya estaba al dia.
#>
function Set-FileContentIfChanged {
    param(
        [Parameter(Mandatory)] [string]$Path,
        [Parameter(Mandatory)] [AllowEmptyString()] [string]$Content,
        [System.Text.Encoding] $Encoding
    )

    if (-not $Encoding) { $Encoding = New-Object System.Text.UTF8Encoding($false) }
    $bytes = $Encoding.GetBytes($Content)

    if (Test-Path -LiteralPath $Path) {
        $current = [System.IO.File]::ReadAllBytes($Path)
        if ($current.Length -eq $bytes.Length) {
            $same = $true
            for ($i = 0; $i -lt $bytes.Length; $i++) {
                if ($current[$i] -ne $bytes[$i]) { $same = $false; break }
            }
            if ($same) { return $false }
        }
    }

    [System.IO.File]::WriteAllBytes($Path, $bytes)
    return $true
}

<#
.SYNOPSIS
Calcula el hash SHA-256 en minúsculas de un archivo de manera universal e indestructible.
.PARAMETER Path
Ruta al archivo.
#>
function Get-FileSha256 {
    param([Parameter(Mandatory)] [string]$Path)

    if (Get-Command Get-FileHash -ErrorAction SilentlyContinue) {
        return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    }

    Import-Module Microsoft.PowerShell.Utility -ErrorAction SilentlyContinue
    if (Get-Command Get-FileHash -ErrorAction SilentlyContinue) {
        return (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash.ToLowerInvariant()
    }

    # Fallback determinista usando .NET BCL directamente (indestructible en cualquier PowerShell/entorno)
    # El tipo del stream se fija de forma explicita: sin el cast, PowerShell
    # marca ComputeHash como ambiguo entre las sobrecargas Stream y byte[].
    # El archivo se abre con FileShare.ReadWrite y con reintentos: el
    # antivirus puede retener un binario recien escrito durante unos segundos
    # y abortar el build de release con "archivo en uso".
    $hasher = [System.Security.Cryptography.SHA256]::Create()
    $stream = $null
    try {
        for ($attempt = 1; $attempt -le 5; $attempt++) {
            try {
                $stream = [System.IO.File]::Open(
                    $Path,
                    [System.IO.FileMode]::Open,
                    [System.IO.FileAccess]::Read,
                    [System.IO.FileShare]::ReadWrite)
                break
            } catch [System.IO.IOException] {
                if ($attempt -eq 5) { throw }
                Start-Sleep -Milliseconds (500 * $attempt)
            }
        }
        if (-not $stream) { throw "No se pudo abrir '$Path' para el calculo SHA-256." }
        $bytes = $hasher.ComputeHash([System.IO.Stream]$stream)
        return (-join ($bytes | ForEach-Object { "{0:x2}" -f $_ }))
    } finally {
        if ($stream) { $stream.Dispose() }
        $hasher.Dispose()
    }
}

