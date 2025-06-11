package com.backend.softtrainer.repositories;

import com.backend.softtrainer.entities.AiOverview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiOverviewRepository extends JpaRepository<AiOverview, Long> {
    @Query("SELECT a FROM AiOverview a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC LIMIT 1")
    Optional<AiOverview> findLatestByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);
} 