ALTER TABLE audit_logs 
ADD COLUMN IF NOT EXISTS trace_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS session_id VARCHAR(100),
ADD COLUMN IF NOT EXISTS device_info VARCHAR(500),
ADD COLUMN IF NOT EXISTS geographic_location VARCHAR(200);

CREATE INDEX IF NOT EXISTS idx_audit_logs_trace_id ON audit_logs(trace_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_session_id ON audit_logs(session_id);