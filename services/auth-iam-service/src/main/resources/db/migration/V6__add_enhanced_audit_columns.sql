-- Add enhanced audit log columns for Bank Malaysia compliance
ALTER TABLE audit_logs 
ADD COLUMN IF NOT EXISTS trace_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS session_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS service_name VARCHAR(50) NOT NULL DEFAULT 'auth-iam-service',
ADD COLUMN IF NOT EXISTS device_info VARCHAR(255),
ADD COLUMN IF NOT EXISTS geographic_location VARCHAR(100);

-- Make sure service_name has a default
ALTER TABLE audit_logs ALTER COLUMN service_name SET DEFAULT 'auth-iam-service';