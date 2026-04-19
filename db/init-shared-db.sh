#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER auth_user WITH PASSWORD 'auth_password';
    CREATE DATABASE authdb;
    GRANT ALL PRIVILEGES ON DATABASE authdb TO auth_user;

    CREATE USER audit_user WITH PASSWORD 'audit_password';
    CREATE DATABASE auditdb;
    GRANT ALL PRIVILEGES ON DATABASE auditdb TO audit_user;

    CREATE USER temporal WITH PASSWORD 'temporal' CREATEDB;
    CREATE DATABASE temporal;
    CREATE DATABASE temporal_visibility;
    GRANT ALL PRIVILEGES ON DATABASE temporal TO temporal;
    GRANT ALL PRIVILEGES ON DATABASE temporal_visibility TO temporal;

    CREATE DATABASE rules_db;
    CREATE DATABASE ledger_db;
    CREATE DATABASE onboarding_db;
    CREATE DATABASE switch_db;
    CREATE DATABASE biller_db;
    CREATE DATABASE orchestrator_db;
EOSQL

# Grant public schema permissions in specific databases for Postgres 15+ compatibility
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "authdb" -c "GRANT ALL ON SCHEMA public TO auth_user;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "auditdb" -c "GRANT ALL ON SCHEMA public TO audit_user;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "temporal" -c "GRANT ALL ON SCHEMA public TO temporal;"
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "temporal_visibility" -c "GRANT ALL ON SCHEMA public TO temporal;"
