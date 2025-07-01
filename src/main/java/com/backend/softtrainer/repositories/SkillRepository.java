package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    
    /**
     * Find skills by generation status that were created before the given timestamp.
     * Used for timeout handling to find skills that have been processing too long.
     */
    List<Skill> findByGenerationStatusInAndTimestampBefore(
            List<SkillGenerationStatus> generationStatuses, 
            LocalDateTime timestamp
    );
}
