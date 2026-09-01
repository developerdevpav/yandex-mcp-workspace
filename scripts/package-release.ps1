param(
    [Parameter(Mandatory = $true)][string]$Version,
    [Parameter(Mandatory = $true)][string]$Classifier,
    [Parameter(Mandatory = $true)][string]$TrackerJar,
    [Parameter(Mandatory = $true)][string]$WikiJar,
    [Parameter(Mandatory = $true)][string]$OutputDir
)

$ErrorActionPreference = "Stop"

if ($Version -notmatch '^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$') {
    throw "Invalid release version: $Version"
}
if ($Classifier -ne "windows-x64") {
    throw "Invalid Windows classifier: $Classifier"
}
if ([System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture -ne
    [System.Runtime.InteropServices.Architecture]::X64) {
    throw "Classifier windows-x64 requires an x64 Windows host"
}
if (-not (Test-Path -LiteralPath $TrackerJar -PathType Leaf) -or
    -not (Test-Path -LiteralPath $WikiJar -PathType Leaf)) {
    throw "Tracker and Wiki JAR files must exist"
}
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    throw "jpackage is required (JDK 21)"
}

$WorkDir = Join-Path ([System.IO.Path]::GetTempPath()) ("yandex-mcp-release-" + [guid]::NewGuid())
$SmokeDir = Join-Path ([System.IO.Path]::GetTempPath()) ("yandex-mcp-smoke-" + [guid]::NewGuid())

function New-PortablePackage {
    param(
        [Parameter(Mandatory = $true)][string]$Component,
        [Parameter(Mandatory = $true)][string]$SourceJar,
        [Parameter(Mandatory = $true)][string]$Description
    )

    $AppName = "yandex-mcp-$Component"
    $BundleName = "$AppName-$Version-$Classifier"
    $BundleDir = Join-Path $WorkDir $BundleName
    $InputDir = Join-Path $WorkDir "input-$Component"
    $ImageDir = Join-Path $WorkDir "image-$Component"
    $IconPath = "packaging/icons/windows/$AppName.ico"

    if (-not (Test-Path -LiteralPath $IconPath -PathType Leaf)) {
        throw "Application icon not found: $IconPath"
    }

    New-Item -ItemType Directory -Force -Path $InputDir, $ImageDir, $BundleDir | Out-Null
    Copy-Item -LiteralPath $SourceJar -Destination (Join-Path $InputDir "$AppName.jar")

    & jpackage `
        --type app-image `
        --name $AppName `
        --dest $ImageDir `
        --input $InputDir `
        --main-jar "$AppName.jar" `
        --main-class org.springframework.boot.loader.launch.JarLauncher `
        --icon $IconPath `
        --win-console `
        --vendor Sorface `
        --description $Description
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed for $Component with exit code $LASTEXITCODE"
    }

    Move-Item -LiteralPath (Join-Path $ImageDir $AppName) -Destination (Join-Path $BundleDir "app")
    Copy-Item -LiteralPath "packaging/release/$Component-README.txt" -Destination (Join-Path $BundleDir "README.txt")
    Set-Content -LiteralPath (Join-Path $BundleDir "VERSION") -Value $Version -Encoding ascii
    Set-Content -LiteralPath (Join-Path $BundleDir "COMPONENT") -Value $Component -Encoding ascii

    $Launcher = Join-Path $BundleDir "app\$AppName.exe"
    & $Launcher doctor --logging.level.root=ERROR
    if ($LASTEXITCODE -ne 0) {
        throw "$Component smoke test failed"
    }

    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    $Archive = Join-Path $OutputDir "$BundleName.zip"
    Compress-Archive -Path $BundleDir -DestinationPath $Archive -CompressionLevel Optimal
    Write-Output "Created $Archive"
}

try {
    New-Item -ItemType Directory -Force -Path $WorkDir, $SmokeDir | Out-Null

    $PreviousConfigPath = $env:YANDEX_CONFIG_PATH
    $PreviousTokenPath = $env:YANDEX_TOKEN_STORE_PATH
    $env:YANDEX_CONFIG_PATH = Join-Path $SmokeDir "config.properties"
    $env:YANDEX_TOKEN_STORE_PATH = Join-Path $SmokeDir "tokens.json"

    try {
        New-PortablePackage -Component "tracker" -SourceJar $TrackerJar -Description "Yandex Tracker MCP server"
        New-PortablePackage -Component "wiki" -SourceJar $WikiJar -Description "Yandex Wiki MCP server"
    }
    finally {
        $env:YANDEX_CONFIG_PATH = $PreviousConfigPath
        $env:YANDEX_TOKEN_STORE_PATH = $PreviousTokenPath
    }
}
finally {
    Remove-Item -LiteralPath $WorkDir, $SmokeDir -Recurse -Force -ErrorAction SilentlyContinue
}
