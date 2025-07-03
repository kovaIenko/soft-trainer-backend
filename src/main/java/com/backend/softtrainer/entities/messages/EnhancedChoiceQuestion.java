package com.backend.softtrainer.entities.messages;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 🔘 Enhanced Choice Question - Advanced interactive choice questions
 * 
 * Features:
 * - Conditional option display based on context
 * - Option randomization and shuffling
 * - Dynamic option generation
 * - Advanced validation rules
 * - Multi-selection support with constraints
 */
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnhancedChoiceQuestion extends Message {
    
    @Column(length = 1000)
    private String questionText;
    
    @Column(length = 2000)
    private String options;
    
    @Column(name = "allow_multiple")
    private Boolean allowMultiple = false;
    
    @Column(name = "min_selections")
    private Integer minSelections = 1;
    
    @Column(name = "max_selections")
    private Integer maxSelections = 1;
    
    @Column(name = "randomize_options")
    private Boolean randomizeOptions = false;
    
    @Column(name = "enable_other_option")
    private Boolean enableOtherOption = false;
    
    @Column(name = "other_option_text")
    private String otherOptionText = "Other (please specify)";
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditional_options")
    private JsonNode conditionalOptions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules")
    private JsonNode validationRules;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "display_rules")
    private JsonNode displayRules;
    
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;
    
    @Column(name = "show_progress")
    private Boolean showProgress = false;
    
    @Column(name = "enable_hints")
    private Boolean enableHints = false;
    
    @Column(name = "hint_delay_seconds")
    private Integer hintDelaySeconds = 30;
    
    @Column(name = "layout_type")
    private String layoutType = "VERTICAL"; // VERTICAL, HORIZONTAL, GRID, CAROUSEL
    
    @Column(name = "option_style")
    private String optionStyle = "RADIO"; // RADIO, CHECKBOX, BUTTON, CARD, IMAGE
    
    /**
     * 🎯 Check if this is a multi-selection question
     */
    public boolean isMultiSelection() {
        return allowMultiple != null && allowMultiple;
    }
    
    /**
     * 🔀 Check if options should be randomized
     */
    public boolean shouldRandomizeOptions() {
        return randomizeOptions != null && randomizeOptions;
    }
    
    /**
     * 🔧 Check if conditional options are configured
     */
    public boolean hasConditionalOptions() {
        return conditionalOptions != null && !conditionalOptions.isEmpty();
    }
    
    /**
     * ⏰ Check if this question has a time limit
     */
    public boolean hasTimeLimit() {
        return timeLimitSeconds != null && timeLimitSeconds > 0;
    }
    
    /**
     * 💡 Check if hints are enabled
     */
    public boolean hasHintsEnabled() {
        return enableHints != null && enableHints;
    }
    
    /**
     * ➕ Check if "Other" option is enabled
     */
    public boolean hasOtherOption() {
        return enableOtherOption != null && enableOtherOption;
    }
    
    /**
     * 📏 Get effective minimum selections
     */
    public int getEffectiveMinSelections() {
        if (minSelections == null || minSelections < 0) {
            return isMultiSelection() ? 0 : 1;
        }
        return minSelections;
    }
    
    /**
     * 📏 Get effective maximum selections
     */
    public int getEffectiveMaxSelections() {
        if (maxSelections == null || maxSelections < 1) {
            return isMultiSelection() ? getOptionCount() : 1;
        }
        return maxSelections;
    }
    
    /**
     * 📊 Get the number of options available (estimate from string)
     */
    private int getOptionCount() {
        if (options == null || options.trim().isEmpty()) {
            return 10; // Default fallback
        }
        // Simple heuristic: count semicolons or newlines as option separators
        return Math.max(1, options.split("[;\n]").length);
    }
    
    /**
     * 🏗️ Create a simple single choice question
     */
    public static EnhancedChoiceQuestion createSingleChoice(String questionText) {
        return EnhancedChoiceQuestion.builder()
            .questionText(questionText)
            .allowMultiple(false)
            .minSelections(1)
            .maxSelections(1)
            .randomizeOptions(false)
            .enableOtherOption(false)
            .layoutType("VERTICAL")
            .optionStyle("RADIO")
            .build();
    }
    
    /**
     * 🏗️ Create a multi-choice question
     */
    public static EnhancedChoiceQuestion createMultiChoice(String questionText, int minSelections, int maxSelections) {
        return EnhancedChoiceQuestion.builder()
            .questionText(questionText)
            .allowMultiple(true)
            .minSelections(minSelections)
            .maxSelections(maxSelections)
            .randomizeOptions(false)
            .enableOtherOption(false)
            .layoutType("VERTICAL")
            .optionStyle("CHECKBOX")
            .build();
    }
    
    /**
     * 🔀 Create a randomized choice question
     */
    public static EnhancedChoiceQuestion createRandomized(String questionText, boolean allowMultiple) {
        return EnhancedChoiceQuestion.builder()
            .questionText(questionText)
            .allowMultiple(allowMultiple)
            .minSelections(allowMultiple ? 1 : 1)
            .maxSelections(allowMultiple ? 5 : 1)
            .randomizeOptions(true)
            .enableOtherOption(false)
            .layoutType("VERTICAL")
            .optionStyle(allowMultiple ? "CHECKBOX" : "RADIO")
            .build();
    }
    
    /**
     * ⏰ Create a timed choice question
     */
    public static EnhancedChoiceQuestion createTimed(String questionText, int timeLimitSeconds) {
        return EnhancedChoiceQuestion.builder()
            .questionText(questionText)
            .allowMultiple(false)
            .minSelections(1)
            .maxSelections(1)
            .timeLimitSeconds(timeLimitSeconds)
            .showProgress(true)
            .enableHints(true)
            .hintDelaySeconds(timeLimitSeconds / 2)
            .layoutType("VERTICAL")
            .optionStyle("BUTTON")
            .build();
    }
    
    /**
     * 🎨 Create a visual card-based choice question
     */
    public static EnhancedChoiceQuestion createCardStyle(String questionText, String layoutType) {
        return EnhancedChoiceQuestion.builder()
            .questionText(questionText)
            .allowMultiple(false)
            .minSelections(1)
            .maxSelections(1)
            .randomizeOptions(false)
            .layoutType(layoutType) // GRID or CAROUSEL
            .optionStyle("CARD")
            .showProgress(false)
            .build();
    }
    
    /**
     * 📷 Create an image-based choice question
     */
    public static EnhancedChoiceQuestion createImageChoice(String questionText, boolean allowMultiple) {
        return EnhancedChoiceQuestion.builder()
            .questionText(questionText)
            .allowMultiple(allowMultiple)
            .minSelections(allowMultiple ? 1 : 1)
            .maxSelections(allowMultiple ? 3 : 1)
            .layoutType("GRID")
            .optionStyle("IMAGE")
            .randomizeOptions(false)
            .build();
    }
} 