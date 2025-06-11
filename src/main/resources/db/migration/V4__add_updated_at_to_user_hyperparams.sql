ALTER TABLE user_hyperparams
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Automatically update updated_at on row modification
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS set_updated_at_on_user_hyperparams ON user_hyperparams;
CREATE TRIGGER set_updated_at_on_user_hyperparams
BEFORE UPDATE ON user_hyperparams
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column(); 