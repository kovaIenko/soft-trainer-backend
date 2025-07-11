package com.backend.softtrainer.dtos.aiagent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the current user message being processed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUserMessageDto {
    
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("message_type")
    private String messageType;
    
    @JsonProperty("content")
    private String content;
    
    @JsonProperty("selected_option")
    private String selectedOption;
    
    @JsonProperty("selected_option_id")
    private String selectedOptionId;
    
    @JsonProperty("user_answer")
    private String userAnswer;
    
    @JsonProperty("response_time")
    private Long responseTime;
} 