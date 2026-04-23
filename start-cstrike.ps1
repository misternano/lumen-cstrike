param(
    [string]$JarPath = "build\libs\cstrike-1.0-SNAPSHOT.jar",
    [string]$JavaPath = "java"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "CSTRIKE jar not found at '$JarPath'. Run '.\gradlew.bat build' first or pass -JarPath."
}

& $JavaPath `
    --enable-native-access=ALL-UNNAMED `
    --sun-misc-unsafe-memory-access=allow `
    -jar $JarPath
