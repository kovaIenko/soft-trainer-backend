package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.entities.enums.TextFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 🧪 Phase 2 Message Types Test Suite
 * 
 * Comprehensive testing for all enhanced message types:
 * - Rich Text Messages
 * - Enhanced Choice Questions
 * - AI-Enhanced Text Messages
 * - Interactive Element Messages
 * - Enhanced Media Messages
 */
@DisplayName("📋 Phase 2: Enhanced Message Types")
class Phase2MessageTypesTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Nested
    @DisplayName("📝 Rich Text Message Tests")
    class RichTextMessageTests {
        
        @Test
        @DisplayName("🏗️ Should create simple rich text message")
        void shouldCreateSimpleRichTextMessage() {
            // Given
            String content = "Welcome to the **training session**!";
            
            // When
            RichTextMessage message = RichTextMessage.createSimple(content);
            
            // Then
            assertNotNull(message);
            assertEquals(content, message.getContent());
            assertEquals(TextFormat.PLAIN_TEXT, message.getTextFormat());
            assertFalse(message.getEnableVariableSubstitution());
            assertFalse(message.getEnableInteractiveElements());
            assertEquals(0, message.getDelaySeconds());
        }
        
        @Test
        @DisplayName("📋 Should create markdown formatted message")
        void shouldCreateMarkdownMessage() {
            // Given
            String content = "## Learning Objectives\n\n- **Communication** skills\n- *Active listening*";
            
            // When
            RichTextMessage message = RichTextMessage.createMarkdown(content);
            
            // Then
            assertNotNull(message);
            assertEquals(content, message.getContent());
            assertEquals(TextFormat.MARKDOWN, message.getTextFormat());
            assertTrue(message.getEnableVariableSubstitution());
            assertFalse(message.getEnableInteractiveElements());
            assertTrue(message.isRichFormatted());
        }
        
        @Test
        @DisplayName("🔧 Should create interactive message with elements")
        void shouldCreateInteractiveMessage() {
            // Given
            String content = "Your progress: [[progress:communication,10]]";
            
            // When
            RichTextMessage message = RichTextMessage.createInteractive(content);
            
            // Then
            assertNotNull(message);
            assertEquals(content, message.getContent());
            assertEquals(TextFormat.INTERACTIVE_TEXT, message.getTextFormat());
            assertTrue(message.getEnableVariableSubstitution());
            assertTrue(message.getEnableInteractiveElements());
            assertTrue(message.hasInteractiveElements());
        }
        
        @Test
        @DisplayName("🎨 Should create styled message with CSS")
        void shouldCreateStyledMessage() {
            // Given
            String content = "<div class='highlight'>Important information</div>";
            String cssClasses = "highlight success-message";
            
            // When
            RichTextMessage message = RichTextMessage.createStyled(content, cssClasses);
            
            // Then
            assertNotNull(message);
            assertEquals(content, message.getContent());
            assertEquals(cssClasses, message.getCssClasses());
            assertEquals(TextFormat.HTML, message.getTextFormat());
            assertTrue(message.hasCustomStyling());
        }
        
        @Test
        @DisplayName("⏱️ Should create animated message with delay")
        void shouldCreateAnimatedMessage() {
            // Given
            String content = "This message appears with animation";
            String animation = "fadeIn";
            int delay = 3;
            
            // When
            RichTextMessage message = RichTextMessage.createAnimated(content, animation, delay);
            
            // Then
            assertNotNull(message);
            assertEquals(content, message.getContent());
            assertEquals(animation, message.getAnimationType());
            assertEquals(delay, message.getDelaySeconds());
            assertTrue(message.hasDelay());
            assertTrue(message.hasCustomStyling());
        }
    }
    
    @Nested
    @DisplayName("🔘 Enhanced Choice Question Tests")
    class EnhancedChoiceQuestionTests {
        
        @Test
        @DisplayName("🏗️ Should create single choice question")
        void shouldCreateSingleChoiceQuestion() {
            // Given
            String questionText = "What is the best approach for giving feedback?";
            
            // When
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createSingleChoice(questionText);
            
            // Then
            assertNotNull(question);
            assertEquals(questionText, question.getQuestionText());
            assertFalse(question.isMultiSelection());
            assertEquals(1, question.getEffectiveMinSelections());
            assertEquals(1, question.getEffectiveMaxSelections());
            assertEquals("VERTICAL", question.getLayoutType());
            assertEquals("RADIO", question.getOptionStyle());
        }
        
        @Test
        @DisplayName("🏗️ Should create multi-choice question with constraints")
        void shouldCreateMultiChoiceQuestion() {
            // Given
            String questionText = "Select all effective communication techniques:";
            int minSelections = 2;
            int maxSelections = 4;
            
            // When
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createMultiChoice(questionText, minSelections, maxSelections);
            
            // Then
            assertNotNull(question);
            assertEquals(questionText, question.getQuestionText());
            assertTrue(question.isMultiSelection());
            assertEquals(minSelections, question.getEffectiveMinSelections());
            assertEquals(maxSelections, question.getEffectiveMaxSelections());
            assertEquals("CHECKBOX", question.getOptionStyle());
        }
        
        @Test
        @DisplayName("🔀 Should create randomized choice question")
        void shouldCreateRandomizedQuestion() {
            // Given
            String questionText = "Choose the appropriate response:";
            
            // When
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createRandomized(questionText, false);
            
            // Then
            assertNotNull(question);
            assertEquals(questionText, question.getQuestionText());
            assertTrue(question.shouldRandomizeOptions());
            assertFalse(question.isMultiSelection());
        }
        
        @Test
        @DisplayName("⏰ Should create timed choice question")
        void shouldCreateTimedQuestion() {
            // Given
            String questionText = "Quick decision needed - choose fast!";
            int timeLimit = 30;
            
            // When
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createTimed(questionText, timeLimit);
            
            // Then
            assertNotNull(question);
            assertEquals(questionText, question.getQuestionText());
            assertEquals(timeLimit, question.getTimeLimitSeconds());
            assertTrue(question.hasTimeLimit());
            assertTrue(question.getShowProgress());
            assertTrue(question.hasHintsEnabled());
            assertEquals("BUTTON", question.getOptionStyle());
        }
        
        @Test
        @DisplayName("🎨 Should create card-style question")
        void shouldCreateCardStyleQuestion() {
            // Given
            String questionText = "Select the best visual approach:";
            String layoutType = "GRID";
            
            // When
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createCardStyle(questionText, layoutType);
            
            // Then
            assertNotNull(question);
            assertEquals(questionText, question.getQuestionText());
            assertEquals(layoutType, question.getLayoutType());
            assertEquals("CARD", question.getOptionStyle());
        }
        
        @Test
        @DisplayName("📷 Should create image choice question")
        void shouldCreateImageChoiceQuestion() {
            // Given
            String questionText = "Which images show good body language?";
            
            // When
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createImageChoice(questionText, true);
            
            // Then
            assertNotNull(question);
            assertEquals(questionText, question.getQuestionText());
            assertTrue(question.isMultiSelection());
            assertEquals("GRID", question.getLayoutType());
            assertEquals("IMAGE", question.getOptionStyle());
        }
    }
    
    @Nested
    @DisplayName("🤖 AI-Enhanced Text Message Tests")
    class AiEnhancedTextMessageTests {
        
        @Test
        @DisplayName("🏗️ Should create simple AI-enhanced message")
        void shouldCreateSimpleAiMessage() {
            // Given
            String template = "Hello {{user_name}}, let's work on {{learning_objective}}";
            String objective = "active listening";
            
            // When
            AiEnhancedTextMessage message = AiEnhancedTextMessage.createSimple(template, objective);
            
            // Then
            assertNotNull(message);
            assertEquals(template, message.getContentTemplate());
            assertEquals(objective, message.getLearningObjective());
            assertTrue(message.isAiGenerationEnabled());
            assertTrue(message.hasLearningObjective());
            assertTrue(message.isAdaptiveDifficulty());
            assertEquals(300, message.getMaxLength());
        }
        
        @Test
        @DisplayName("🎓 Should create educational message with examples")
        void shouldCreateEducationalMessage() {
            // Given
            String template = "Let's explore {{learning_objective}} with practical examples";
            String objective = "conflict resolution";
            
            // When
            AiEnhancedTextMessage message = AiEnhancedTextMessage.createEducational(template, objective);
            
            // Then
            assertNotNull(message);
            assertEquals(template, message.getContentTemplate());
            assertEquals(objective, message.getLearningObjective());
            assertTrue(message.getIncludeExamples());
            assertEquals("ENCOURAGING", message.getToneStyle());
            assertEquals(600, message.getMaxLength());
            assertTrue(message.shouldCache());
        }
        
        @Test
        @DisplayName("💪 Should create challenging message")
        void shouldCreateChallengingMessage() {
            // Given
            String template = "Advanced scenario: {{learning_objective}}";
            String objective = "strategic communication";
            
            // When
            AiEnhancedTextMessage message = AiEnhancedTextMessage.createChallenging(template, objective);
            
            // Then
            assertNotNull(message);
            assertEquals(template, message.getContentTemplate());
            assertEquals(objective, message.getLearningObjective());
            assertEquals("HARD", message.getDifficultyLevel());
            assertEquals("CHALLENGING", message.getToneStyle());
            assertFalse(message.getIncludeEmoji());
            assertEquals(0.8, message.getRegenerateThreshold());
            assertTrue(message.shouldRegenerate(0.7));
        }
        
        @Test
        @DisplayName("🎯 Should create personalized message")
        void shouldCreatePersonalizedMessage() {
            // Given
            String template = "Based on your progress, let's focus on {{learning_objective}}";
            String objective = "presentation skills";
            
            // When
            AiEnhancedTextMessage message = AiEnhancedTextMessage.createPersonalized(template, objective);
            
            // Then
            assertNotNull(message);
            assertEquals(template, message.getContentTemplate());
            assertEquals(objective, message.getLearningObjective());
            assertEquals(TextFormat.INTERACTIVE_TEXT, message.getTextFormat());
            assertEquals(0.6, message.getRegenerateThreshold());
            assertTrue(message.shouldRegenerate(0.5));
        }
    }
    
    @Nested
    @DisplayName("🎮 Interactive Element Message Tests")
    class InteractiveElementMessageTests {
        
        @Test
        @DisplayName("🎯 Should create drag-and-drop sorting exercise")
        void shouldCreateDragAndDropSort() {
            // Given
            String instructions = "Sort these communication styles by effectiveness";
            
            // When
            InteractiveElementMessage message = InteractiveElementMessage.createDragAndDropSort(instructions);
            
            // Then
            assertNotNull(message);
            assertEquals(instructions, message.getInstructions());
            assertTrue(message.isDragAndDrop());
            assertEquals(120, message.getTimeLimitSeconds());
            assertTrue(message.hasTimeLimit());
            assertTrue(message.hasHintsEnabled());
            assertTrue(message.isMobileOptimized());
        }
        
        @Test
        @DisplayName("🎚️ Should create priority slider")
        void shouldCreatePrioritySlider() {
            // Given
            String instructions = "Rank these factors by importance";
            
            // When
            InteractiveElementMessage message = InteractiveElementMessage.createPrioritySlider(instructions);
            
            // Then
            assertNotNull(message);
            assertEquals(instructions, message.getInstructions());
            assertTrue(message.isSliderInput());
            assertEquals(90, message.getTimeLimitSeconds());
            assertEquals("EASY", message.getDifficultyLevel());
        }
        
        @Test
        @DisplayName("🎨 Should create drawing canvas")
        void shouldCreateDrawingCanvas() {
            // Given
            String instructions = "Draw a diagram showing the communication flow";
            
            // When
            InteractiveElementMessage message = InteractiveElementMessage.createDrawingCanvas(instructions);
            
            // Then
            assertNotNull(message);
            assertEquals(instructions, message.getInstructions());
            assertTrue(message.isDrawingCanvas());
            assertEquals(300, message.getTimeLimitSeconds());
            assertEquals(1, message.getMaxAttempts());
            assertFalse(message.hasHintsEnabled());
            assertFalse(message.isMobileOptimized());
        }
        
        @Test
        @DisplayName("🎤 Should create voice recording")
        void shouldCreateVoiceRecording() {
            // Given
            String instructions = "Record your elevator pitch";
            
            // When
            InteractiveElementMessage message = InteractiveElementMessage.createVoiceRecording(instructions);
            
            // Then
            assertNotNull(message);
            assertEquals(instructions, message.getInstructions());
            assertTrue(message.isVoiceRecording());
            assertEquals(60, message.getTimeLimitSeconds());
            assertTrue(message.getAutoSubmit());
            assertTrue(message.isAccessibilityEnabled());
        }
        
        @Test
        @DisplayName("🎮 Should create matching game with difficulty")
        void shouldCreateMatchingGame() {
            // Given
            String instructions = "Match communication techniques with scenarios";
            String difficulty = "HARD";
            
            // When
            InteractiveElementMessage message = InteractiveElementMessage.createMatchingGame(instructions, difficulty);
            
            // Then
            assertNotNull(message);
            assertEquals(instructions, message.getInstructions());
            assertEquals(difficulty, message.getDifficultyLevel());
            assertTrue(message.isDragAndDrop());
            assertEquals(90, message.getTimeLimitSeconds()); // Hard = 90 seconds
            assertEquals(2, message.getMaxAttempts()); // Hard = 2 attempts
        }
    }
    
    @Nested
    @DisplayName("📸 Enhanced Media Message Tests")
    class EnhancedMediaMessageTests {
        
        @Test
        @DisplayName("📸 Should create image message")
        void shouldCreateImageMessage() {
            // Given
            String mediaUrl = "https://example.com/image.jpg";
            String altText = "Communication diagram";
            String caption = "Effective communication patterns";
            
            // When
            EnhancedMediaMessage message = EnhancedMediaMessage.createImage(mediaUrl, altText, caption);
            
            // Then
            assertNotNull(message);
            assertTrue(message.isImage());
            assertEquals(mediaUrl, message.getMediaUrl());
            assertEquals(altText, message.getAltText());
            assertEquals(caption, message.getCaption());
            assertTrue(message.isLazyLoadingEnabled());
            assertTrue(message.isZoomEnabled());
            assertFalse(message.getDownloadAllowed());
        }
        
        @Test
        @DisplayName("📹 Should create video message")
        void shouldCreateVideoMessage() {
            // Given
            String mediaUrl = "https://example.com/video.mp4";
            String thumbnailUrl = "https://example.com/thumb.jpg";
            String caption = "Training video on active listening";
            
            // When
            EnhancedMediaMessage message = EnhancedMediaMessage.createVideo(mediaUrl, thumbnailUrl, caption);
            
            // Then
            assertNotNull(message);
            assertTrue(message.isVideo());
            assertEquals(mediaUrl, message.getMediaUrl());
            assertEquals(thumbnailUrl, message.getThumbnailUrl());
            assertEquals(caption, message.getCaption());
            assertFalse(message.isAutoPlayEnabled());
            assertTrue(message.getControlsVisible());
            assertEquals("HLS", message.getStreamingProtocol());
        }
        
        @Test
        @DisplayName("🔊 Should create audio message")
        void shouldCreateAudioMessage() {
            // Given
            String mediaUrl = "https://example.com/audio.mp3";
            String caption = "Pronunciation guide";
            
            // When
            EnhancedMediaMessage message = EnhancedMediaMessage.createAudio(mediaUrl, caption);
            
            // Then
            assertNotNull(message);
            assertTrue(message.isAudio());
            assertEquals(mediaUrl, message.getMediaUrl());
            assertEquals(caption, message.getCaption());
            assertFalse(message.isAutoPlayEnabled());
            assertTrue(message.getControlsVisible());
            assertFalse(message.getDownloadAllowed());
        }
        
        @Test
        @DisplayName("📸 Should create interactive image")
        void shouldCreateInteractiveImage() {
            // Given
            String mediaUrl = "https://example.com/interactive.jpg";
            String altText = "Interactive communication diagram";
            String caption = "Click on areas to learn more";
            
            // When
            EnhancedMediaMessage message = EnhancedMediaMessage.createInteractiveImage(mediaUrl, altText, caption);
            
            // Then
            assertNotNull(message);
            assertTrue(message.isImage());
            assertEquals(mediaUrl, message.getMediaUrl());
            assertEquals(altText, message.getAltText());
            assertEquals(caption, message.getCaption());
            assertTrue(message.isAnnotationEnabled());
            assertTrue(message.isZoomEnabled());
        }
        
        @Test
        @DisplayName("🎠 Should create media carousel")
        void shouldCreateCarousel() {
            // Given
            String caption = "Multiple training scenarios";
            
            // When
            EnhancedMediaMessage message = EnhancedMediaMessage.createCarousel(caption);
            
            // Then
            assertNotNull(message);
            assertTrue(message.isCarousel());
            assertEquals(caption, message.getCaption());
            assertTrue(message.isLazyLoadingEnabled());
            assertTrue(message.isZoomEnabled());
        }
        
        @Test
        @DisplayName("📄 Should create document viewer")
        void shouldCreateDocument() {
            // Given
            String mediaUrl = "https://example.com/guide.pdf";
            String caption = "Communication best practices guide";
            
            // When
            EnhancedMediaMessage message = EnhancedMediaMessage.createDocument(mediaUrl, caption);
            
            // Then
            assertNotNull(message);
            assertTrue(message.isDocument());
            assertEquals(mediaUrl, message.getMediaUrl());
            assertEquals(caption, message.getCaption());
            assertTrue(message.isAnnotationEnabled());
            assertTrue(message.getDownloadAllowed());
        }
        
        @Test
        @DisplayName("📐 Should calculate aspect ratio correctly")
        void shouldCalculateAspectRatio() {
            // Given
            EnhancedMediaMessage message = EnhancedMediaMessage.builder()
                .widthPixels(1920)
                .heightPixels(1080)
                .build();
            
            // When
            double aspectRatio = message.getAspectRatio();
            
            // Then
            assertEquals(16.0 / 9.0, aspectRatio, 0.001);
        }
        
        @Test
        @DisplayName("📊 Should calculate file size in MB")
        void shouldCalculateFileSizeMB() {
            // Given
            long fileSizeBytes = 5 * 1024 * 1024; // 5 MB
            EnhancedMediaMessage message = EnhancedMediaMessage.builder()
                .fileSizeBytes(fileSizeBytes)
                .build();
            
            // When
            double fileSizeMB = message.getFileSizeMB();
            
            // Then
            assertEquals(5.0, fileSizeMB, 0.001);
        }
    }
    
    @Nested
    @DisplayName("🔧 Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("🌊 Should work together in complete scenario")
        void shouldWorkTogetherInCompleteScenario() {
            // Given - Create a complete learning scenario with all message types
            
            // Rich text introduction
            RichTextMessage intro = RichTextMessage.createMarkdown(
                "# Welcome to Communication Training\n\nLet's start with **{{user_name}}**!"
            );
            
            // AI-enhanced personalized content
            AiEnhancedTextMessage aiContent = AiEnhancedTextMessage.createPersonalized(
                "Based on your role as {{role}}, let's focus on {{learning_objective}}",
                "active listening"
            );
            
            // Interactive choice question
            EnhancedChoiceQuestion question = EnhancedChoiceQuestion.createTimed(
                "What's the most important aspect of active listening?", 30
            );
            
            // Interactive exercise
            InteractiveElementMessage exercise = InteractiveElementMessage.createDragAndDropSort(
                "Sort these listening techniques by effectiveness"
            );
            
            // Media content
            EnhancedMediaMessage video = EnhancedMediaMessage.createInteractiveVideo(
                "https://example.com/training.mp4",
                "https://example.com/thumb.jpg",
                "Watch this scenario and identify key moments"
            );
            
            // Then - All messages should be properly configured
            assertNotNull(intro);
            assertTrue(intro.isRichFormatted());
            assertTrue(intro.usesVariableSubstitution());
            
            assertNotNull(aiContent);
            assertTrue(aiContent.isAiGenerationEnabled());
            assertTrue(aiContent.hasLearningObjective());
            
            assertNotNull(question);
            assertTrue(question.hasTimeLimit());
            assertTrue(question.hasHintsEnabled());
            
            assertNotNull(exercise);
            assertTrue(exercise.isDragAndDrop());
            assertTrue(exercise.hasTimeLimit());
            
            assertNotNull(video);
            assertTrue(video.isVideo());
            assertTrue(video.isAnnotationEnabled());
        }
    }
} 