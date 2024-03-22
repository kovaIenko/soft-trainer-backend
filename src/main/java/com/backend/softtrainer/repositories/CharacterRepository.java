package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Character;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<Character, Long> {
}
