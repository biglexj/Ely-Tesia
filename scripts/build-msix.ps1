param(
    [ValidatePattern('^\d+\.\d+\.\d+\.\d+$')]
    [string]$Version = '1.0.2.0',
    [string]$Publisher = 'CN=biglexj',
    [string]$OutputPath,
    [string]$CertificatePath,
    [securestring]$CertificatePassword
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
if (-not $OutputPath) { $OutputPath = Join-Path $root "release\ElyTesia-Windows-$($Version.Substring(0, $Version.LastIndexOf('.'))).msix" }

function Find-WindowsSdkTool([string]$Name) {
    $fromPath = Get-Command $Name -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty Source
    if ($fromPath) { return $fromPath }
    $sdkRoot = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin'
    if (Test-Path $sdkRoot) {
        return Get-ChildItem $sdkRoot -Recurse -Filter $Name -File -ErrorAction SilentlyContinue |
            Where-Object { $_.DirectoryName -match '\\x64$' } |
            Sort-Object FullName -Descending |
            Select-Object -First 1 -ExpandProperty FullName
    }
}

$makeAppx = Find-WindowsSdkTool 'makeappx.exe'
if (-not $makeAppx) {
    throw 'No se encontró MakeAppx.exe. Instala Windows SDK > Windows SDK Signing Tools for Desktop Apps.'
}

$appImage = Join-Path $root 'composeApp\build\compose\binaries\main\app\ElyTesia'
if (-not (Test-Path (Join-Path $appImage 'ElyTesia.exe'))) {
    $gradleArgs = @()
    if ($env:JAVA_HOME) { $gradleArgs += "-Dorg.gradle.java.home=$env:JAVA_HOME" }
    $gradleArgs += ":composeApp:createDistributable"
    & (Join-Path $root 'gradlew.bat') @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle terminó con código $LASTEXITCODE" }
}

$workDir = Join-Path $root 'build\msix'
$packageDir = Join-Path $workDir 'package'
if (Test-Path $workDir) { Remove-Item $workDir -Recurse -Force }
New-Item $packageDir -ItemType Directory -Force | Out-Null
Copy-Item (Join-Path $appImage '*') $packageDir -Recurse -Force

$manifest = Get-Content (Join-Path $root 'packaging\windows\AppxManifest.xml.template') -Raw
$manifest = $manifest.Replace('__VERSION__', $Version).Replace('__PUBLISHER__', $Publisher)
$manifest | Set-Content (Join-Path $packageDir 'AppxManifest.xml') -Encoding utf8

$assetsDir = Join-Path $packageDir 'Assets'
New-Item $assetsDir -ItemType Directory -Force | Out-Null
$sourceIcon = Join-Path $root 'assets\branding\elytesia-icon-1024.png'
Add-Type -AssemblyName System.Drawing
function Write-Logo([string]$Name, [int]$Width, [int]$Height) {
    $source = [System.Drawing.Image]::FromFile($sourceIcon)
    try {
        $bitmap = [System.Drawing.Bitmap]::new($Width, $Height)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $side = [Math]::Min($Width, $Height)
                $x = [int](($Width - $side) / 2)
                $y = [int](($Height - $side) / 2)
                $graphics.DrawImage($source, $x, $y, $side, $side)
            } finally { $graphics.Dispose() }
            $bitmap.Save((Join-Path $assetsDir $Name), [System.Drawing.Imaging.ImageFormat]::Png)
        } finally { $bitmap.Dispose() }
    } finally { $source.Dispose() }
}
Write-Logo 'StoreLogo.png' 50 50
Write-Logo 'Square44x44Logo.png' 44 44
Write-Logo 'Square150x150Logo.png' 150 150
Write-Logo 'Wide310x150Logo.png' 310 150

$outputDirectory = Split-Path -Parent $OutputPath
New-Item $outputDirectory -ItemType Directory -Force | Out-Null
if (Test-Path $OutputPath) { Remove-Item $OutputPath -Force }
& $makeAppx pack /d $packageDir /p $OutputPath /o
if ($LASTEXITCODE -ne 0) { throw "MakeAppx terminó con código $LASTEXITCODE" }

if ($CertificatePath) {
    $signTool = Find-WindowsSdkTool 'signtool.exe'
    if (-not $signTool) { throw 'No se encontró SignTool.exe en el Windows SDK.' }
    $passwordArgs = @()
    if ($CertificatePassword) {
        $plainPassword = [System.Net.NetworkCredential]::new('', $CertificatePassword).Password
        $passwordArgs = @('/p', $plainPassword)
    }
    try {
        & $signTool sign /fd SHA256 /a /f $CertificatePath @passwordArgs $OutputPath
        if ($LASTEXITCODE -ne 0) { throw "SignTool terminó con código $LASTEXITCODE" }
    } finally { $plainPassword = $null }
}

Write-Host "MSIX generado: $OutputPath" -ForegroundColor Green
