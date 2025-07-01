-- Increase skill description column length to support up to 3000 words (approximately 20,000 characters)
ALTER TABLE skills ALTER COLUMN description TYPE VARCHAR(20000); 