package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.enums.TextFormat;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 🤖 AI-Enhanced Text Message - Dynamic content with AI generation
 * 
 * Features:
 * - Real-time AI content generation based on user context
 * - Adaptive difficulty and personalization
 * - Dynamic educational hints and feedback
 * - Context-aware language and tone adjustment
 * - Learning objective integration
 */
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiEnhancedTextMessage extends Message {
    
    @Column(length = 2000)
    private String contentTemplate;
    
    @Column(length = 2000)
    private String fallbackContent;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "text_format")
    private TextFormat textFormat = TextFormat.MARKDOWN;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_generation_params")
    private JsonNode aiGenerationParams;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_requirements")
    private JsonNode contextRequirements;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personalization_rules")
    private JsonNode personalizationRules;
    
    @Column(name = "enable_ai_generation")
    private Boolean enableAiGeneration = true;
    
    @Column(name = "ai_prompt_template")
    private String aiPromptTemplate;
    
    @Column(name = "learning_objective")
    private String learningObjective;
    
    @Column(name = "difficulty_level")
    private String difficultyLevel = "ADAPTIVE"; // EASY, MEDIUM, HARD, ADAPTIVE
    
    @Column(name = "tone_style")
    private String toneStyle = "PROFESSIONAL"; // CASUAL, PROFESSIONAL, ENCOURAGING, CHALLENGING
    
    @Column(name = "max_length")
    private Integer maxLength = 500;
    
    @Column(name = "min_length")
    private Integer minLength = 50;
    
    @Column(name = "cache_duration_minutes")
    private Integer cacheDurationMinutes = 60;
    
    @Column(name = "regenerate_threshold")
    private Double regenerateThreshold = 0.7; // Regenerate if context changes significantly
    
    @Column(name = "include_emoji")
    private Boolean includeEmoji = true;
    
    @Column(name = "include_examples")
    private Boolean includeExamples = true;
    
    /**
     * 🤖 Check if AI generation is enabled
     */
    public boolean isAiGenerationEnabled() {
        return enableAiGeneration != null && enableAiGeneration;
    }
    
    /**
     * 📊 Check if content should be personalized
     */
    public boolean shouldPersonalize() {
        return personalizationRules != null && !personalizationRules.isEmpty();
    }
    
    /**
     * 🎯 Check if this has a specific learning objective
     */
    public boolean hasLearningObjective() {
        return learningObjective != null && !learningObjective.trim().isEmpty();
    }
    
    /**
     * 📐 Check if difficulty is adaptive
     */
    public boolean isAdaptiveDifficulty() {
        return "ADAPTIVE".equals(difficultyLevel);
    }
    
    /**
     * 💾 Check if content should be cached
     */
    public boolean shouldCache() {
        return cacheDurationMinutes != null && cacheDurationMinutes > 0;
    }
    
    /**
     * 🔄 Check if content should be regenerated based on context changes
     */
    public boolean shouldRegenerate(double contextSimilarity) {
        return regenerateThreshold != null && contextSimilarity < regenerateThreshold;
    }
    
    /**
     * 🏗️ Create a simple AI-enhanced message
     */
    public static AiEnhancedTextMessage createSimple(String contentTemplate, String learningObjective) {
        return AiEnhancedTextMessage.builder()
            .contentTemplate(contentTemplate)
            .learningObjective(learningObjective)
            .textFormat(TextFormat.MARKDOWN)
            .enableAiGeneration(true)
            .difficultyLevel("ADAPTIVE")
            .toneStyle("PROFESSIONAL")
            .maxLength(300)
            .minLength(50)
            .includeEmoji(true)
            .includeExamples(false)
            .build();
    }
    
    /**
     * 🎓 Create an educational message with examples
     */
    public static AiEnhancedTextMessage createEducational(String contentTemplate, String learningObjective) {
        return AiEnhancedTextMessage.builder()
            .contentTemplate(contentTemplate)
            .learningObjective(learningObjective)
            .textFormat(TextFormat.MARKDOWN)
            .enableAiGeneration(true)
            .difficultyLevel("ADAPTIVE")
            .toneStyle("ENCOURAGING")
            .maxLength(600)
            .minLength(100)
            .includeEmoji(true)
            .includeExamples(true)
            .cacheDurationMinutes(30)
            .build();
    }
    
    /**
     * 💪 Create a challenging message for advanced users
     */
    public static AiEnhancedTextMessage createChallenging(String contentTemplate, String learningObjective) {
        return AiEnhancedTextMessage.builder()
            .contentTemplate(contentTemplate)
            .learningObjective(learningObjective)
            .textFormat(TextFormat.MARKDOWN)
            .enableAiGeneration(true)
            .difficultyLevel("HARD")
            .toneStyle("CHALLENGING")
            .maxLength(400)
            .minLength(75)
            .includeEmoji(false)
            .includeExamples(true)
            .regenerateThreshold(0.8)
            .build();
    }
    
    /**
     * 🎯 Create a personalized message with context awareness
     */
    public static AiEnhancedTextMessage createPersonalized(String contentTemplate, String learningObjective) {
        return AiEnhancedTextMessage.builder()
            .contentTemplate(contentTemplate)
            .learningObjective(learningObjective)
            .textFormat(TextFormat.INTERACTIVE_TEXT)
            .enableAiGeneration(true)
            .difficultyLevel("ADAPTIVE")
            .toneStyle("PROFESSIONAL")
            .maxLength(500)
            .minLength(75)
            .includeEmoji(true)
            .includeExamples(true)
            .cacheDurationMinutes(45)
            .regenerateThreshold(0.6)
            .build();
    }
} 