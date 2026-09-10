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
    [string]$ModeOrVersion,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$RemainingArgs
)

$scriptPath = Join-Path $PSScriptRoot "scripts\build-release.ps1"
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "No se encontró el script de release en $scriptPath"
}

$passArgs = @()
if ($ModeOrVersion -match '^(?i)local|-local|-localonly$') {
    $passArgs += "-LocalOnly"
} elseif ($ModeOrVersion) {
    $passArgs += $ModeOrVersion
}

if ($RemainingArgs) {
    foreach ($arg in $RemainingArgs) {
        if ($arg -match '^(?i)local|-local|-localonly$') {
            $passArgs += "-LocalOnly"
        } else {
            $passArgs += $arg
        }
    }
}

& $scriptPath @passArgs
exit $LASTEXITCODE
