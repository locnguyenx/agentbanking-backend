-- V4__add_agent_code_column.sql

-- Add agent_code column for EXTERNAL users (agent reference code from onboarding service)
ALTER TABLE users ADD COLUMN IF NOT EXISTS agent_code VARCHAR(50);
