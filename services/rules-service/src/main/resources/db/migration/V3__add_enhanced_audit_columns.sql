-- Add enhanced audit log columns for Bank Malaysia compliance
ALTER TABLE audit_logs 
ADD COLUMN IF NOT EXISTS outcome VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500),
ADD COLUMN IF NOT EXISTS trace_id VARCHAR(50),
ADD COLUMN IF NOT EXISTS session_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS service_name VARCHAR(50) NOT NULL DEFAULT 'rules-service',
ADD COLUMN IF NOT EXISTS device_info VARCHAR(255),
ADD COLUMN IF NOT EXISTS geographic_location VARCHAR(100);

ALTER TABLE audit_logs ALTER COLUMN outcome SET DEFAULT 'SUCCESS';
ALTER TABLE audit_logs ALTER COLUMN service_name SET DEFAULT 'rules-service';