-- Add flow_rules column to nodes table to support modern simulation format
-- This enables rule-based simulations alongside legacy show_predicate simulations

ALTER TABLE nodes ADD COLUMN flow_rules TEXT;

-- Add comment to explain the new column
COMMENT ON COLUMN nodes.flow_rules IS 'JSON array of modern flow rules for rule-based simulations'; 