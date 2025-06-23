ALTER TABLE skills ADD COLUMN type VARCHAR(255);
ALTER TABLE skills ADD COLUMN behavior VARCHAR(255);
ALTER TABLE skills ADD COLUMN simulation_count INT;
ALTER TABLE skills ADD COLUMN is_hidden BOOLEAN;
ALTER TABLE skills ADD COLUMN is_protected BOOLEAN;

CREATE TABLE materials (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    tag VARCHAR(255),
    file_content BYTEA,
    skill_id BIGINT,
    FOREIGN KEY (skill_id) REFERENCES skills(id)
); 