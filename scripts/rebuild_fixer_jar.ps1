Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$jarDir = Join-Path $projectRoot "jars"
if (-not (Test-Path $jarDir)) {
    throw "Jar directory not found: $jarDir"
}

Push-Location $jarDir
try {
    Remove-Item "Fixer*.class" -Force -ErrorAction SilentlyContinue
    $targetRelease = if ($env:FIXER_JAVA_RELEASE) { $env:FIXER_JAVA_RELEASE } else { "8" }

    $cp = @(
        "resources.jar",
        "starfarer.api.jar",
        "starfarer_obf.jar",
        "game_lwjgl.jar",
        "jinput.jar",
        "lwjgl_util.jar",
        "fs.sound_obf.jar",
        "fs.common_obf.jar",
        "janino.jar",
        "commons-compiler.jar",
        "log4j-1.2.9.jar",
        "json.jar",
        "jogg-0.0.7.jar",
        "jorbis-0.0.15.jar",
        "xstream-1.4.10.jar"
    ) -join ";"

    # Compile Fixer against a CheerpJ-compatible classfile level instead of host JDK default.
    & javac -encoding UTF-8 --release $targetRelease -cp $cp "Fixer.java"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed with exit code $LASTEXITCODE"
    }

    $classFiles = Get-ChildItem -Name "Fixer*.class" | Sort-Object
    if (-not $classFiles -or $classFiles.Count -eq 0) {
        throw "No compiled Fixer class files found."
    }

    Remove-Item "fixer.jar" -Force -ErrorAction SilentlyContinue
    & jar cf "fixer.jar" $classFiles
    if ($LASTEXITCODE -ne 0) {
        throw "jar packaging failed with exit code $LASTEXITCODE"
    }

    $entries = & jar tf "fixer.jar"
    $required = @(
        "Fixer.class",
        "Fixer`$1.class",
        "Fixer`$2.class",
        "Fixer`$DriverContext.class"
    )
    foreach ($requiredEntry in $required) {
        if (-not ($entries -contains $requiredEntry)) {
            throw "fixer.jar missing required entry: $requiredEntry"
        }
    }

    Write-Output ("Rebuilt fixer.jar with entries: " + ($classFiles -join ", "))
} finally {
    Pop-Location
}
