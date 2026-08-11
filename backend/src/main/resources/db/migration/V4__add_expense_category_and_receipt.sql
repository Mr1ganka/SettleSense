-- Add category and receipt_url columns to expense table
ALTER TABLE expense ADD COLUMN category VARCHAR(50) NOT NULL DEFAULT 'GENERAL';
ALTER TABLE expense ADD COLUMN receipt_url VARCHAR(500);
