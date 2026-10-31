$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

if (-not $env:JAVA_HOME) {
    throw 'Set JAVA_HOME to your JDK 17 installation before continuing.'
}
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
    throw 'JAVA_HOME does not contain bin\java.exe. Install and select JDK 17.'
}

Push-Location $projectRoot
try {
    foreach ($name in @('postgres-admin-password', 'postgres-password', 'application-local.properties')) {
        if (Test-Path -LiteralPath (Join-Path '.local' $name)) {
            throw 'Local configuration already exists; no files were overwritten. Use Start-Local.ps1.'
        }
    }
    New-Item -ItemType Directory -Path '.local' -Force | Out-Null
    $classpathFile = Join-Path $projectRoot '.local\runtime-classpath.txt'
    & '.\mvnw.cmd' '-B' '-ntp' 'dependency:build-classpath' '-DincludeScope=runtime' "-Dmdep.outputFile=$classpathFile"
    if ($LASTEXITCODE -ne 0) {
        throw 'Maven could not prepare the runtime classpath.'
    }
    $classpath = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
    & $java '-cp' $classpath (Join-Path $PSScriptRoot 'InitializeLocal.java')
    if ($LASTEXITCODE -ne 0) {
        throw 'Local configuration was not initialized.'
    }
} finally {
    Pop-Location
}
