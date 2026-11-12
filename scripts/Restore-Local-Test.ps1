param(
    [Parameter(Mandatory=$true)][string]$Backup,
    [Parameter(Mandatory=$true)][ValidatePattern('^nclean_restore_[a-z0-9_]{1,40}$')][string]$TargetDatabase
)
$ErrorActionPreference = 'Stop'
$backupPath = (Resolve-Path -LiteralPath $Backup).Path
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$containerFile = '/tmp/nclean-restore-' + [guid]::NewGuid().ToString('N') + '.dump'
Push-Location $root
try {
    # createdb fails if the target already exists. Never --clean, never the ordinary CRM database.
    & docker compose exec -T postgres createdb '-U' 'crm_bootstrap' '-O' 'crm_dev' $TargetDatabase
    if ($LASTEXITCODE -ne 0) { throw 'Target was not created; existing databases are never overwritten.' }
    & docker compose cp $backupPath "postgres:$containerFile"
    if ($LASTEXITCODE -ne 0) { throw 'Could not copy backup into container.' }
    & docker compose exec -T postgres pg_restore '-U' 'crm_bootstrap' '--role=crm_dev' '--no-owner' '--no-acl' '--exit-on-error' '--single-transaction' '-d' $TargetDatabase $containerFile
    if ($LASTEXITCODE -ne 0) { throw 'Restore failed. The separately created target database is left for inspection.' }
    & docker compose exec -T postgres psql '-U' 'crm_bootstrap' '-d' $TargetDatabase '-v' 'ON_ERROR_STOP=1' '-c' 'SELECT (SELECT count(*) FROM booking) AS orders, (SELECT count(*) FROM booking_payment) AS payments, (SELECT count(*) FROM fuel_receipt) AS receipts, (SELECT count(*) FROM booking_photo) AS service_photos, (SELECT count(*) FROM day_review) AS day_reviews;'
    if ($LASTEXITCODE -ne 0) { throw 'Restore verification failed.' }
    Write-Host "Restored to isolated test database: $TargetDatabase. The regular CRM database was not modified."
} finally {
    & docker compose exec -T postgres rm '-f' $containerFile
    Pop-Location
}
