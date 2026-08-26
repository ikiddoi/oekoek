#Requires -Version 5.1
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ($env:SC968_CAPTURE_ENABLED -ne 'true') {
    throw 'SC968_CAPTURE_ENABLED=true is required'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($null -eq $maven) {
    $maven = Get-Command mvn -ErrorAction SilentlyContinue
}
if ($null -eq $maven) {
    throw 'Maven is required to launch standard RuneLite'
}
$mavenPath = if ($maven.Path) { $maven.Path } else { $maven.Source }
$repositoryArgument = if ([string]::IsNullOrWhiteSpace($env:SC968_MAVEN_REPO)) {
    $null
} else {
    '-Dmaven.repo.local=' + $env:SC968_MAVEN_REPO
}

Write-Output '[SC968] Starting standard RuneLite observation capture. Log in and wait for the FISH NOW prompt.'
Push-Location $projectRoot
try {
    $buildArguments = @('-q', '-DskipTests')
    if ($repositoryArgument) { $buildArguments += $repositoryArgument }
    $buildArguments += @('-pl', 'runelite-client', '-am', 'package')
    & $mavenPath @buildArguments
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $launchArguments = @('-q')
    if ($repositoryArgument) { $launchArguments += $repositoryArgument }
    $launchArguments += @(
        '-f', (Join-Path $projectRoot 'runelite-client\pom.xml'),
        'org.codehaus.mojo:exec-maven-plugin:3.5.0:java',
        '-Dexec.mainClass=net.runelite.client.RuneLite',
        '-Dexec.args=--developer-mode'
    )
    & $mavenPath @launchArguments
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
