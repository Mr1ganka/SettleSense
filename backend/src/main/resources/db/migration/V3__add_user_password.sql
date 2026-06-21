-- Add password fields to app_user table for authentication
ALTER TABLE app_user ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE app_user ADD COLUMN salt VARCHAR(255);