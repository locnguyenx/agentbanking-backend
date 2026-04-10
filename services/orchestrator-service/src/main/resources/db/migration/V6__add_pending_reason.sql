-- Add pending reason columns for transaction tracking
-- V6__add_pending_reason.sql

-- Add pending_reason to transaction_record for PENDING/RUNNING status
ALTER TABLE transaction_record ADD COLUMN IF NOT EXISTS pending_reason VARCHAR(100);

-- Add maker_pending_reason and checker_pending_reason to transaction_resolution_case
ALTER TABLE transaction_resolution_case ADD COLUMN IF NOT EXISTS maker_pending_reason VARCHAR(100);
ALTER TABLE transaction_resolution_case ADD COLUMN IF NOT EXISTS checker_pending_reason VARCHAR(100);