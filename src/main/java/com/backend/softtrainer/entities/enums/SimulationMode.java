package com.backend.softtrainer.entities.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SimulationMode {
    /**
     * Traditional predefined flow using FlowNodes
     * All content is static and pre-authored
     */
    PREDEFINED("predefined", false, true),
    
    /**
     * Fully AI-generated content in real-time
     * No predefined FlowNodes, everything generated
     */
    DYNAMIC("dynamic", true, false),
    
    /**
     * Mix of predefined structure with AI enhancement
     * Uses FlowNodes as templates but allows AI modification
     */
    HYBRID("hybrid", true, true),
    
    /**
     * AI-assisted predefined flow
     * Predefined structure with AI hints and summaries
     */
    AI_ASSISTED("ai_assisted", true, true);
    
    private final String value;
    private final boolean supportsAIGeneration;
    private final boolean supportsPredefinedContent;
    
    public boolean isFullyDynamic() {
        return this == DYNAMIC;
    }
    
    public boolean isFullyStatic() {
        return this == PREDEFINED;
    }
    
    public boolean isHybrid() {
        return supportsAIGeneration && supportsPredefinedContent;
    }
} 