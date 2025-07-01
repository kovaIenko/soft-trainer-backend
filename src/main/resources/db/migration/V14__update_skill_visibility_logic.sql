-- Update existing skills visibility based on their completion status
-- Skills that already have simulations (completed) should remain visible
-- Skills without simulations should be hidden until AI generation completes

-- First, hide all skills by default
UPDATE skills SET is_hidden = true;

-- Then, make visible only skills that have simulations (indicating they were completed)
UPDATE skills 
SET is_hidden = false 
WHERE id IN (
    SELECT DISTINCT s.id 
    FROM skills s 
    JOIN simulations sim ON sim.skill_id = s.id
);

-- For skills marked as COMPLETED in generation_status, also make them visible
-- (This handles edge cases where status was set but simulations weren't created yet)
UPDATE skills 
SET is_hidden = false 
WHERE generation_status = 'COMPLETED'; 