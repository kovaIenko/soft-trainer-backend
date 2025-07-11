-- Add simulation type field to support AI-generated simulations
-- This enables distinguishing between predefined and AI-generated simulations

ALTER TABLE simulations ADD COLUMN type VARCHAR(50) DEFAULT 'PREDEFINED';

-- Update existing simulations to be marked as predefined
UPDATE simulations SET type = 'PREDEFINED' WHERE type IS NULL;

-- Make the column non-nullable now that all records have values
ALTER TABLE simulations ALTER COLUMN type SET NOT NULL;

-- Add index for better query performance
CREATE INDEX idx_simulations_type ON simulations(type);

-- Add comment to explain the column
COMMENT ON COLUMN simulations.type IS 'Type of simulation: PREDEFINED (legacy/modern with nodes) or AI_GENERATED (real-time AI generation)'; 