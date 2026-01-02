-- Remove the roles_name_check constraint
-- Run this script in your PostgreSQL database

-- Drop the constraint (it's not needed - application uses enum for validation)
ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check;

-- Insert ROLE_USER if it doesn't exist
INSERT INTO roles (name) VALUES ('ROLE_USER')
ON CONFLICT (name) DO NOTHING;
