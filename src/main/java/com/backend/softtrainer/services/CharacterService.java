package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.repositories.CharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dynamic Character Service - Creates characters based on AI-agent responses
 * 
 * This service processes character names from AI responses and:
 * 1. Detects character type (AI assistant, workspace, user)
 * 2. Detects gender (male/female) from name
 * 3. Checks if character already exists in database
 * 4. Creates new character with appropriate avatar if needed
 * 5. Returns Character object for message assignment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CharacterService {
    
    private final CharacterRepository characterRepository;
    private final CharacterTypeDetector typeDetector;
    private final GenderDetector genderDetector;
    
    // Avatar URLs for different character types and genders
    // TODO: Replace with actual S3 URLs
    private static final Map<String, String> AVATAR_URLS = new HashMap<>() {{
        // AI Assistant avatars
        put("AI_ASSISTANT_MALE", "https://softtrainer.s3.eu-north-1.amazonaws.com/characters/ai-assistant-male.png");
        put("AI_ASSISTANT_FEMALE", "https://softtrainer.s3.eu-north-1.amazonaws.com/characters/ai-assistant-female.png");
        
        // Workspace character avatars  
        put("WORKSPACE_MALE", "https://softtrainer.s3.eu-north-1.amazonaws.com/characters/workspace-male.png");
        put("WORKSPACE_FEMALE", "https://softtrainer.s3.eu-north-1.amazonaws.com/characters/workspace-female.png");
        
        // User avatars (though user characters should be null)
        put("USER_MALE", "https://softtrainer.s3.eu-north-1.amazonaws.com/characters/user-male.png");
        put("USER_FEMALE", "https://softtrainer.s3.eu-north-1.amazonaws.com/characters/user-female.png");
    }};
    
    /**
     * Main method: Process character from AI response
     * This is called whenever AI returns a character name in a message
     */
    public Character processCharacterFromAiResponse(String characterName, String characterRole) {
        if (characterName == null || characterName.trim().isEmpty()) {
            log.debug("🔍 Character name is null/empty, returning null");
            return null;
        }
        
        String trimmedName = characterName.trim();
        log.info("🎭 Processing character from AI response: '{}' with role: '{}'", trimmedName, characterRole);
        
        // Step 1: Detect character type
        CharacterTemplate.CharacterType type = typeDetector.detectCharacterType(trimmedName, characterRole);
        
        // Step 2: For USER type, always return null (legacy behavior)
        if (type == CharacterTemplate.CharacterType.USER) {
            log.debug("✅ Character '{}' is USER type, returning null", trimmedName);
            return null;
        }
        
        // Step 3: Check if character already exists by name
        Optional<Character> existingCharacter = findExistingCharacterByName(trimmedName);
        if (existingCharacter.isPresent()) {
            log.debug("✅ Found existing character: {}", trimmedName);
            return existingCharacter.get();
        }
        
        // Step 4: Create new character
        Character newCharacter = createNewCharacter(trimmedName, characterRole, type);
        log.info("✅ Created new character: '{}' (type: {}, gender: {})", 
                newCharacter.getName(), type, genderDetector.detectGender(trimmedName));
        
        return newCharacter;
    }
    
    /**
     * Find existing character by name (case-insensitive)
     */
    private Optional<Character> findExistingCharacterByName(String characterName) {
        // Since we don't have findByName method, we need to find by exact name match
        // For now, we'll use a flowCharacterId pattern based on name hash
        long nameHash = Math.abs(characterName.toLowerCase().hashCode());
        return characterRepository.findByFlowCharacterId(nameHash);
    }
    
    /**
     * Create new character with appropriate avatar
     */
    private Character createNewCharacter(String characterName, String characterRole, CharacterTemplate.CharacterType type) {
        // Detect gender
        CharacterTemplate.Gender gender = genderDetector.detectGender(characterName);
        
        // Get appropriate avatar URL
        String avatarKey = type.name() + "_" + gender.name();
        String avatarUrl = AVATAR_URLS.get(avatarKey);
        
        if (avatarUrl == null) {
            log.warn("⚠️ No avatar URL found for key: {}, using default", avatarKey);
            avatarUrl = AVATAR_URLS.get("AI_ASSISTANT_MALE"); // Fallback
        }
        
        // Create unique flowCharacterId based on name hash
        long flowCharacterId = Math.abs(characterName.toLowerCase().hashCode());
        
        // Build and save character
        Character character = Character.builder()
                .name(characterName)
                .avatar(avatarUrl)
                .flowCharacterId(flowCharacterId)
                .build();
        
        Character savedCharacter = characterRepository.save(character);
        
        log.info("🎭 Created character: name='{}', type={}, gender={}, avatar={}", 
                characterName, type, gender, avatarUrl);
        
        return savedCharacter;
    }
    
    /**
     * Get avatar URL for character type and gender
     */
    public String getAvatarUrl(CharacterTemplate.CharacterType type, CharacterTemplate.Gender gender) {
        String key = type.name() + "_" + gender.name();
        return AVATAR_URLS.getOrDefault(key, AVATAR_URLS.get("AI_ASSISTANT_MALE"));
    }
    
    /**
     * Update avatar URLs (for configuration)
     */
    public void updateAvatarUrl(CharacterTemplate.CharacterType type, CharacterTemplate.Gender gender, String url) {
        String key = type.name() + "_" + gender.name();
        AVATAR_URLS.put(key, url);
        log.info("🔧 Updated avatar URL for {}: {}", key, url);
    }
    
    /**
     * Check if character exists by name
     */
    public boolean characterExists(String characterName) {
        if (characterName == null || characterName.trim().isEmpty()) {
            return false;
        }
        
        return findExistingCharacterByName(characterName.trim()).isPresent();
    }
    
    /**
     * Get character statistics for debugging
     */
    public Map<String, Object> getCharacterStats() {
        long totalCharacters = characterRepository.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_characters", totalCharacters);
        stats.put("avatar_templates", AVATAR_URLS.size());
        
        return stats;
    }
} 