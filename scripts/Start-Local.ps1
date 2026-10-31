$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

if ($env:SPRING_DATASOURCE_URL -and $env:SPRING_DATASOURCE_URL -ne 'jdbc:postgresql://127.0.0.1:55433/nclean_crm') {
    throw 'Remove SPRING_DATASOURCE_URL from this terminal. The local launcher only uses its isolated PostgreSQL database.'
}
if ($env:SPRING_FLYWAY_URL) {
    throw 'Remove SPRING_FLYWAY_URL from this terminal. Local Flyway must use the same isolated database as the application.'
}

Push-Location $projectRoot
try {
    foreach ($name in @('postgres-admin-password', 'postgres-password', 'application-local.properties')) {
        if (-not (Test-Path -LiteralPath (Join-Path '.local' $name) -PathType Leaf)) {
            throw 'Local configuration is missing. Run .\scripts\Initialize-Local.ps1 first.'
        }
    }
    & docker compose up '-d' '--wait'
    if ($LASTEXITCODE -ne 0) {
        throw 'The local PostgreSQL database did not start. Check Docker Desktop and port 55433.'
    }
    Write-Host 'Open http://127.0.0.1:8081 after the application starts. Login: local-owner.'
    & '.\mvnw.cmd' 'spring-boot:run' '-Dspring-boot.run.profiles=local' '-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://127.0.0.1:55433/nclean_crm --spring.flyway.url=jdbc:postgresql://127.0.0.1:55433/nclean_crm'
    if ($LASTEXITCODE -ne 0) {
        throw 'The CRM application exited with an error.'
    }
} finally {
    Pop-Location
}
