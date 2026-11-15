#!/bin/sh
set -eu
umask 077
export PGHOST=postgres PGPORT=5432 PGUSER=crm_bootstrap PGDATABASE=nclean_crm
PGPASSWORD="$(cat /run/secrets/postgres_admin_password)"
export PGPASSWORD

fail() { echo "$*" >&2; exit 1; }

case "${1:-}" in
  backup)
    # mktemp reserves a unique name; incomplete files are never called .dump.
    temporary=$(mktemp /backups/nclean-$(date +%Y%m%d-%H%M%S)-XXXXXX.partial)
    trap 'rm -f "$temporary"' EXIT HUP INT TERM
    pg_dump --format=custom --no-owner --no-acl --file="$temporary"
    pg_restore --list "$temporary" >/dev/null
    name=$(basename "$temporary" .partial).dump
    cd /backups
    mv "$temporary" "$name"
    sha256sum "$name" > "$name.sha256"
    echo "Backup verified: $name (database, service photos and receipts)."
    ;;
  restore-test|import-empty)
    case "${BACKUP_FILE:-}" in
      ''|*[!a-zA-Z0-9_.-]*|.*) fail 'Set BACKUP_FILE to a backup filename, without a path.' ;;
    esac
    cd /backups
    [ -f "$BACKUP_FILE" ] && [ -f "$BACKUP_FILE.sha256" ] || fail 'Backup or SHA-256 sidecar missing.'
    # Do not execute paths or options supplied in the sidecar.
    expected=$(cut -d ' ' -f1 "$BACKUP_FILE.sha256")
    [ "${#expected}" -eq 64 ] || fail 'Invalid checksum.'
    case "$expected" in *[!a-fA-F0-9]*) fail 'Invalid checksum.' ;; esac
    actual=$(sha256sum "$BACKUP_FILE" | cut -d ' ' -f1)
    [ "$expected" = "$actual" ] || fail 'Backup checksum mismatch; no database was changed.'
    pg_restore --list "$BACKUP_FILE" >/dev/null
    if [ "$1" = restore-test ]; then
      target=${RESTORE_DATABASE:-}
      case "$target" in nclean_restore_?*) ;; *) fail 'Use a new nclean_restore_... database.' ;; esac
      case "$target" in *[!a-z0-9_]* ) fail 'Invalid database name.' ;; esac
      [ "${#target}" -le 63 ] || fail 'Database name is too long.'
      # Existing targets fail. Never --clean, DROP or restore over the live CRM.
      createdb --owner=crm_dev "$target"
    else
      target=nclean_crm
      count=$(psql -At -v ON_ERROR_STOP=1 -c "SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema')")
      [ "$count" = 0 ] || fail 'Import refused: CRM database is not empty.'
    fi
    pg_restore --role=crm_dev --no-owner --no-acl --exit-on-error --single-transaction --dbname="$target" "$BACKUP_FILE"
    psql -d "$target" -v ON_ERROR_STOP=1 -c 'SELECT (SELECT count(*) FROM booking) AS orders, (SELECT count(*) FROM booking_photo) AS photos, (SELECT count(*) FROM fuel_receipt) AS receipts;'
    echo "Restore verified: $target. Check the data in the application before relying on this backup."
    ;;
  *) fail 'Use backup, restore-test or import-empty.' ;;
esac
