param(
    [string]$Alias = "elytesia"
)

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$signingDirectory = Join-Path $root "signing"
$keyStorePath = Join-Path $signingDirectory "elytesia-release.jks"
$propertiesPath = Join-Path $root "keystore.properties"

if ((Test-Path $keyStorePath) -or (Test-Path $propertiesPath)) {
    throw "Ya existe una configuración de firma. No se reemplazó ningún archivo."
}

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool) {
    $javaHomeKeytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\keytool.exe" } else { $null }
    if (-not $javaHomeKeytool -or -not (Test-Path $javaHomeKeytool)) {
        throw "No se encontró keytool. Configura JAVA_HOME con un JDK 17 o posterior."
    }
    $keytoolPath = $javaHomeKeytool
} else {
    $keytoolPath = $keytool.Source
}

New-Item -ItemType Directory -Path $signingDirectory -Force | Out-Null
$password = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))

& $keytoolPath `
    -genkeypair `
    -keystore $keyStorePath `
    -storepass $password `
    -keypass $password `
    -alias $Alias `
    -keyalg RSA `
    -keysize 4096 `
    -validity 10000 `
    -dname "CN=Ely-Tesia, OU=Release, O=biglexj, L=Lima, C=PE"
if ($LASTEXITCODE -ne 0) { throw "keytool no pudo generar la clave Android." }

@"
storeFile=signing/elytesia-release.jks
storePassword=$password
keyAlias=$Alias
keyPassword=$password
"@ | Set-Content -LiteralPath $propertiesPath -Encoding UTF8 -NoNewline

Write-Host "Firma Android permanente configurada." -ForegroundColor Green
Write-Host "Guarda una copia privada de:" -ForegroundColor Yellow
Write-Host "  $keyStorePath"
Write-Host "  $propertiesPath"
