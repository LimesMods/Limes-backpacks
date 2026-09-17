# GUI borders now follow Slot coordinates, rather than vanilla chest texture slices.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & .\gradlew.bat verifyBackpackGui
    if ($LASTEXITCODE -ne 0) { throw 'Backpack GUI verification failed.' }
} finally { Pop-Location }
