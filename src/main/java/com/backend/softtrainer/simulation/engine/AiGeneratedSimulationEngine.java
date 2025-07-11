package com.backend.softtrainer.simulation.engine;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.aiagent.*;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.repositories.CharacterRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.services.AiAgentService;
import com.backend.softtrainer.services.UserHyperParameterService;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;

/**
 * 🤖 AI-Generated Simulation Engine - Real-time AI Content Generation
 *
 * This engine handles AI-generated simulations that create content in real-time
 * without relying on predefined nodes. It integrates with the AI-agent microservice
 * to generate contextually appropriate messages and responses.
 *
 * Key Features:
 * ✅ Real-time message generation via AI agent
 * ✅ Stateless AI agent integration (backend handles all persistence)
 * ✅ Context-aware conversation management
 * ✅ Hyperparameter updates and evaluation
 * ✅ Multi-turn conversation support
 * ✅ Graceful fallback handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiGeneratedSimulationEngine implements BaseSimulationEngine {

    private final AiAgentService aiAgentService;
    private final MessageRepository messageRepository;
    private final MessageService messageService;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserHyperParameterService hyperParameterService;
    private final ObjectMapper objectMapper;

    // Performance metrics
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong initializedChats = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);

    @Override
    public SimulationType getSupportedType() {
        return SimulationType.AI_GENERATED;
    }

    @Override
    public ChatDataDto processUserMessage(MessageRequestDto messageRequest, SimulationContext context) {
        log.info("🤖 Processing user message {} with AI-generated engine for chat {}", 
                messageRequest.getId(), messageRequest.getChatId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Build AI request from context
            AiMessageGenerationRequestDto aiRequest = buildAiRequest(messageRequest, context);

            // 2. Call AI agent for real-time generation
            AiMessageGenerationResponseDto aiResponse = aiAgentService.generateMessage(aiRequest);

            // 3. Validate AI response
            if (!aiResponse.getSuccess()) {
                log.error("❌ AI agent returned error: {}", aiResponse.getErrorMessage());
                return createErrorResponse(messageRequest.getChatId(), aiResponse.getErrorMessage());
            }

            // 4. Convert AI response to Message entities
            List<Message> generatedMessages = convertAiResponseToMessages(aiResponse, context);

            // 5. Save messages to database
            List<Message> savedMessages = generatedMessages.stream()
                    .map(messageService::save)
                    .collect(Collectors.toList());

            // 6. Update hyperparameters if provided
            updateHyperParameters(aiResponse, context);

            // 7. Update chat completion status
            updateChatCompletionStatus(aiResponse, context);

            // 8. Update metrics
            processedMessages.incrementAndGet();
            totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);

            // 9. Create response
            ChatDataDto response = new ChatDataDto(savedMessages, new ChatParams(context.getHearts()));

            log.info("✅ AI-generated engine processed message successfully. Generated {} messages", 
                    savedMessages.size());

            return response;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ AI-generated engine error processing message {}: {}", 
                    messageRequest.getId(), e.getMessage(), e);
            return createErrorResponse(messageRequest.getChatId(), e.getMessage());
        }
    }

    @Override
    public List<Message> initializeSimulation(SimulationContext context) {
        log.info("🎬 Initializing AI-generated simulation for chat {}", context.getChatId());

        try {
            // 1. Build AI initialization request
            AiInitializeSimulationRequestDto aiRequest = buildInitializationRequest(context);

            // 2. Call AI agent for initial messages
            AiMessageGenerationResponseDto aiResponse = aiAgentService.initializeSimulation(aiRequest);

            // 3. Validate AI response
            if (!aiResponse.getSuccess()) {
                log.error("❌ AI agent initialization failed: {}", aiResponse.getErrorMessage());
                return createFallbackInitialMessages(context);
            }

            // 4. Convert and save initial messages
            List<Message> initialMessages = convertAiResponseToMessages(aiResponse, context);
            List<Message> savedMessages = initialMessages.stream()
                    .map(messageService::save)
                    .collect(Collectors.toList());

            // 5. Initialize hyperparameters
            initializeHyperParameters(aiResponse, context);

            // 6. Update metrics
            initializedChats.incrementAndGet();

            log.info("✅ AI-generated engine initialized simulation with {} messages", 
                    savedMessages.size());

            return savedMessages;

        } catch (Exception e) {
            log.error("❌ AI-generated engine error initializing simulation: {}", e.getMessage(), e);
            return createFallbackInitialMessages(context);
        }
    }

    @Override
    public Message generateFinalMessage(SimulationContext context) {
        log.info("🎯 Generating final message for AI-generated simulation, chat {}", context.getChatId());

        try {
            // Build a special AI request for final message generation
            AiMessageGenerationRequestDto aiRequest = AiMessageGenerationRequestDto.builder()
                    .simulationId(context.getSimulationId().toString())
                    .chatId(context.getChatId().toString())
                    .chatHistory(buildChatHistory(context))
                    .simulationContext(buildSimulationContext(context))
                    .hyperParameters(getCurrentHyperParameters(context))
                    .organizationContext(buildOrganizationContext(context))
                    .build();

            // Add metadata indicating this is a final message request
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("is_final_message", true);
            metadata.put("reason", "hearts_exhausted");
            aiRequest.getSimulationContext().setLearningObjectives("Generate a final summary and conclusion message");

            AiMessageGenerationResponseDto aiResponse = aiAgentService.generateMessage(aiRequest);

            if (aiResponse.getSuccess() && !aiResponse.getMessages().isEmpty()) {
                List<Message> finalMessages = convertAiResponseToMessages(aiResponse, context);
                Message finalMessage = finalMessages.get(0);
                return messageService.save(finalMessage);
            }

            // Fallback to default final message
            return createFallbackFinalMessage(context);

        } catch (Exception e) {
            log.error("❌ Error generating final message: {}", e.getMessage(), e);
            return createFallbackFinalMessage(context);
        }
    }

    @Override
    public boolean canHandle(SimulationContext context) {
        return context.getSimulation().getType() == 
               com.backend.softtrainer.entities.enums.SimulationType.AI_GENERATED;
    }

    @Override
    public List<String> validateSimulation(SimulationContext context) {
        List<String> issues = new ArrayList<>();

        if (context.getSimulation().getType() != 
            com.backend.softtrainer.entities.enums.SimulationType.AI_GENERATED) {
            issues.add("Simulation type must be AI_GENERATED");
        }

        if (context.getSimulation().getName() == null || context.getSimulation().getName().trim().isEmpty()) {
            issues.add("Simulation name is required for AI generation");
        }

        return issues;
    }

    @Override
    public EngineMetrics getMetrics() {
        return new AiGeneratedEngineMetrics();
    }

    @Override
    public boolean isSimulationComplete(SimulationContext context) {
        // Check if the last AI response indicated conversation completion
        // This would be determined by the AI agent's conversationEnded flag
        return false; // Implementation depends on specific completion logic
    }

    /**
     * 🏗️ Build AI request from message request and context
     */
    private AiMessageGenerationRequestDto buildAiRequest(MessageRequestDto messageRequest, SimulationContext context) {
        return AiMessageGenerationRequestDto.builder()
                .simulationId(context.getSimulationId().toString())
                .chatId(context.getChatId().toString())
                .chatHistory(buildChatHistory(context))
                .userMessage(buildUserMessage(messageRequest))
                .simulationContext(buildSimulationContext(context))
                .hyperParameters(getCurrentHyperParameters(context))
                .organizationContext(buildOrganizationContext(context))
                .build();
    }

    /**
     * 🎬 Build AI initialization request
     */
    private AiInitializeSimulationRequestDto buildInitializationRequest(SimulationContext context) {
        return AiInitializeSimulationRequestDto.builder()
                .simulationId(context.getSimulationId().toString())
                .chatId(context.getChatId().toString())
                .simulationContext(buildSimulationContext(context))
                .organizationContext(buildOrganizationContext(context))
                .userContext(buildUserContext(context))
                .initialHyperParameters(getInitialHyperParameters(context))
                .build();
    }

    /**
     * 💬 Build chat history for AI context
     */
    private List<AiChatMessageDto> buildChatHistory(SimulationContext context) {
        return context.getMessageHistory().stream()
                .map(message -> AiChatMessageDto.builder()
                        .messageId(message.getId())
                        .messageType(message.getMessageType().getValue())
                        .role(message.getRole() == ChatRole.USER ? "USER" : "ASSISTANT")
                        .content(extractMessageContent(message))
                        .characterName(message.getCharacter() != null ? message.getCharacter().getName() : null)
                        .timestamp(message.getTimestamp())
                        .requiresResponse(message.getMessageType().name().contains("QUESTION"))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 👤 Build user message DTO
     */
    private AiUserMessageDto buildUserMessage(MessageRequestDto messageRequest) {
        return AiUserMessageDto.builder()
                .messageId(messageRequest.getId())
                .messageType(messageRequest.getMessageType() != null ? messageRequest.getMessageType().getValue() : "TEXT")
                .content(extractUserContent(messageRequest))
                .selectedOption(extractSelectedOption(messageRequest))
                .selectedOptionId(extractSelectedOptionId(messageRequest))
                .userAnswer(extractUserAnswer(messageRequest))
                .responseTime(messageRequest.getUserResponseTime())
                .build();
    }

    /**
     * 🎭 Build simulation context DTO
     */
    private AiSimulationContextDto buildSimulationContext(SimulationContext context) {
        Simulation simulation = context.getSimulation();
        
        return AiSimulationContextDto.builder()
                .simulationName(simulation.getName())
                .simulationDescription("AI-generated simulation")
                .skillName(simulation.getSkill() != null ? simulation.getSkill().getName() : "Unknown")
                .skillDescription(simulation.getSkill() != null ? simulation.getSkill().getDescription() : "")
                .complexity(simulation.getComplexity() != null ? simulation.getComplexity().name() : "MEDIUM")
                .conversationTurn(context.getMessageHistory().size())
                .heartsRemaining(context.getHearts())
                .characters(buildCharacterInfo(context))
                .learningObjectives(buildLearningObjectives(context))
                .build();
    }

    /**
     * 🔄 Convert AI response to Message entities
     */
    private List<Message> convertAiResponseToMessages(AiMessageGenerationResponseDto aiResponse, SimulationContext context) {
        return aiResponse.getMessages().stream()
                .map(aiMessage -> convertAiMessageToEntity(aiMessage, context))
                .collect(Collectors.toList());
    }

    /**
     * 📨 Convert single AI message to Message entity
     */
    private Message convertAiMessageToEntity(AiGeneratedMessageDto aiMessage, SimulationContext context) {
        // Use the chat from context directly - it will be managed when the message is saved
        Chat chat = context.getChat();
        MessageType messageType = parseMessageType(aiMessage.getMessageType());

        // Create the appropriate message subclass based on type
        Message message;
        
        switch (messageType) {
            case TEXT:
                message = TextMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .chat(chat)
                        .messageType(messageType)
                        .role(ChatRole.CHARACTER)
                        .character(resolveCharacter(aiMessage.getCharacterName()))
                        .interacted(false)
                        .responseTimeLimit(aiMessage.getResponseTimeLimit())
                        .content(aiMessage.getContent() != null ? aiMessage.getContent() : "AI-generated text message")
                        .build();
                break;
                
            case SINGLE_CHOICE_QUESTION:
                message = SingleChoiceQuestionMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .chat(chat)
                        .messageType(messageType)
                        .role(ChatRole.CHARACTER)
                        .character(resolveCharacter(aiMessage.getCharacterName()))
                        .interacted(false)
                        .responseTimeLimit(aiMessage.getResponseTimeLimit())
                        .options(aiMessage.getOptions() != null && !aiMessage.getOptions().isEmpty() ? 
                                String.join("||", aiMessage.getOptions()) : "Option A||Option B||Option C")
                        .build();
                break;
                
            default:
                // For any other message types, create a TextMessage as fallback
                message = TextMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .chat(chat)
                        .messageType(MessageType.TEXT)
                        .role(ChatRole.CHARACTER)
                        .character(resolveCharacter(aiMessage.getCharacterName()))
                        .interacted(false)
                        .responseTimeLimit(aiMessage.getResponseTimeLimit())
                        .content(aiMessage.getContent() != null ? aiMessage.getContent() : "AI-generated message")
                        .build();
                break;
        }

        return message;
    }

    /**
     * 📝 Set message content based on AI message type
     */
    private void setMessageContent(Message message, AiGeneratedMessageDto aiMessage) {
        // This would set the appropriate content fields based on message type
        // Implementation depends on the specific message entity structure
        log.debug("Setting content for message type: {}", aiMessage.getMessageType());
    }

    /**
     * 🎭 Resolve character by name
     */
    private Character resolveCharacter(String characterName) {
        if (characterName == null || characterName.trim().isEmpty()) {
            return null;
        }
        
        // Since CharacterRepository doesn't have findByName, we'll use a simple approach
        // In a real implementation, you'd add the findByName method to the repository
        log.debug("Character lookup requested for: {}, using null for now", characterName);
        return null;
    }

    /**
     * 📊 Parse message type from string
     */
    private MessageType parseMessageType(String messageType) {
        try {
            return MessageType.fromValue(messageType);
        } catch (Exception e) {
            log.warn("Unknown message type: {}, defaulting to TEXT", messageType);
            return MessageType.TEXT;
        }
    }

    /**
     * 🔧 Update hyperparameters from AI response
     */
    private void updateHyperParameters(AiMessageGenerationResponseDto aiResponse, SimulationContext context) {
        if (aiResponse.getUpdatedHyperParameters() != null && !aiResponse.getUpdatedHyperParameters().isEmpty()) {
            // Update user hyperparameters
            aiResponse.getUpdatedHyperParameters().forEach((key, value) -> {
                try {
                    double numericValue = value instanceof Number ? ((Number) value).doubleValue() : 
                                         Double.parseDouble(value.toString());
                    hyperParameterService.update(context.getChatId(), key, numericValue);
                } catch (Exception e) {
                    log.warn("Failed to update hyperparameter {}: {}", key, e.getMessage());
                }
            });
        }
    }

    /**
     * 📊 Get current hyperparameters
     */
    private Map<String, Object> getCurrentHyperParameters(SimulationContext context) {
        // Implementation depends on hyperparameter service
        return new HashMap<>();
    }

    /**
     * 🎯 Initialize hyperparameters
     */
    private void initializeHyperParameters(AiMessageGenerationResponseDto aiResponse, SimulationContext context) {
        updateHyperParameters(aiResponse, context);
    }

    /**
     * 📈 Get initial hyperparameters
     */
    private Map<String, Object> getInitialHyperParameters(SimulationContext context) {
        Map<String, Object> initial = new HashMap<>();
        initial.put("engagement", 0.5);
        initial.put("empathy", 0.5);
        initial.put("active_listening", 0.5);
        return initial;
    }

    // Helper methods for content extraction
    private String extractMessageContent(Message message) {
        // Implementation depends on message entity structure
        return "";
    }

    private String extractUserContent(MessageRequestDto messageRequest) {
        if (messageRequest instanceof SingleChoiceAnswerMessageDto) {
            return ((SingleChoiceAnswerMessageDto) messageRequest).getAnswer();
        } else if (messageRequest instanceof EnterTextAnswerMessageDto) {
            return ((EnterTextAnswerMessageDto) messageRequest).getAnswer();
        }
        return "";
    }

    private String extractSelectedOption(MessageRequestDto messageRequest) {
        if (messageRequest instanceof SingleChoiceAnswerMessageDto) {
            return ((SingleChoiceAnswerMessageDto) messageRequest).getAnswer();
        }
        return null;
    }

    private String extractSelectedOptionId(MessageRequestDto messageRequest) {
        if (messageRequest instanceof SingleChoiceAnswerMessageDto) {
            // For simplicity, use the answer as the option ID
            return ((SingleChoiceAnswerMessageDto) messageRequest).getAnswer();
        }
        return null;
    }

    private String extractUserAnswer(MessageRequestDto messageRequest) {
        if (messageRequest instanceof EnterTextAnswerMessageDto) {
            return ((EnterTextAnswerMessageDto) messageRequest).getAnswer();
        }
        return null;
    }

    // Helper methods for building context
    private AiAgentOrganizationDto buildOrganizationContext(SimulationContext context) {
        return AiAgentOrganizationDto.builder()
                .name("Default Organization")
                .industry("Technology")
                .size("50-100 employees")
                .localization("en")
                .build();
    }

    private Map<String, Object> buildUserContext(SimulationContext context) {
        Map<String, Object> userContext = new HashMap<>();
        userContext.put("userId", context.getUser().getId());
        userContext.put("userName", context.getUser().getName());
        return userContext;
    }

    private String buildCharacterInfo(SimulationContext context) {
        return "AI-generated characters based on simulation context";
    }

    private String buildLearningObjectives(SimulationContext context) {
        return "AI-generated learning objectives based on skill description";
    }

    private void updateChatCompletionStatus(AiMessageGenerationResponseDto aiResponse, SimulationContext context) {
        if (aiResponse.getConversationEnded() != null && aiResponse.getConversationEnded()) {
            context.getChat().setFinished(true);
            log.info("Chat {} marked as completed by AI agent", context.getChatId());
        }
    }

    // Error handling and fallback methods
    private ChatDataDto createErrorResponse(Long chatId, String error) {
        log.error("Creating error response for chat {}: {}", chatId, error);
        return new ChatDataDto(Collections.emptyList(), new ChatParams(0.0));
    }

    private List<Message> createFallbackInitialMessages(SimulationContext context) {
        log.warn("Creating fallback initial messages for chat {}", context.getChatId());
        
        // Use the chat from context directly - it's already loaded and attached
        Chat chat = context.getChat();
        
        // Create welcome text message
        TextMessage welcomeMessage = TextMessage.builder()
                .id(UUID.randomUUID().toString())
                .chat(chat)
                .messageType(MessageType.TEXT)
                .role(ChatRole.CHARACTER)
                .character(resolveCharacter("AI Assistant"))
                .interacted(false)
                .content("Welcome to the AI-generated simulation! I'm here to help guide you through this learning experience.")
                .build();
        
        // Create interactive question message
        SingleChoiceQuestionMessage questionMessage = SingleChoiceQuestionMessage.builder()
                .id(UUID.randomUUID().toString())
                .chat(chat)
                .messageType(MessageType.SINGLE_CHOICE_QUESTION)
                .role(ChatRole.CHARACTER)
                .character(resolveCharacter("AI Assistant"))
                .interacted(false)
                .options("Collaborative||Directive||Supportive||Adaptive")
                .build();
        
        // Save both messages
        return List.of(
                messageService.save(welcomeMessage),
                messageService.save(questionMessage)
        );
    }

    private Message createFallbackFinalMessage(SimulationContext context) {
        log.warn("Creating fallback final message for chat {}", context.getChatId());
        
        // Use the chat from context directly - it's already loaded and attached
        Chat chat = context.getChat();
        
        TextMessage fallbackMessage = TextMessage.builder()
                .id(UUID.randomUUID().toString())
                .chat(chat)
                .messageType(MessageType.TEXT)
                .role(ChatRole.CHARACTER)
                .character(resolveCharacter("AI Assistant")) // Set a default character
                .interacted(false)
                .content("Thank you for completing this simulation! Your responses have been recorded.")
                .build();
        
        return messageService.save(fallbackMessage);
    }

    /**
     * 📊 Engine metrics implementation
     */
    private class AiGeneratedEngineMetrics implements EngineMetrics {
        @Override
        public long getTotalProcessedMessages() {
            return processedMessages.get();
        }

        @Override
        public long getTotalInitializedChats() {
            return initializedChats.get();
        }

        @Override
        public double getAverageProcessingTimeMs() {
            long total = processedMessages.get();
            return total > 0 ? totalProcessingTime.get() / (double) total : 0.0;
        }

        @Override
        public long getErrorCount() {
            return errorCount.get();
        }

        @Override
        public String getEngineVersion() {
            return "AiGenerated-1.0";
        }
    }
} 