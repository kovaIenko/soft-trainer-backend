-- Fix NULL values in boolean columns for existing skills
UPDATE skills SET is_hidden = false WHERE is_hidden IS NULL;
UPDATE skills SET is_protected = false WHERE is_protected IS NULL;

-- Add NOT NULL constraints to prevent future NULL values
ALTER TABLE skills ALTER COLUMN is_hidden SET NOT NULL;
ALTER TABLE skills ALTER COLUMN is_protected SET NOT NULL;

-- Set default values for new records
ALTER TABLE skills ALTER COLUMN is_hidden SET DEFAULT false;
ALTER TABLE skills ALTER COLUMN is_protected SET DEFAULT false; 