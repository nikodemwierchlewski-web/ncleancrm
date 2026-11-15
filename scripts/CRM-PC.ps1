param(
    [ValidateSet('Init','Start','Stop','Status','Logs','Backup','Update','RestoreTest','ImportEmpty')]
    [string]$Action = 'Start',
    [string]$BackupFile,
    [string]$TargetDatabase
)
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$composeArgs = @('compose', '-f', (Join-Path $root 'compose.pc.yaml'))

function Invoke-Docker {
    param([string[]]$Arguments)
    & docker @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Docker command failed (exit $LASTEXITCODE). Read the error above; no data cleanup is performed." }
}
function Invoke-Compose {
    param([string[]]$Arguments)
    Invoke-Docker -Arguments ($composeArgs + $Arguments)
}
function Assert-Secrets {
    foreach ($name in @('postgres-admin-password','postgres-password','application-local.properties')) {
        $path = Join-Path $root ('.local/' + $name)
        if (-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-Item -LiteralPath $path).Length -eq 0) {
            throw 'Local secrets are missing. Run CRM-PC.ps1 -Action Init. Never regenerate secrets for an existing database.'
        }
    }
}

Push-Location $root
try {
    Invoke-Docker -Arguments @('info','--format','{{.ServerVersion}}')
    Invoke-Docker -Arguments @('compose','version')
    if ($Action -eq 'Init') {
        $present = @('postgres-admin-password','postgres-password','application-local.properties') |
            Where-Object { Test-Path -LiteralPath (Join-Path $root ('.local/' + $_)) }
        if (@($present).Count -gt 0 -and @($present).Count -lt 3) {
            throw 'Incomplete local configuration. Restore the original secrets; do not overwrite them.'
        }
        if (@($present).Count -eq 0) {
            & docker volume inspect 'ncleancrm-pc-data' *> $null
            if ($LASTEXITCODE -eq 0) { throw 'Existing PC volume without secrets. Restore its original .local files before continuing.' }
            New-Item -ItemType Directory -Force -Path (Join-Path $root '.local') | Out-Null
        }
        Invoke-Compose -Arguments @('build','app','postgres')
        if (@($present).Count -eq 0) {
            Invoke-Compose -Arguments @('--profile','tools','run','--rm','--no-deps','init')
        } else { Write-Host 'Using existing local secrets. No passwords were changed.' }
        Assert-Secrets
        New-Item -ItemType Directory -Force -Path (Join-Path $root '.local/pc-backups') | Out-Null
        Invoke-Docker -Arguments @('volume','create','ncleancrm-pc-data')
        Write-Host 'Ready. Run .\scripts\CRM-PC.ps1 -Action Start.'
        return
    }
    Assert-Secrets
    New-Item -ItemType Directory -Force -Path (Join-Path $root '.local/pc-backups') | Out-Null
    switch ($Action) {
        'Start' {
            # Starting does not silently build/update code or choose another volume.
            Invoke-Compose -Arguments @('up','-d','--no-build','--wait','--wait-timeout','180')
            Write-Host 'CRM: http://127.0.0.1:8081 (unless CRM_HTTP_PORT was changed). Login: local-owner.'
        }
        'Stop' { Invoke-Compose -Arguments @('stop') }
        'Status' { Invoke-Compose -Arguments @('ps') }
        'Logs' { Invoke-Compose -Arguments @('logs','--tail','100','app','postgres') }
        'Backup' { Invoke-Compose -Arguments @('--profile','tools','run','--rm','ops','backup') }
        'Update' {
            Invoke-Compose -Arguments @('stop','app')
            Invoke-Compose -Arguments @('--profile','tools','run','--rm','ops','backup')
            Invoke-Compose -Arguments @('build','app','postgres')
            Invoke-Compose -Arguments @('up','-d','--no-build','--wait','--wait-timeout','180')
            Write-Host 'Update completed. Verify login and your saved orders; copy the backup to another drive.'
        }
        { $_ -in 'RestoreTest','ImportEmpty' } {
            if (-not $BackupFile -or $BackupFile -notmatch '^[A-Za-z0-9_][A-Za-z0-9_.-]*$') { throw 'Provide -BackupFile: a filename from .local/pc-backups, without a path.' }
            if ($Action -eq 'ImportEmpty') {
                Invoke-Compose -Arguments @('stop','app')
                $operation = 'import-empty'
            } else {
                if ($TargetDatabase -notmatch '^nclean_restore_[a-z0-9_]{1,40}$') { throw 'Provide a new -TargetDatabase starting with nclean_restore_.' }
                $operation = 'restore-test'
            }
            Invoke-Compose -Arguments @('--profile','tools','run','--rm','-e',"BACKUP_FILE=$BackupFile",'-e',"RESTORE_DATABASE=$TargetDatabase",'ops',$operation)
        }
    }
} finally { Pop-Location }
