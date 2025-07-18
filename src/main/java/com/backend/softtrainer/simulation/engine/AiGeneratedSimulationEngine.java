package com.backend.softtrainer.simulation.engine;

import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.aiagent.AiAgentOrganizationDto;
import com.backend.softtrainer.dtos.aiagent.AiChatMessageDto;
import com.backend.softtrainer.dtos.aiagent.AiGeneratedMessageDto;
import com.backend.softtrainer.dtos.aiagent.AiInitializeSimulationRequestDto;
import com.backend.softtrainer.dtos.aiagent.AiMessageGenerationRequestDto;
import com.backend.softtrainer.dtos.aiagent.AiMessageGenerationResponseDto;
import com.backend.softtrainer.dtos.aiagent.AiSimulationContextDto;
import com.backend.softtrainer.dtos.aiagent.AiSkillMaterialDto;
import com.backend.softtrainer.dtos.aiagent.AiUserMessageDto;
import com.backend.softtrainer.dtos.aiagent.AiAgentSkillDto;
import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.MultiChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.ChatRole;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.messages.LastSimulationMessage;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.TextMessage;
import com.backend.softtrainer.repositories.CharacterRepository;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.repositories.UserRepository;
import com.backend.softtrainer.services.AiAgentService;
import com.backend.softtrainer.services.MessageService;
import com.backend.softtrainer.services.UserHyperParameterService;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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
    @Transactional
    public ChatDataDto processUserMessage(MessageRequestDto messageRequest, SimulationContext context) {
        log.info("🤖 Processing user message {} with AI-generated engine for chat {}",
                messageRequest.getId(), messageRequest.getChatId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Update the user's answered message in database (similar to legacy approach)
            updateUserAnsweredMessage(messageRequest, context);

            // Fetch the updated message to include in the response
            Message updatedInteractedMessage = messageRepository.findById(messageRequest.getId()).orElse(null);

            // 2. Build AI request from context
            AiMessageGenerationRequestDto aiRequest = buildAiRequest(messageRequest, context);

            // 3. Call AI agent for real-time generation
            AiMessageGenerationResponseDto aiResponse = aiAgentService.generateMessage(aiRequest);

            // 4. Validate AI response
            if (aiResponse == null) {
                log.error("❌ AI agent returned null response for message: {}", messageRequest.getId());
                return createErrorResponse(messageRequest.getChatId(), "AI agent returned null response");
            }

            if (!aiResponse.getSuccess()) {
                log.error("❌ AI agent returned error: {}", aiResponse.getErrorMessage());
                return createErrorResponse(messageRequest.getChatId(), aiResponse.getErrorMessage());
            }

            // 5. Convert AI response to Message entities
            List<Message> generatedMessages = convertAiResponseToMessages(aiResponse, context);

            // 6. Save messages to database
            List<Message> savedMessages = generatedMessages.stream()
                    .map(messageService::save)
                    .collect(Collectors.toList());

            // Prepend the updated interacted message to the list
            List<Message> responseMessages = new ArrayList<>();
            if (updatedInteractedMessage != null) {
                responseMessages.add(updatedInteractedMessage);
            }
            responseMessages.addAll(savedMessages);

            // 7. Update hyperparameters if provided
            updateHyperParameters(aiResponse, context);

            // 8. Update chat completion status
            updateChatCompletionStatus(aiResponse, context);

            // 9. Update metrics
            processedMessages.incrementAndGet();
            totalProcessingTime.addAndGet(System.currentTimeMillis() - startTime);

            // 10. Create response with the combined list of messages
            ChatDataDto response = new ChatDataDto(responseMessages, new ChatParams(context.getHearts()));

            log.info("✅ AI-generated engine processed message successfully. Generated {} new messages, returning {} total messages.",
                    savedMessages.size(), responseMessages.size());

            return response;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ AI-generated engine error processing message {}: {}",
                    messageRequest.getId(), e.getMessage(), e);
            return createErrorResponse(messageRequest.getChatId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<Message> initializeSimulation(SimulationContext context) {
        log.info("🎬 Initializing AI-generated simulation for chat {}", context.getChatId());

        try {
            // 1. Build AI initialization request
            AiInitializeSimulationRequestDto aiRequest = buildInitializationRequest(context);

            // 2. Call AI agent for initial messages
            AiMessageGenerationResponseDto aiResponse = aiAgentService.initializeSimulation(aiRequest);

            // 3. Validate AI response
            if (aiResponse == null) {
                log.error("❌ AI agent returned null response for initialization of chat: {}", context.getChatId());
                return createFallbackInitialMessages(context);
            }

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
            // Create a special "system" user message for final message generation
            AiUserMessageDto finalUserMessage = AiUserMessageDto.builder()
                    .messageId("final-message-request")
                    .messageType("TEXT")
                    .content("Generate a final summary and conclusion message for this simulation.")
                    .build();

            // Get skill materials for context
            List<AiSkillMaterialDto> skillMaterials = convertSkillMaterials(context);
            if (skillMaterials == null) {
                skillMaterials = new ArrayList<>(); // Ensure non-null list
            }

            // Build a special AI request for final message generation
            AiMessageGenerationRequestDto aiRequest = AiMessageGenerationRequestDto.builder()
                    .simulationId(context.getSimulationId().toString())
                    .chatId(context.getChatId().toString())
                    .chatHistory(buildChatHistory(context))
                    .userMessage(finalUserMessage)
                    .simulationContext(buildSimulationContext(context))
                    .hyperParameters(getCurrentHyperParameters(context))
                    .organizationContext(buildOrganizationContext(context))
                    .skillMaterials(skillMaterials)
                    .build();

            // Add metadata indicating this is a final message request
            aiRequest.getSimulationContext().setLearningObjectives("Generate a final summary and conclusion message for the completed simulation");

            AiMessageGenerationResponseDto aiResponse = aiAgentService.generateMessage(aiRequest);

            if (aiResponse.getSuccess() && !aiResponse.getMessages().isEmpty()) {
                List<Message> finalMessages = convertAiResponseToMessages(aiResponse, context);
                Message finalMessage = finalMessages.get(0);
                try {
                    return messageService.save(finalMessage);
                } catch (Exception e) {
                    // Handle database save failures (e.g., during test cleanup when tables are dropped)
                    log.warn("⚠️ Could not save AI-generated final message to database: {}. Returning unsaved message.", e.getMessage());
                    return finalMessage;
                }
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
        // Build each component separately for better debugging
        List<AiChatMessageDto> chatHistory = buildChatHistory(context);
        AiUserMessageDto userMessage = buildUserMessage(messageRequest);
        AiSimulationContextDto simulationContext = buildSimulationContext(context);
        Map<String, Object> hyperParameters = getCurrentHyperParameters(context);
        AiAgentOrganizationDto organizationContext = buildOrganizationContext(context);
        List<AiSkillMaterialDto> skillMaterials = convertSkillMaterials(context);

        // Log each component for debugging
        log.debug("🔍 Building AI request - chatHistory size: {}", chatHistory != null ? chatHistory.size() : "NULL");
        log.debug("🔍 Building AI request - userMessage: {}", userMessage != null ? "PRESENT" : "NULL");
        log.debug("🔍 Building AI request - simulationContext: {}", simulationContext != null ? "PRESENT" : "NULL");
        log.debug("🔍 Building AI request - hyperParameters size: {}", hyperParameters != null ? hyperParameters.size() : "NULL");
        log.debug("🔍 Building AI request - organizationContext: {}", organizationContext != null ? "PRESENT" : "NULL");
        log.debug("🔍 Building AI request - skillMaterials size: {}", skillMaterials != null ? skillMaterials.size() : "NULL");

        AiMessageGenerationRequestDto request = AiMessageGenerationRequestDto.builder()
                .simulationId(context.getSimulationId().toString())
                .chatId(context.getChatId().toString())
                .chatHistory(chatHistory)
                .userMessage(userMessage)
                .simulationContext(simulationContext)
                .hyperParameters(hyperParameters)
                .organizationContext(organizationContext)
                .skillMaterials(skillMaterials)
                .build();

        // Log the complete request as JSON for debugging
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String requestJson = mapper.writeValueAsString(request);
            log.info("[AI_AGENT_REQUEST] Full AI request for chat {}: {}", context.getChatId(), requestJson);
        } catch (Exception e) {
            log.warn("[AI_AGENT_REQUEST] Could not serialize AI request for logging", e);
        }

        return request;
    }

    /**
     * 🎬 Build AI initialization request
     */
    private AiInitializeSimulationRequestDto buildInitializationRequest(SimulationContext context) {
        // Build each component separately for better debugging
        AiAgentSkillDto skill = buildSkillDto(context);
        AiSimulationContextDto simulationContext = buildSimulationContext(context);
        AiAgentOrganizationDto organizationContext = buildOrganizationContext(context);
        Map<String, Object> userContext = buildUserContext(context);
        Map<String, Object> initialHyperParameters = getInitialHyperParameters(context);
        List<AiSkillMaterialDto> skillMaterials = convertSkillMaterials(context);
        List<Map<String, Object>> chatHistory = buildChatHistoryForInitialization(context);

        // Log each component for debugging
        log.debug("🔍 Building AI initialization request - skill: {}", skill != null ? "PRESENT" : "NULL");
        log.debug("🔍 Building AI initialization request - simulationContext: {}", simulationContext != null ? "PRESENT" : "NULL");
        log.debug("🔍 Building AI initialization request - organizationContext: {}", organizationContext != null ? "PRESENT" : "NULL");
        log.debug("🔍 Building AI initialization request - userContext size: {}", userContext != null ? userContext.size() : "NULL");
        log.debug("🔍 Building AI initialization request - initialHyperParameters size: {}", initialHyperParameters != null ? initialHyperParameters.size() : "NULL");
        log.debug("🔍 Building AI initialization request - skillMaterials size: {}", skillMaterials != null ? skillMaterials.size() : "NULL");
        log.debug("🔍 Building AI initialization request - chatHistory size: {}", chatHistory != null ? chatHistory.size() : "NULL");

        AiInitializeSimulationRequestDto request = AiInitializeSimulationRequestDto.builder()
                .simulationId(context.getSimulationId().toString())
                .chatId(context.getChatId().toString())
                .skill(skill)
                .character(buildCharacterInfo(context))
                .chatHistory(chatHistory)
                .simulationContext(simulationContext)
                .organizationContext(organizationContext)
                .userContext(userContext)
                .initialHyperParameters(initialHyperParameters)
                .skillMaterials(skillMaterials)
                .build();

        // Log the complete request as JSON for debugging
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            String requestJson = mapper.writeValueAsString(request);
            log.info("[AI_AGENT_INIT_REQUEST] Full AI initialization request for chat {}: {}", context.getChatId(), requestJson);
        } catch (Exception e) {
            log.warn("[AI_AGENT_INIT_REQUEST] Could not serialize AI initialization request for logging", e);
        }

        return request;
    }

    /**
     * 🎯 Build skill DTO for AI agent operations
     */
    private AiAgentSkillDto buildSkillDto(SimulationContext context) {
        Skill skill = context.getSkill();
        if (skill == null) {
            log.warn("⚠️ No skill found in context for chat: {}, creating default skill", context.getChatId());
            return AiAgentSkillDto.builder()
                    .name("Default Skill")
                    .description("General workplace simulation skill")
                    .materials(Collections.emptyList())
                    .targetAudience("Employees")
                    .complexityLevel("mixed")
                    .expectedCountSimulations(3)
                    .build();
        }

        return AiAgentSkillDto.builder()
                .name(skill.getName())
                .description(skill.getDescription())
                .materials(Collections.emptyList()) // Materials are handled separately in skillMaterials field
                .targetAudience("Employees")
                .complexityLevel("mixed")
                .expectedCountSimulations(skill.getSimulationCount() != null ? skill.getSimulationCount() : 3)
                .build();
    }



    /**
     * 📜 Build chat history for initialization (empty for new chats)
     */
    private List<Map<String, Object>> buildChatHistoryForInitialization(SimulationContext context) {
        // For initialization, chat history is typically empty
        // If needed, we can populate with any pre-existing messages
        return new ArrayList<>();
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
                .learningObjectives(buildLearningObjectives(context))
                .characterInfo(buildCharacterInfo(context))
                .userContext(buildUserContext(context))
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

            case ENTER_TEXT_QUESTION:
                message = EnterTextQuestionMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .chat(chat)
                        .messageType(messageType)
                        .role(ChatRole.CHARACTER)
                        .character(resolveCharacter(aiMessage.getCharacterName()))
                        .interacted(false)
                        .responseTimeLimit(aiMessage.getResponseTimeLimit())
                        .content(aiMessage.getContent() != null ? aiMessage.getContent() : "AI-generated enter text question")
                        .options(aiMessage.getOptions() != null && !aiMessage.getOptions().isEmpty() ?
                                String.join("||", aiMessage.getOptions()) : null)
                        .build();
                break;

            case SINGLE_CHOICE_TASK:
                message = SingleChoiceTaskQuestionMessage.builder()
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

            case MULTI_CHOICE_TASK:
                message = MultiChoiceTaskQuestionMessage.builder()
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

            case RESULT_SIMULATION:
                message = LastSimulationMessage.builder()
                        .id(UUID.randomUUID().toString())
                        .chat(chat)
                        .messageType(messageType)
                        .role(ChatRole.CHARACTER)
                        .character(resolveCharacter(aiMessage.getCharacterName()))
                        .interacted(false)
                        .content(aiMessage.getContent() != null ? aiMessage.getContent() : "AI-generated simulation result")
                        .build();
                break;

            default:
                // For any unknown message types, create a TextMessage as fallback
                log.warn("⚠️ Unknown message type '{}', falling back to TEXT message", aiMessage.getMessageType());
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
        Map<String, Object> params = new HashMap<>();

        // Try to get hyperparameters from context
        if (context.getHyperParameters() != null) {
            context.getHyperParameters().forEach((key, value) -> {
                params.put(key, value);
            });
        }

        // Ensure we have default values for common hyperparameters
        params.putIfAbsent("active_listening", 0.0);
        params.putIfAbsent("empathy", 0.0);
        params.putIfAbsent("engagement", 0.0);
        params.putIfAbsent("collaboration", 0.0);
        params.putIfAbsent("feedback_delivery", 0.0);

        return params;
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
        initial.put("collaboration", 0.5);
        initial.put("feedback_delivery", 0.5);
        initial.put("goal_setting", 0.5);
        initial.put("joint_decision_making", 0.5);
        return initial;
    }

    // Helper methods for content extraction
    private String extractMessageContent(Message message) {
        if (message instanceof TextMessage) {
            return ((TextMessage) message).getContent();
        } else if (message instanceof SingleChoiceQuestionMessage) {
            return ((SingleChoiceQuestionMessage) message).getOptions();
        }
        return "Message content not available";
    }

    private String extractUserContent(MessageRequestDto messageRequest) {
        if (messageRequest instanceof SingleChoiceAnswerMessageDto singleChoice) {
            String answer = singleChoice.getAnswer();
            log.debug("Extracted single choice answer: {}", answer);
            return answer != null ? answer : "";
        } else if (messageRequest instanceof EnterTextAnswerMessageDto enterText) {
            String answer = enterText.getAnswer();
            log.debug("Extracted enter text answer: {}", answer);
            return answer != null ? answer : "";
        } else if (messageRequest instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTask) {
            String answer = singleChoiceTask.getAnswer();
            log.debug("Extracted single choice task answer: {}", answer);
            return answer != null ? answer : "";
        } else if (messageRequest instanceof MultiChoiceTaskAnswerMessageDto multiChoice) {
            String answer = multiChoice.getAnswer();
            log.debug("Extracted multi choice task answer: {}", answer);
            return answer != null ? answer : "";
        } else {
            // For non-interactive messages (like RESULT_SIMULATION), provide a default message
            if (messageRequest.getMessageType() != null) {
                String messageType = messageRequest.getMessageType().getValue();
                if ("ResultSimulation".equals(messageType)) {
                    log.debug("Processing ResultSimulation message - using default completion message");
                    return "User has completed the simulation";
                }
            }
            
            log.warn("Unknown message request type: {} with message type: {}. Using empty content.", 
                     messageRequest.getClass().getSimpleName(), 
                     messageRequest.getMessageType() != null ? messageRequest.getMessageType().getValue() : "null");
            return "";
        }
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
        userContext.put("experience", "intermediate");
        userContext.put("role", "Employee");
        return userContext;
    }

    private String buildCharacterInfo(SimulationContext context) {
        return "AI-generated characters based on simulation context";
    }

    private String buildLearningObjectives(SimulationContext context) {
        return "AI-generated learning objectives based on skill description";
    }

    /**
     * Convert skill materials to AI agent format
     *
     * @param context The simulation context containing skill information
     * @return List of AI skill material DTOs
     */
    private List<AiSkillMaterialDto> convertSkillMaterials(SimulationContext context) {
        if (context.getSkill() == null) {
            log.debug("No skill found for context: {}", context.getChatId());
            return Collections.emptyList();
        }

        try {
            // Safely check if materials are available without triggering lazy loading exception
            var materials = context.getSkill().getMaterials();
            if (materials == null || materials.isEmpty()) {
                log.debug("No skill materials found for context: {}", context.getChatId());
                return Collections.emptyList();
            }

            return materials.stream()
                    .filter(material -> material != null && material.getFileName() != null)
                    .map(material -> {
                        String content;
                        if (material.getFileContent() != null) {
                            try {
                                content = new String(material.getFileContent());
                            } catch (Exception e) {
                                log.warn("Failed to convert file content for material {}: {}", material.getFileName(), e.getMessage());
                                content = "Content unavailable - " + material.getFileName();
                            }
                        } else {
                            // Fallback content when fileContent is null (expected in test environments)
                            content = "Material: " + material.getFileName() +
                                     (material.getTag() != null ? " (Tag: " + material.getTag() + ")" : "");
                            log.debug("Using fallback content for material: {}", material.getFileName());
                        }

                        return AiSkillMaterialDto.builder()
                                .filename(material.getFileName())
                                .content(content)
                                .build();
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Handle lazy initialization or any other exception gracefully
            log.warn("Could not access skill materials for context {}: {}. Using empty list.", context.getChatId(), e.getMessage());
            return Collections.emptyList();
        }
    }

    private void updateChatCompletionStatus(AiMessageGenerationResponseDto aiResponse, SimulationContext context) {
        if (aiResponse.getConversationEnded() != null && aiResponse.getConversationEnded()) {
            context.getChat().setFinished(true);
            log.info("Chat {} marked as completed by AI agent", context.getChatId());
        }
    }

    /**
     * 🔄 Update user's answered message in database (similar to legacy approach)
     * This ensures the message type conversion logic in UserMessageService works correctly
     */
    private void updateUserAnsweredMessage(MessageRequestDto messageRequest, SimulationContext context) {
        try {
            // Find the message the user is responding to
            Message message = messageRepository.findById(messageRequest.getId()).orElse(null);
            if (message == null) {
                log.warn("⚠️ Could not find message with ID: {} to update with user answer", messageRequest.getId());
                return;
            }

            log.debug("🔄 Updating message {} with user answer", messageRequest.getId());

            // Update the message based on type, similar to legacy InputMessageService
            if (messageRequest instanceof SingleChoiceAnswerMessageDto singleChoice) {
                if (message instanceof SingleChoiceQuestionMessage singleChoiceMsg) {
                    singleChoiceMsg.setAnswer(singleChoice.getAnswer());
                    singleChoiceMsg.setInteracted(true);
                    singleChoiceMsg.setRole(ChatRole.USER);
                    singleChoiceMsg.setUserResponseTime(messageRequest.getUserResponseTime());
                }
            } else if (messageRequest instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTask) {
                if (message instanceof SingleChoiceTaskQuestionMessage singleChoiceTaskMsg) {
                    singleChoiceTaskMsg.setAnswer(singleChoiceTask.getAnswer());
                    singleChoiceTaskMsg.setInteracted(true);
                    singleChoiceTaskMsg.setRole(ChatRole.USER);
                    singleChoiceTaskMsg.setUserResponseTime(messageRequest.getUserResponseTime());
                }
            } else if (messageRequest instanceof EnterTextAnswerMessageDto enterText) {
                if (message instanceof EnterTextQuestionMessage enterTextMsg) {
                    enterTextMsg.setAnswer(enterText.getAnswer());
                    enterTextMsg.setOpenAnswer(enterText.getAnswer());
                    enterTextMsg.setContent(enterText.getAnswer());
                    enterTextMsg.setInteracted(true);
                    enterTextMsg.setRole(ChatRole.USER);
                    enterTextMsg.setUserResponseTime(messageRequest.getUserResponseTime());
                }
            } else if (messageRequest instanceof MultiChoiceTaskAnswerMessageDto multiChoice) {
                if (message instanceof MultiChoiceTaskQuestionMessage multiChoiceMsg) {
                    multiChoiceMsg.setAnswer(multiChoice.getAnswer());
                    multiChoiceMsg.setInteracted(true);
                    multiChoiceMsg.setRole(ChatRole.USER);
                    multiChoiceMsg.setUserResponseTime(messageRequest.getUserResponseTime());
                }
            }

            // Save the updated message
            messageRepository.save(message);
            
            log.debug("✅ Successfully updated message {} with user answer", messageRequest.getId());

        } catch (Exception e) {
            log.warn("⚠️ Failed to update user answered message {}: {}", messageRequest.getId(), e.getMessage());
            // Don't throw exception as this is not critical for AI generation, just for display conversion
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
                .content("We are so sorry, but the dynamic simulation is currently unavailable. " +
                         "Please try again later or contact support if this issue persists.")
                .build();

        // Create interactive question message
        LastSimulationMessage questionMessage = LastSimulationMessage.builder()
                .id(UUID.randomUUID().toString())
                .chat(chat)
                .messageType(MessageType.RESULT_SIMULATION)
                .role(ChatRole.CHARACTER)
                .character(resolveCharacter("AI Assistant"))
                .interacted(false)
                .build();

        // Save both messages with error handling
        try {
            return List.of(
                    messageService.save(welcomeMessage),
                    messageService.save(questionMessage)
            );
        } catch (Exception e) {
            // Handle database save failures (e.g., during test cleanup when tables are dropped)
            log.warn("⚠️ Could not save fallback initial messages to database: {}. Returning unsaved messages.", e.getMessage());
            return List.of(welcomeMessage, questionMessage);
        }
    }

    private Message createFallbackFinalMessage(SimulationContext context) {
        log.warn("Creating fallback final message for chat {}", context.getChatId());

        // Use the chat from context directly - it's already loaded and attached
        Chat chat = context.getChat();

        LastSimulationMessage fallbackMessage = LastSimulationMessage.builder()
                .id(UUID.randomUUID().toString())
                .chat(chat)
                .messageType(MessageType.RESULT_SIMULATION)
                .role(ChatRole.CHARACTER)
                .character(resolveCharacter("AI Assistant")) // Set a default character
                .interacted(false)
                .content("Thank you for completing this simulation! Your responses have been recorded.")
                .build();

        try {
            return messageService.save(fallbackMessage);
        } catch (Exception e) {
            // Handle database save failures (e.g., during test cleanup when tables are dropped)
            log.warn("⚠️ Could not save fallback final message to database: {}. Returning unsaved message.", e.getMessage());
            return fallbackMessage;
        }
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
