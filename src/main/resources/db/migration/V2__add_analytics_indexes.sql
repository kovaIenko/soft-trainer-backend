-- Add indexes for analytics queries
CREATE INDEX IF NOT EXISTS idx_user_hyperparams_owner_id ON user_hyperparams(owner_id);
CREATE INDEX IF NOT EXISTS idx_user_hyperparams_chat_id ON user_hyperparams(chat_id);
CREATE INDEX IF NOT EXISTS idx_user_hyperparams_simulation_id ON user_hyperparams(simulation_id);
CREATE INDEX IF NOT EXISTS idx_user_hyperparams_key ON user_hyperparams(key);
CREATE INDEX IF NOT EXISTS idx_chats_timestamp ON chats(timestamp); 