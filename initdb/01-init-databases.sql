-- Creates additional databases on first Postgres boot.
-- This script runs only once (when ./data/postgres is empty).

SELECT 'CREATE DATABASE n8n_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'n8n_db')\gexec