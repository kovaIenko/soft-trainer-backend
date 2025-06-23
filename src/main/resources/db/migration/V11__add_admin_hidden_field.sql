-- Add is_admin_hidden field for soft deletion functionality
ALTER TABLE skills ADD COLUMN is_admin_hidden BOOLEAN DEFAULT false;

-- Update existing records to have is_admin_hidden = false
UPDATE skills SET is_admin_hidden = false WHERE is_admin_hidden IS NULL;

-- Add NOT NULL constraint
ALTER TABLE skills ALTER COLUMN is_admin_hidden SET NOT NULL; 