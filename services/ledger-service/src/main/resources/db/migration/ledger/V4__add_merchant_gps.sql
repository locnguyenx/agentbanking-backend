ALTER TABLE agent_float ADD COLUMN IF NOT EXISTS merchant_gps_lat DECIMAL(9,6);
ALTER TABLE agent_float ADD COLUMN IF NOT EXISTS merchant_gps_lng DECIMAL(9,6);
ALTER TABLE journal_entry ALTER COLUMN account_code TYPE VARCHAR(64);
