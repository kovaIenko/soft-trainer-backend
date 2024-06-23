package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Prompt;
import com.backend.softtrainer.entities.PromptName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<Prompt, Long> {

  Optional<Prompt> findFirstByNameOrderByIdDesc(@Param("name") final PromptName name);

}
