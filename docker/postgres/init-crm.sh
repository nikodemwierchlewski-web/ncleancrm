#!/bin/sh
set -eu

# Run once, only when Compose initializes its own empty PostgreSQL volume.
CRM_INIT_PASSWORD="$(cat /run/crm-init/app-password)"
export CRM_INIT_PASSWORD
psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    <<'SQL'
\getenv crm_password CRM_INIT_PASSWORD
CREATE ROLE crm_dev LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'crm_password';
ALTER DATABASE nclean_crm OWNER TO crm_dev;
REVOKE ALL ON DATABASE nclean_crm FROM PUBLIC;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
ALTER SCHEMA public OWNER TO crm_dev;
SQL
unset CRM_INIT_PASSWORD
rm /run/crm-init/app-password
