package com.backend.softtrainer.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json", columnDefinition = "TEXT")
    private JsonNode paramsJson;
    
    private String source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "overview_json", columnDefinition = "TEXT")
    private JsonNode overviewJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 