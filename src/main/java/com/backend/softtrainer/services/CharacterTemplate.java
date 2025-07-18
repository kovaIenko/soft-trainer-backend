package com.backend.softtrainer.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template for creating characters with predefined avatars based on type and gender
 * 
 * Character Types:
 * - AI_ASSISTANT: AI coordinator/assistant characters
 * - WORKSPACE: Characters representing people in the workplace scenario  
 * - USER: The user themselves (always null character)
 * 
 * Gender variants: MALE, FEMALE
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterTemplate {
    
    private CharacterType type;
    private Gender gender;
    private String avatarUrl;
    
    public enum CharacterType {
        AI_ASSISTANT,    // AI coordinator, AI assistant, etc.
        WORKSPACE,       // Manager, colleague, customer, etc.
        USER            // The user (always gets null character)
    }
    
    public enum Gender {
        MALE,
        FEMALE
    }
} 