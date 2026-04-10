-- Add missing columns to ledger_transaction table for comprehensive detail tracking
ALTER TABLE ledger_transaction 
ADD COLUMN IF NOT EXISTS agent_tier VARCHAR(50),
ADD COLUMN IF NOT EXISTS target_bin VARCHAR(20),
ADD COLUMN IF NOT EXISTS biller_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS ref1 VARCHAR(100),
ADD COLUMN IF NOT EXISTS ref2 VARCHAR(100),
ADD COLUMN IF NOT EXISTS destination_account VARCHAR(100);

COMMENT ON COLUMN ledger_transaction.agent_tier IS 'Tier of the agent at transaction time';
COMMENT ON COLUMN ledger_transaction.target_bin IS 'Target BIN for card/switch operations';
COMMENT ON COLUMN ledger_transaction.biller_code IS 'Biller code for bill payment transactions';
COMMENT ON COLUMN ledger_transaction.ref1 IS 'Reference 1 for bill payments';
COMMENT ON COLUMN ledger_transaction.ref2 IS 'Reference 2 for bill payments';
COMMENT ON COLUMN ledger_transaction.destination_account IS 'Destination account for deposits/transfers';
