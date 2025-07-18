package com.backend.softtrainer.services;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;

/**
 * Service to detect character type from AI-generated character names
 */
@Service
@Slf4j
public class CharacterTypeDetector {
    
    // Keywords that indicate AI assistant/coordinator characters
    private static final Set<String> AI_ASSISTANT_KEYWORDS = Set.of(
        "ai", "assistant", "coordinator", "bot", "agent", "system", 
        "coach", "trainer", "guide", "facilitator", "mentor"
    );
    
    // Keywords that indicate workspace characters  
    private static final Set<String> WORKSPACE_KEYWORDS = Set.of(
        "manager", "colleague", "customer", "client", "employee", "worker",
        "director", "supervisor", "team lead", "specialist", "expert",
        "representative", "consultant", "analyst", "developer", "designer",
        "sales", "marketing", "hr", "finance", "operations", "executive",
        "ceo", "cto", "cfo", "vp", "president", "owner", "partner"
    );
    
    // Keywords that clearly indicate user
    private static final Set<String> USER_KEYWORDS = Set.of(
        "user", "you", "participant", "trainee", "learner", "student"
    );
    
    /**
     * Detect character type from character name and role
     */
    public CharacterTemplate.CharacterType detectCharacterType(String characterName, String characterRole) {
        if (characterName == null) {
            return CharacterTemplate.CharacterType.AI_ASSISTANT; // Default
        }
        
        String lowerName = characterName.toLowerCase().trim();
        String lowerRole = characterRole != null ? characterRole.toLowerCase().trim() : "";
        
        log.debug("🔍 Detecting character type for: '{}' with role: '{}'", characterName, characterRole);
        
        // Check for user keywords first
        if (containsAnyKeyword(lowerName, USER_KEYWORDS) || "user".equals(lowerName)) {
            log.debug("✅ Detected USER character: {}", characterName);
            return CharacterTemplate.CharacterType.USER;
        }
        
        // Check for AI assistant keywords
        if (containsAnyKeyword(lowerName, AI_ASSISTANT_KEYWORDS) || 
            containsAnyKeyword(lowerRole, AI_ASSISTANT_KEYWORDS)) {
            log.debug("✅ Detected AI_ASSISTANT character: {}", characterName);
            return CharacterTemplate.CharacterType.AI_ASSISTANT;
        }
        
        // Check for workspace keywords
        if (containsAnyKeyword(lowerName, WORKSPACE_KEYWORDS) || 
            containsAnyKeyword(lowerRole, WORKSPACE_KEYWORDS)) {
            log.debug("✅ Detected WORKSPACE character: {}", characterName);
            return CharacterTemplate.CharacterType.WORKSPACE;
        }
        
        // Default logic based on role
        if ("COACH".equalsIgnoreCase(characterRole)) {
            log.debug("✅ Detected AI_ASSISTANT (COACH role): {}", characterName);
            return CharacterTemplate.CharacterType.AI_ASSISTANT;
        }
        
        // If name contains actual person names, likely workspace character
        if (containsPersonName(lowerName)) {
            log.debug("✅ Detected WORKSPACE (person name detected): {}", characterName);
            return CharacterTemplate.CharacterType.WORKSPACE;
        }
        
        // Default to AI assistant for generic names
        log.debug("✅ Defaulting to AI_ASSISTANT: {}", characterName);
        return CharacterTemplate.CharacterType.AI_ASSISTANT;
    }
    
    /**
     * Check if text contains any of the keywords
     */
    private boolean containsAnyKeyword(String text, Set<String> keywords) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        return keywords.stream().anyMatch(keyword -> 
            text.contains(keyword) || text.equals(keyword)
        );
    }
    
    /**
     * Simple heuristic to detect if name contains common person names
     */
    private boolean containsPersonName(String name) {
        // Common first names that suggest a person
        Set<String> commonNames = Set.of(
            "john", "jane", "mike", "sarah", "david", "lisa", "chris", "maria",
            "james", "jennifer", "robert", "linda", "michael", "patricia",
            "william", "barbara", "richard", "elizabeth", "joseph", "jessica",
            "thomas", "susan", "charles", "karen", "daniel", "nancy", "matthew",
            "betty", "anthony", "helen", "mark", "sandra", "donald", "donna",
            "steven", "carol", "paul", "ruth", "andrew", "sharon", "joshua",
            "michelle", "kenneth", "laura", "kevin", "sarah", "brian", "kimberly",
            "alex", "anna", "ryan", "emma", "derek", "olivia", "sean", "sophia"
        );
        
        // Check if any common name is found in the character name
        String[] words = name.split("\\s+");
        for (String word : words) {
            if (commonNames.contains(word.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
} 