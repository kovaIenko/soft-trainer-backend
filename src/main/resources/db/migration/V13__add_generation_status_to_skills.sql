-- Add generation_status column to skills table
ALTER TABLE skills ADD COLUMN generation_status VARCHAR(20) DEFAULT 'GENERATING';

-- Update existing skills to COMPLETED status (assuming they were created before this feature)
UPDATE skills SET generation_status = 'COMPLETED' WHERE generation_status IS NULL; 