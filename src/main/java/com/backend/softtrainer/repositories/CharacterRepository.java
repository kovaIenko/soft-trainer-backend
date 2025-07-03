package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Character;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacterRepository extends JpaRepository<Character, Long> {
  
  Optional<Character> findByFlowCharacterId(long flowCharacterId);
}
