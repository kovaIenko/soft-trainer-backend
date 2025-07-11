package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a generated message from AI agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGeneratedMessageDto {
    
    @JsonProperty("message_type")
    private String messageType;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("options")
    private List<String> options;
    
    @JsonProperty("character_name")
    private String characterName;
    
    @JsonProperty("character_role")
    private String characterRole;
    
    @JsonProperty("requires_response")
    private Boolean requiresResponse;
    
    @JsonProperty("response_time_limit")
    private Long responseTimeLimit;
    
    @JsonProperty("hint")
    private String hint;
    
    @JsonProperty("correct_answer_position")
    private Integer correctAnswerPosition;
    
    @JsonProperty("metadata")
    private Object metadata;
} 