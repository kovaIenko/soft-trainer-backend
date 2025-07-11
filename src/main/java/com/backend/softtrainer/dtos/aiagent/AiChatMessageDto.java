package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO representing a chat message for AI agent context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDto {
    
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("message_type")
    private String messageType;
    
    @JsonProperty("role")
    private String role; // USER or ASSISTANT
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("character_name")
    private String characterName;
    
    @JsonProperty("options")
    private List<String> options;
    
    @JsonProperty("user_answer")
    private String userAnswer;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("requires_response")
    private Boolean requiresResponse;
} 