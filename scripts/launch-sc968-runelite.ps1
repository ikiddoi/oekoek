#Requires -Version 5.1
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($env:SC968_CAPTURE_ENABLED -ne 'true') {
    throw 'SC968_CAPTURE_ENABLED=true is required'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradlePath = Join-Path $projectRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $gradlePath -PathType Leaf)) {
    throw 'The RuneLite Gradle wrapper is required to launch standard RuneLite'
}
$contractVersion = $env:SC968_CAPTURE_CONTRACT_VERSION
if ([string]::IsNullOrWhiteSpace($contractVersion)) {
    throw 'SC968_CAPTURE_CONTRACT_VERSION is required to launch standard RuneLite'
}

Write-Output '[SC968] Starting standard RuneLite observation capture. Log in and wait for the FISH NOW prompt.'
Push-Location $projectRoot
try {
    & $gradlePath --no-daemon --no-watch-fs :client:runSc968
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
