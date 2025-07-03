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
 * 📝 Rich Text Message - Enhanced text display with formatting support
 * 
 * Features:
 * - Multiple text formats (Plain, Markdown, HTML, Interactive)
 * - Variable substitution from simulation context
 * - Interactive elements (progress bars, badges, tooltips)
 * - Context-aware content adaptation
 */
@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RichTextMessage extends Message {
    
    @Column(length = 2000)
    private String content;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "text_format")
    private TextFormat textFormat = TextFormat.PLAIN_TEXT;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "format_options")
    private JsonNode formatOptions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables")
    private JsonNode variables;
    
    @Column(name = "enable_variable_substitution")
    private Boolean enableVariableSubstitution = true;
    
    @Column(name = "enable_interactive_elements")
    private Boolean enableInteractiveElements = false;
    
    @Column(name = "css_classes")
    private String cssClasses;
    
    @Column(name = "animation_type")
    private String animationType;
    
    @Column(name = "delay_seconds")
    private Integer delaySeconds = 0;
    
    /**
     * 🎯 Check if this message supports rich formatting
     */
    public boolean isRichFormatted() {
        return textFormat != null && textFormat != TextFormat.PLAIN_TEXT;
    }
    
    /**
     * 🔧 Check if this message has interactive elements
     */
    public boolean hasInteractiveElements() {
        return enableInteractiveElements != null && enableInteractiveElements &&
               (textFormat == TextFormat.INTERACTIVE_TEXT || 
                (content != null && content.contains("[[")));
    }
    
    /**
     * 📊 Check if this message uses variable substitution
     */
    public boolean usesVariableSubstitution() {
        return enableVariableSubstitution != null && enableVariableSubstitution &&
               (content != null && content.contains("{{"));
    }
    
    /**
     * 🎨 Check if this message has custom styling
     */
    public boolean hasCustomStyling() {
        return (cssClasses != null && !cssClasses.trim().isEmpty()) ||
               (animationType != null && !animationType.trim().isEmpty());
    }
    
    /**
     * ⏱️ Check if this message has a display delay
     */
    public boolean hasDelay() {
        return delaySeconds != null && delaySeconds > 0;
    }
    
    /**
     * 🏗️ Create a simple rich text message
     */
    public static RichTextMessage createSimple(String content) {
        return RichTextMessage.builder()
            .content(content)
            .textFormat(TextFormat.PLAIN_TEXT)
            .enableVariableSubstitution(false)
            .enableInteractiveElements(false)
            .delaySeconds(0)
            .build();
    }
    
    /**
     * 📋 Create a markdown formatted message
     */
    public static RichTextMessage createMarkdown(String content) {
        return RichTextMessage.builder()
            .content(content)
            .textFormat(TextFormat.MARKDOWN)
            .enableVariableSubstitution(true)
            .enableInteractiveElements(false)
            .delaySeconds(0)
            .build();
    }
    
    /**
     * 🔧 Create an interactive message with elements
     */
    public static RichTextMessage createInteractive(String content) {
        return RichTextMessage.builder()
            .content(content)
            .textFormat(TextFormat.INTERACTIVE_TEXT)
            .enableVariableSubstitution(true)
            .enableInteractiveElements(true)
            .delaySeconds(0)
            .build();
    }
    
    /**
     * 🎨 Create a styled message with CSS classes
     */
    public static RichTextMessage createStyled(String content, String cssClasses) {
        return RichTextMessage.builder()
            .content(content)
            .textFormat(TextFormat.HTML)
            .cssClasses(cssClasses)
            .enableVariableSubstitution(true)
            .enableInteractiveElements(false)
            .delaySeconds(0)
            .build();
    }
    
    /**
     * ⏱️ Create a delayed message with animation
     */
    public static RichTextMessage createAnimated(String content, String animation, int delaySeconds) {
        return RichTextMessage.builder()
            .content(content)
            .textFormat(TextFormat.HTML)
            .animationType(animation)
            .delaySeconds(delaySeconds)
            .enableVariableSubstitution(true)
            .enableInteractiveElements(false)
            .build();
    }
} 