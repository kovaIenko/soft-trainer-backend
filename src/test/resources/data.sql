-- Initialize default roles (from V1__insertDefaultValues.sql)
INSERT INTO roles (id, name) VALUES (1, 'ROLE_USER');
INSERT INTO roles (id, name) VALUES (2, 'ROLE_OWNER');
INSERT INTO roles (id, name) VALUES (3, 'ROLE_ADMIN');

-- Initialize default organizations
INSERT INTO organizations (id, name) VALUES (1, 'SoftTrainer');
INSERT INTO organizations (id, name) VALUES (5, 'Onboarding');

-- Create test user with all roles (password is 'password')
-- Using 'test-admin' as both username and email to match @WithMockUser
INSERT INTO users (id, email, username, password, organization_id) 
VALUES (1, 'test-admin', 'test-admin', '$2a$10$E6lEIWn7DyKGqPNIQHnAkuUFwGYTk1q2fGGvnEQcZlFEqoGi5HGpG', 1);

-- Assign all roles to test user (USER, ADMIN, OWNER)
INSERT INTO user_roles (user_id, role_id) VALUES (1, 1); -- ROLE_USER
INSERT INTO user_roles (user_id, role_id) VALUES (1, 2); -- ROLE_OWNER  
INSERT INTO user_roles (user_id, role_id) VALUES (1, 3); -- ROLE_ADMIN 

-- Add required prompts for simulation completion
INSERT INTO prompts (id, name, prompt, is_on, assistant_id) VALUES 
(1, 'SIMULATION_SUMMARY', 'Analyze the user''s performance in this simulation and provide a brief summary of their strengths and areas for improvement.', true, 1);

-- NOTE: Skills and characters will be created through the import logic during tests
-- This ensures we test the actual skill creation and organization association functionality

-- Set sequence values to avoid conflicts (start after our manual inserts)
-- This ensures auto-generated IDs don't conflict with our test data
ALTER SEQUENCE users_seq RESTART WITH 100;