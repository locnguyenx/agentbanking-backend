-- Add reference_number column to ledger_transaction table
ALTER TABLE ledger_transaction ADD COLUMN IF NOT EXISTS reference_number VARCHAR(50);
