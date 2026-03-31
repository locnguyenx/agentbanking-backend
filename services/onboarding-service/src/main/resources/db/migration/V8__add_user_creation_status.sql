-- V8__add_user_creation_status.sql

-- Add user creation status to agent table
ALTER TABLE agent ADD COLUMN user_creation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE agent ADD COLUMN user_creation_error VARCHAR(500);

-- Add index for status queries
CREATE INDEX IF NOT EXISTS idx_agent_user_creation_status ON agent(user_creation_status);
