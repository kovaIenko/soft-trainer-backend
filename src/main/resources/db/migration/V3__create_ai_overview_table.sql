CREATE TABLE ai_overview (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(16) NOT NULL, -- PROFILE, TEAM, COMPANY
    entity_id BIGINT NOT NULL,
    overview_text TEXT NOT NULL,
    prompt_id BIGINT,
    prompt_used TEXT,
    llm_model VARCHAR(64),
    params_json JSONB,
    source VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_overview_entity ON ai_overview(entity_type, entity_id); 