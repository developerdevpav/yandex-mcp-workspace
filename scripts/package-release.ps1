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
$InputDir = Join-Path $WorkDir "input"
$ImageDir = Join-Path $WorkDir "image"
$BundleName = "yandex-mcp-workspace-$Version-$Classifier"
$BundleDir = Join-Path $WorkDir $BundleName

try {
    New-Item -ItemType Directory -Force -Path $InputDir, $ImageDir, $BundleDir, $SmokeDir | Out-Null
    Copy-Item -LiteralPath $TrackerJar -Destination (Join-Path $InputDir "yandex-mcp-tracker.jar")
    Copy-Item -LiteralPath $WikiJar -Destination (Join-Path $InputDir "yandex-mcp-wiki.jar")

    & jpackage `
        --type app-image `
        --name yandex-mcp-tracker `
        --dest $ImageDir `
        --input $InputDir `
        --main-jar yandex-mcp-tracker.jar `
        --main-class org.springframework.boot.loader.launch.JarLauncher `
        --add-launcher "yandex-mcp-wiki=packaging/jpackage/wiki.properties" `
        --win-console `
        --vendor Sorface `
        --description "Yandex Tracker and Wiki MCP servers"
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE"
    }

    $AppContainer = Join-Path $BundleDir "app"
    New-Item -ItemType Directory -Force -Path $AppContainer | Out-Null
    Move-Item -LiteralPath (Join-Path $ImageDir "yandex-mcp-tracker") -Destination $AppContainer
    Copy-Item -LiteralPath "packaging/release/README.txt" -Destination (Join-Path $BundleDir "README.txt")
    Set-Content -LiteralPath (Join-Path $BundleDir "VERSION") -Value $Version -Encoding ascii

    $TrackerLauncher = Join-Path $AppContainer "yandex-mcp-tracker\yandex-mcp-tracker.exe"
    $WikiLauncher = Join-Path $AppContainer "yandex-mcp-tracker\yandex-mcp-wiki.exe"
    $PreviousConfigPath = $env:YANDEX_CONFIG_PATH
    $PreviousTokenPath = $env:YANDEX_TOKEN_STORE_PATH
    $env:YANDEX_CONFIG_PATH = Join-Path $SmokeDir "config.properties"
    $env:YANDEX_TOKEN_STORE_PATH = Join-Path $SmokeDir "tokens.json"

    try {
        & $TrackerLauncher doctor --logging.level.root=ERROR
        if ($LASTEXITCODE -ne 0) { throw "Tracker smoke test failed" }
        & $WikiLauncher doctor --logging.level.root=ERROR
        if ($LASTEXITCODE -ne 0) { throw "Wiki smoke test failed" }
    }
    finally {
        $env:YANDEX_CONFIG_PATH = $PreviousConfigPath
        $env:YANDEX_TOKEN_STORE_PATH = $PreviousTokenPath
    }

    New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
    $Archive = Join-Path $OutputDir "$BundleName.zip"
    Compress-Archive -Path $BundleDir -DestinationPath $Archive -CompressionLevel Optimal
    Write-Output "Created $Archive"
}
finally {
    Remove-Item -LiteralPath $WorkDir, $SmokeDir -Recurse -Force -ErrorAction SilentlyContinue
}
