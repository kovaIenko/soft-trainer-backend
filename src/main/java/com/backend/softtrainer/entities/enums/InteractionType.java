package com.backend.softtrainer.entities.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum InteractionType {
    /**
     * Simple text display, no user input required
     */
    TEXT_DISPLAY("text_display", false, false),
    
    /**
     * Single choice question with predefined options
     */
    SINGLE_CHOICE("single_choice", true, true),
    
    /**
     * Multiple choice question allowing multiple selections
     */
    MULTI_CHOICE("multi_choice", true, true),
    
    /**
     * Open text input from user
     */
    OPEN_TEXT("open_text", true, false),
    
    /**
     * Open text with AI classification/enhancement
     */
    AI_ENHANCED_TEXT("ai_enhanced_text", true, false),
    
    /**
     * Media content (images, videos)
     */
    MEDIA_CONTENT("media_content", false, false),
    
    /**
     * AI-generated hint message
     */
    AI_HINT("ai_hint", false, false),
    
    /**
     * Simulation result/summary
     */
    RESULT_SUMMARY("result_summary", false, false),
    
    /**
     * Dynamically generated response based on context
     */
    DYNAMIC_RESPONSE("dynamic_response", false, false);
    
    private final String value;
    private final boolean requiresUserInput;
    private final boolean hasPreDefinedOptions;
    
    public boolean isActionable() {
        return requiresUserInput;
    }
    
    public boolean isAIGenerated() {
        return this == AI_ENHANCED_TEXT || 
               this == AI_HINT || 
               this == DYNAMIC_RESPONSE;
    }
    
    public boolean supportsOptions() {
        return hasPreDefinedOptions;
    }
    
    /**
     * Map from legacy MessageType to new InteractionType
     */
    public static InteractionType fromMessageType(MessageType messageType) {
        return switch (messageType) {
            case TEXT -> TEXT_DISPLAY;
            case SINGLE_CHOICE_QUESTION -> SINGLE_CHOICE;
            case SINGLE_CHOICE_TASK -> SINGLE_CHOICE;
            case MULTI_CHOICE_TASK -> MULTI_CHOICE;
            case ENTER_TEXT_QUESTION -> OPEN_TEXT;
            case IMAGES, VIDEOS -> MEDIA_CONTENT;
            case HINT_MESSAGE -> AI_HINT;
            case RESULT_SIMULATION -> RESULT_SUMMARY;
            default -> TEXT_DISPLAY;
        };
    }
} 