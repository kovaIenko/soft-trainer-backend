package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.messages.*;
import com.backend.softtrainer.entities.enums.TextFormat;
import com.backend.softtrainer.services.content.RichTextProcessor;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.context.SimulationContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

/**
 * 🎮 Enhanced Message Processor - Runtime Message Enhancement
 * 
 * Works alongside existing InputMessageService to gradually introduce
 * enhanced message types and processing capabilities.
 * 
 * Features:
 * - Rich text processing with variable substitution
 * - AI-enhanced content generation
 * - Interactive element rendering
 * - Context-aware message adaptation
 * - Seamless legacy compatibility
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedMessageProcessor {
    
    private final RichTextProcessor richTextProcessor;
    private final SimulationContextBuilder contextBuilder;
    
    @Value("${app.enhanced-messages.rich-text.enabled:true}")
    private boolean richTextEnabled;
    
    @Value("${app.enhanced-messages.ai-generation.enabled:true}")
    private boolean aiGenerationEnabled;
    
    @Value("${app.enhanced-messages.interactive-elements.enabled:true}")
    private boolean interactiveElementsEnabled;
    
    /**
     * 🎨 Process Message with Enhanced Features
     */
    public ProcessedMessage processMessage(Message message, SimulationContext context) {
        if (message == null) {
            return ProcessedMessage.createEmpty();
        }
        
        log.debug("🎨 Processing message type: {} with enhanced features", message.getClass().getSimpleName());
        
        if (message instanceof RichTextMessage) {
            return processRichTextMessage((RichTextMessage) message, context);
        } else if (message instanceof AiEnhancedTextMessage) {
            return processAiEnhancedMessage((AiEnhancedTextMessage) message, context);
        } else if (message instanceof EnhancedChoiceQuestion) {
            return processEnhancedChoiceQuestion((EnhancedChoiceQuestion) message, context);
        } else if (message instanceof InteractiveElementMessage) {
            return processInteractiveElementMessage((InteractiveElementMessage) message, context);
        } else if (message instanceof EnhancedMediaMessage) {
            return processEnhancedMediaMessage((EnhancedMediaMessage) message, context);
        } else {
            return processLegacyMessage(message, context);
        }
    }
    
    /**
     * 📝 Process Rich Text Message
     */
    private ProcessedMessage processRichTextMessage(RichTextMessage message, SimulationContext context) {
        if (!richTextEnabled) {
            return ProcessedMessage.createSimple(message.getContent());
        }
        
        String processedContent = richTextProcessor.processText(
                message.getContent(), 
                message.getTextFormat(), 
                context
        );
        
        return ProcessedMessage.builder()
                .content(processedContent)
                .messageType("rich_text")
                .format(message.getTextFormat().name())
                .hasDelay(message.hasDelay())
                .delaySeconds(message.getDelaySeconds())
                .hasCustomStyling(message.hasCustomStyling())
                .cssClasses(message.getCssClasses())
                .animationType(message.getAnimationType())
                .build();
    }
    
    /**
     * 🤖 Process AI-Enhanced Text Message
     */
    private ProcessedMessage processAiEnhancedMessage(AiEnhancedTextMessage message, SimulationContext context) {
        if (!aiGenerationEnabled) {
            return ProcessedMessage.createSimple(message.getContentTemplate());
        }
        
        // For now, use template content - actual AI generation would happen here
        String content = message.getContentTemplate();
        
        // Apply rich text processing if enabled
        if (richTextEnabled) {
            content = richTextProcessor.processText(content, message.getTextFormat(), context);
        }
        
        return ProcessedMessage.builder()
                .content(content)
                .messageType("ai_enhanced_text")
                .format(message.getTextFormat().name())
                .aiGenerated(message.isAiGenerationEnabled())
                .learningObjective(message.getLearningObjective())
                .difficultyLevel(message.getDifficultyLevel())
                .toneStyle(message.getToneStyle())
                .shouldCache(message.shouldCache())
                .build();
    }
    
    /**
     * 🔘 Process Enhanced Choice Question
     */
    private ProcessedMessage processEnhancedChoiceQuestion(EnhancedChoiceQuestion message, SimulationContext context) {
        return ProcessedMessage.builder()
                .content(message.getQuestionText())
                .messageType("enhanced_choice_question")
                .isMultiSelection(message.isMultiSelection())
                .minSelections(message.getEffectiveMinSelections())
                .maxSelections(message.getEffectiveMaxSelections())
                .hasTimeLimit(message.hasTimeLimit())
                .timeLimitSeconds(message.getTimeLimitSeconds())
                .layoutType(message.getLayoutType())
                .optionStyle(message.getOptionStyle())
                .enableHints(message.hasHintsEnabled())
                .showProgress(message.getShowProgress())
                .build();
    }
    
    /**
     * 🎮 Process Interactive Element Message
     */
    private ProcessedMessage processInteractiveElementMessage(InteractiveElementMessage message, SimulationContext context) {
        if (!interactiveElementsEnabled) {
            return ProcessedMessage.createSimple(message.getInstructions());
        }
        
        return ProcessedMessage.builder()
                .content(message.getInstructions())
                .messageType("interactive_element")
                .interactionType(message.getInteractionType())
                .hasTimeLimit(message.hasTimeLimit())
                .timeLimitSeconds(message.getTimeLimitSeconds())
                .maxAttempts(message.getMaxAttempts())
                .difficultyLevel(message.getDifficultyLevel())
                .feedbackMode(message.getFeedbackMode())
                .enableHints(message.hasHintsEnabled())
                .allowRetry(message.allowsRetry())
                .mobileOptimized(message.isMobileOptimized())
                .accessibilityEnabled(message.isAccessibilityEnabled())
                .build();
    }
    
    /**
     * 📸 Process Enhanced Media Message
     */
    private ProcessedMessage processEnhancedMediaMessage(EnhancedMediaMessage message, SimulationContext context) {
        return ProcessedMessage.builder()
                .content(message.getCaption())
                .messageType("enhanced_media")
                .mediaType(message.getMediaType())
                .mediaUrl(message.getMediaUrl())
                .thumbnailUrl(message.getThumbnailUrl())
                .altText(message.getAltText())
                .aspectRatio(message.getAspectRatio())
                .fileSizeMB(message.getFileSizeMB())
                .enableLazyLoading(message.isLazyLoadingEnabled())
                .autoPlay(message.isAutoPlayEnabled())
                .zoomEnabled(message.isZoomEnabled())
                .annotationEnabled(message.isAnnotationEnabled())
                .mobileOptimized(message.isMobileOptimized())
                .build();
    }
    
    /**
     * 🔧 Process Legacy Message (fallback)
     */
    private ProcessedMessage processLegacyMessage(Message message, SimulationContext context) {
        log.debug("🔧 Processing legacy message type: {}", message.getClass().getSimpleName());
        
        // For legacy messages, try to extract content
        String content = "Legacy message content";
        
        // Use reflection to try to get content field
        try {
            var contentField = message.getClass().getDeclaredField("content");
            contentField.setAccessible(true);
            Object contentValue = contentField.get(message);
            if (contentValue instanceof String) {
                content = (String) contentValue;
            }
        } catch (Exception e) {
            // If no content field, try other common fields
            try {
                var textField = message.getClass().getDeclaredField("text");
                textField.setAccessible(true);
                Object textValue = textField.get(message);
                if (textValue instanceof String) {
                    content = (String) textValue;
                }
            } catch (Exception e2) {
                log.debug("Could not extract content from legacy message: {}", e2.getMessage());
            }
        }
        
        return ProcessedMessage.createSimple(content);
    }
    
    /**
     * 🏗️ Create Simulation Context from Chat
     */
    public SimulationContext createContextFromChat(Chat chat, User user) {
        return contextBuilder.buildFromChat(chat);
    }
    
    /**
     * 📊 Processed Message Result
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessedMessage {
        private String content;
        private String messageType;
        private String format;
        private boolean aiGenerated;
        private String learningObjective;
        private String difficultyLevel;
        private String toneStyle;
        private boolean shouldCache;
        
        // Rich Text Properties
        private boolean hasDelay;
        private Integer delaySeconds;
        private boolean hasCustomStyling;
        private String cssClasses;
        private String animationType;
        
        // Choice Question Properties
        private boolean isMultiSelection;
        private Integer minSelections;
        private Integer maxSelections;
        private boolean hasTimeLimit;
        private Integer timeLimitSeconds;
        private String layoutType;
        private String optionStyle;
        private Boolean enableHints;
        private Boolean showProgress;
        
        // Interactive Element Properties
        private String interactionType;
        private Integer maxAttempts;
        private String feedbackMode;
        private boolean allowRetry;
        private boolean mobileOptimized;
        private boolean accessibilityEnabled;
        
        // Media Properties
        private String mediaType;
        private String mediaUrl;
        private String thumbnailUrl;
        private String altText;
        private double aspectRatio;
        private double fileSizeMB;
        private boolean enableLazyLoading;
        private boolean autoPlay;
        private boolean zoomEnabled;
        private boolean annotationEnabled;
        
        public static ProcessedMessage createEmpty() {
            return ProcessedMessage.builder()
                    .content("")
                    .messageType("empty")
                    .build();
        }
        
        public static ProcessedMessage createSimple(String content) {
            return ProcessedMessage.builder()
                    .content(content != null ? content : "")
                    .messageType("simple")
                    .build();
        }
    }
} 