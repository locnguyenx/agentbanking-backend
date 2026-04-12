-- Add error_details for detailed error messages
-- V7__add_error_details.sql

-- Add error_details to transaction_record for detailed error information
ALTER TABLE transaction_record ADD COLUMN IF NOT EXISTS error_details TEXT;

-- Note: pending_reason remains VARCHAR(100) but will be truncated to summary
-- error_details stores full error info for troubleshooting