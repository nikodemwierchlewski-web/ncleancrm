#!/bin/sh
set -eu

# The postgres OS user cannot read the host's mode-0600 secret. Copy it to
# private, postgres-owned memory before the official entrypoint drops privileges.
if [ ! -s "${PGDATA:-/var/lib/postgresql/data}/PG_VERSION" ]; then
    install -d -m 0700 -o postgres -g postgres /run/crm-init
    install -m 0600 -o postgres -g postgres /run/secrets/crm_db_password /run/crm-init/app-password
fi
exec /usr/local/bin/docker-entrypoint.sh "$@"
