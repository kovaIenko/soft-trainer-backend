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
 * 🎮 Interactive Element Message - Advanced interaction components
 * 
 * Supported Interaction Types:
 * - DRAG_AND_DROP: Sorting, matching, categorization
 * - SLIDER_INPUT: Numerical range selection, priority ranking
 * - DRAWING_CANVAS: Sketch responses, diagram annotation
 * - VOICE_RECORDING: Audio input, pronunciation practice
 * - FILE_UPLOAD: Document submission, image analysis
 * - VIDEO_ANNOTATION: Video marking, timestamp comments
 */
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InteractiveElementMessage extends Message {
    
    @Column(name = "interaction_type")
    private String interactionType; // DRAG_AND_DROP, SLIDER_INPUT, DRAWING_CANVAS, VOICE_RECORDING, etc.
    
    @Column(length = 1000)
    private String instructions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "element_configuration")
    private JsonNode elementConfiguration;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules")
    private JsonNode validationRules;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scoring_criteria")
    private JsonNode scoringCriteria;
    
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;
    
    @Column(name = "max_attempts")
    private Integer maxAttempts = 3;
    
    @Column(name = "enable_hints")
    private Boolean enableHints = true;
    
    @Column(name = "hint_threshold")
    private Integer hintThreshold = 30; // Show hint after 30 seconds
    
    @Column(name = "auto_submit")
    private Boolean autoSubmit = false;
    
    @Column(name = "allow_retry")
    private Boolean allowRetry = true;
    
    @Column(name = "feedback_mode")
    private String feedbackMode = "IMMEDIATE"; // IMMEDIATE, DELAYED, END_OF_SESSION
    
    @Column(name = "difficulty_level")
    private String difficultyLevel = "MEDIUM"; // EASY, MEDIUM, HARD
    
    @Column(name = "accessibility_mode")
    private Boolean accessibilityMode = false;
    
    @Column(name = "mobile_optimized")
    private Boolean mobileOptimized = true;
    
    /**
     * 🎯 Check if this is a drag-and-drop interaction
     */
    public boolean isDragAndDrop() {
        return "DRAG_AND_DROP".equals(interactionType);
    }
    
    /**
     * 🎚️ Check if this is a slider input
     */
    public boolean isSliderInput() {
        return "SLIDER_INPUT".equals(interactionType);
    }
    
    /**
     * 🎨 Check if this is a drawing canvas
     */
    public boolean isDrawingCanvas() {
        return "DRAWING_CANVAS".equals(interactionType);
    }
    
    /**
     * 🎤 Check if this is voice recording
     */
    public boolean isVoiceRecording() {
        return "VOICE_RECORDING".equals(interactionType);
    }
    
    /**
     * 📁 Check if this is file upload
     */
    public boolean isFileUpload() {
        return "FILE_UPLOAD".equals(interactionType);
    }
    
    /**
     * ⏰ Check if this interaction has a time limit
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
     * 🔄 Check if retries are allowed
     */
    public boolean allowsRetry() {
        return allowRetry != null && allowRetry;
    }
    
    /**
     * 📱 Check if optimized for mobile
     */
    public boolean isMobileOptimized() {
        return mobileOptimized != null && mobileOptimized;
    }
    
    /**
     * ♿ Check if accessibility mode is enabled
     */
    public boolean isAccessibilityEnabled() {
        return accessibilityMode != null && accessibilityMode;
    }
    
    /**
     * 🏗️ Create a drag-and-drop sorting exercise
     */
    public static InteractiveElementMessage createDragAndDropSort(String instructions) {
        return InteractiveElementMessage.builder()
            .interactionType("DRAG_AND_DROP")
            .instructions(instructions)
            .timeLimitSeconds(120)
            .maxAttempts(3)
            .enableHints(true)
            .hintThreshold(45)
            .feedbackMode("IMMEDIATE")
            .difficultyLevel("MEDIUM")
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 🎚️ Create a priority ranking slider
     */
    public static InteractiveElementMessage createPrioritySlider(String instructions) {
        return InteractiveElementMessage.builder()
            .interactionType("SLIDER_INPUT")
            .instructions(instructions)
            .timeLimitSeconds(90)
            .maxAttempts(2)
            .enableHints(true)
            .hintThreshold(30)
            .feedbackMode("IMMEDIATE")
            .difficultyLevel("EASY")
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 🎨 Create a drawing/sketching exercise
     */
    public static InteractiveElementMessage createDrawingCanvas(String instructions) {
        return InteractiveElementMessage.builder()
            .interactionType("DRAWING_CANVAS")
            .instructions(instructions)
            .timeLimitSeconds(300)
            .maxAttempts(1)
            .enableHints(false)
            .autoSubmit(false)
            .feedbackMode("DELAYED")
            .difficultyLevel("HARD")
            .mobileOptimized(false) // Better on desktop/tablet
            .build();
    }
    
    /**
     * 🎤 Create a voice recording exercise
     */
    public static InteractiveElementMessage createVoiceRecording(String instructions) {
        return InteractiveElementMessage.builder()
            .interactionType("VOICE_RECORDING")
            .instructions(instructions)
            .timeLimitSeconds(60)
            .maxAttempts(3)
            .enableHints(true)
            .hintThreshold(20)
            .autoSubmit(true)
            .feedbackMode("IMMEDIATE")
            .difficultyLevel("MEDIUM")
            .accessibilityMode(true)
            .build();
    }
    
    /**
     * 📁 Create a file upload exercise
     */
    public static InteractiveElementMessage createFileUpload(String instructions) {
        return InteractiveElementMessage.builder()
            .interactionType("FILE_UPLOAD")
            .instructions(instructions)
            .maxAttempts(2)
            .enableHints(true)
            .hintThreshold(60)
            .autoSubmit(false)
            .feedbackMode("DELAYED")
            .difficultyLevel("EASY")
            .mobileOptimized(true)
            .build();
    }
    
    /**
     * 🎮 Create a matching game
     */
    public static InteractiveElementMessage createMatchingGame(String instructions, String difficulty) {
        return InteractiveElementMessage.builder()
            .interactionType("DRAG_AND_DROP")
            .instructions(instructions)
            .timeLimitSeconds("EASY".equals(difficulty) ? 180 : "MEDIUM".equals(difficulty) ? 120 : 90)
            .maxAttempts("EASY".equals(difficulty) ? 5 : "MEDIUM".equals(difficulty) ? 3 : 2)
            .enableHints(true)
            .hintThreshold("EASY".equals(difficulty) ? 60 : "MEDIUM".equals(difficulty) ? 45 : 30)
            .feedbackMode("IMMEDIATE")
            .difficultyLevel(difficulty)
            .mobileOptimized(true)
            .build();
    }
} 