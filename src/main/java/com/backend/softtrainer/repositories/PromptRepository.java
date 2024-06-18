package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.PromptName;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptRepository extends JpaRepository<Prompt, PromptName> {
}
