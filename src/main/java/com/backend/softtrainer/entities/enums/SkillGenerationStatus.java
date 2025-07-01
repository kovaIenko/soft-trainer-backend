package com.backend.softtrainer.entities.enums;

/**
 * Represents the status of AI-powered skill generation process.
 * Controls both the generation state and user visibility of skills.
 */
public enum SkillGenerationStatus {
    /**
     * AI generation is in progress.
     * Skills in this state are hidden from regular users.
     */
    GENERATING,
    
    /**
     * AI generation completed successfully.
     * Skills in this state are made visible to users.
     */
    COMPLETED,
    
    /**
     * AI generation failed.
     * Skills in this state remain hidden from users.
     */
    FAILED
} 