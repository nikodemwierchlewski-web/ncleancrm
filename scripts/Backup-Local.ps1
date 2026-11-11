param([string]$Destination)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if (-not $Destination) { $Destination = Join-Path $root ('.local/backups/nclean-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.dump') }
$destinationPath = [IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $destinationPath) { throw 'Destination already exists. Choose a new file; backups are never overwritten.' }
New-Item -ItemType Directory -Force -Path (Split-Path $destinationPath) | Out-Null
$containerFile = '/tmp/nclean-' + [guid]::NewGuid().ToString('N') + '.dump'
Push-Location $root
try {
    # Unix socket in this project's local container; no password in command arguments.
    & docker compose exec -T postgres pg_dump '-U' 'crm_bootstrap' '-d' 'nclean_crm' '-Fc' '--no-owner' '--no-acl' '-f' $containerFile
    if ($LASTEXITCODE -ne 0) { throw 'pg_dump failed.' }
    & docker compose cp "postgres:$containerFile" $destinationPath
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy backup.' }
    Get-FileHash -Algorithm SHA256 -LiteralPath $destinationPath | Format-List
    Write-Host "Backup saved: $destinationPath. It includes private receipts; keep it outside Git and protect access."
} finally {
    & docker compose exec -T postgres rm '-f' $containerFile
    Pop-Location
}
