<#
.SYNOPSIS
    Wrapper raíz de conveniencia para invocar el pipeline de release de Ely-Tesia.
.DESCRIPTION
    Redirige automáticamente a scripts/build-release.ps1 soportando sintaxis intuitiva:
    - .\build-release.ps1 local
    - .\build-release.ps1 -LocalOnly
    - .\build-release.ps1 1.0.9
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$Version,
    [switch]$LocalOnly,
    [switch]$SkipWindows,
    [switch]$SkipAndroid,
    [switch]$SkipTests,
    # Generar el instalador oficial de Windows con Inno Setup.
    # Sin este flag el build produce el EXE de jpackage (formato legacy).
    [switch]$BuildInno,
    # Firmar el instalador EXE con un certificado Authenticode.
    [string]$InstallerCertificate,
    [securestring]$InstallerCertificatePassword,
    # Mensaje del commit de release (la primera linea es el asunto).
    [string]$CommitMessage,
    # Flujo MSIX heredado: se acepta para no romper invocaciones previas, pero
    # MSIX esta descartado como formato de distribucion oficial.
    [switch]$SkipMsix,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RemainingArgs
)

# Detectar si se pasó 'local' como primer parámetro posicional
if ($Version -match '^(?i)local|-local|-localonly$') {
    $Version = $null
    $LocalOnly = $true
}

$scriptPath = Join-Path $PSScriptRoot "scripts\build-release.ps1"
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "No se encontró el script de release en $scriptPath"
}

$splat = @{}
if ($Version) { $splat['Version'] = $Version }
if ($LocalOnly) { $splat['LocalOnly'] = $true }
if ($SkipWindows) { $splat['SkipWindows'] = $true }
if ($SkipAndroid) { $splat['SkipAndroid'] = $true }
if ($SkipTests) { $splat['SkipTests'] = $true }
if ($BuildInno) { $splat['BuildInno'] = $true }
if ($SkipMsix) { $splat['SkipMsix'] = $true }
if ($InstallerCertificate) { $splat['InstallerCertificate'] = $InstallerCertificate }
if ($InstallerCertificatePassword) { $splat['InstallerCertificatePassword'] = $InstallerCertificatePassword }
if ($CommitMessage) { $splat['CommitMessage'] = $CommitMessage }

& $scriptPath @splat @RemainingArgs
exit $LASTEXITCODE
