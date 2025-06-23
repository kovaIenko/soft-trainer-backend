package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    @Query("SELECT m.id, m.fileName, m.tag FROM materials m WHERE m.skill.id = :skillId")
    List<Object[]> findMaterialMetadataBySkillId(@Param("skillId") Long skillId);
} 