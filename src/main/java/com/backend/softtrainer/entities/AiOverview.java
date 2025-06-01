package com.backend.softtrainer.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_overview")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiOverview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String entityType; // PROFILE, TEAM, COMPANY
    private Long entityId;
    @Column(columnDefinition = "TEXT")
    private String overviewText;

    @Column(columnDefinition = "TEXT")
    private String promptUsed;
    private String llmModel;
    @Column(columnDefinition = "jsonb")
    private String paramsJson;
    private String source;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 