package com.backend.softtrainer.entities.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ContentSource {
    /**
     * Content comes directly from a predefined FlowNode
     */
    PREDEFINED("predefined"),
    
    /**
     * Content generated in real-time by AI
     */
    AI_GENERATED("ai_generated"),
    
    /**
     * Predefined content enhanced/modified by AI
     */
    AI_ENHANCED("ai_enhanced"),
    
    /**
     * User-provided content/input
     */
    USER_INPUT("user_input"),
    
    /**
     * Content from a reusable template
     */
    TEMPLATE("template"),
    
    /**
     * System-generated fallback content
     */
    SYSTEM_FALLBACK("system_fallback");
    
    private final String value;
    
    public boolean isAIInvolved() {
        return this == AI_GENERATED || 
               this == AI_ENHANCED;
    }
    
    public boolean isPredefined() {
        return this == PREDEFINED || 
               this == TEMPLATE;
    }
    
    public boolean requiresAICapability() {
        return isAIInvolved();
    }
} 