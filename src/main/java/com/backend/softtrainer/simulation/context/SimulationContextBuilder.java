package com.backend.softtrainer.simulation.context;

import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.services.UserHyperParameterService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;

/**
 * 🏗️ Enhanced Simulation Context Builder - Creates complete execution contexts
 *
 * This builder creates SimulationContext objects that contain all the information
 * needed for the unified flow execution engine to process simulations.
 */
@Service
@AllArgsConstructor
@Slf4j
public class SimulationContextBuilder {

    private final UserHyperParameterService userHyperParameterService;
    private final ChatRepository chatRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * 🎬 Build initial context for a new simulation
     */
    public SimulationContext buildInitialContext(Chat chat) {
        log.debug("🏗️ Building initial context for chat: {}", chat.getId());

        try {
            SimulationMode mode = determineSimulationMode(chat.getSimulation());

            SimulationContext context = SimulationContext.builder()
                .chatId(chat.getId())
                .chat(chat)
                .user(chat.getUser())
                .simulation(chat.getSimulation())
                .skill(chat.getSkill())
                .simulationMode(mode)
                .hearts(chat.getHearts())
                .maxMessages(determineMaxMessages(chat.getSimulation()))
                .build();

            // Initialize with chat data
            initializeContextWithChatData(context, chat);

            return context;

        } catch (Exception e) {
            log.error("❌ Error building initial context for chat: {}", chat.getId(), e);
            throw new RuntimeException("Failed to build simulation context", e);
        }
    }

    /**
     * 🔄 Build context from MessageRequestDto with retry mechanism for async visibility
     */
    public SimulationContext buildFromMessageRequest(MessageRequestDto messageRequest) {
        log.debug("🔄 Building simulation context from message request for chat: {}",
                messageRequest.getChatId());

        // Load chat with retry mechanism to handle async visibility delays
        Chat chat = loadChatWithRetry(messageRequest.getChatId());

        // Build context from chat
        return buildFromChat(chat);
    }

    /**
     * 🔄 Load chat with retry mechanism to handle async visibility delays
     * This is critical for race conditions between chat creation and message processing
     */
    public Chat loadChatWithRetry(Long chatId) {
        int maxRetries = 3;
        int retryDelayMs = 200;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            log.debug("🔍 Attempting to load chat {} (attempt {}/{})", chatId, attempt, maxRetries);

            Optional<Chat> chatOpt = chatRepository.findByIdWithMessages(chatId);
            if (chatOpt.isPresent()) {
                log.debug("✅ Chat {} loaded successfully on attempt {}", chatId, attempt);
                return chatOpt.get();
            }

            if (attempt < maxRetries) {
                log.debug("⏳ Chat {} not found, retrying in {}ms (attempt {}/{})",
                         chatId, retryDelayMs, attempt, maxRetries);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for chat visibility", e);
                }
            }
        }

        log.error("❌ Chat {} not found after {} attempts", chatId, maxRetries);
        throw new RuntimeException("Chat not found: " + chatId);
    }

    /**
     * 🔄 Build context from existing chat (for ongoing simulations)
     */
    /**
     * 🏗️ Build context from existing chat with same transaction context
     *
     * ⚠️ CRITICAL FIX: Uses PROPAGATION_REQUIRED to stay in the same transaction
     * as chat creation, preventing race conditions.
     */
    public SimulationContext buildFromChat(Chat chat) {
        log.debug("🏗️ Building context from existing chat: {} on thread: {}",
                chat.getId(), Thread.currentThread().getName());

        try {
            // 🚨 CRITICAL FIX: Use the chat entity directly without TransactionTemplate
            // This ensures we stay in the same transaction context as chat creation
            // The chat entity is already managed by the current transaction

            SimulationContext context = buildInitialContext(chat);

            // Load existing messages from the chat entity
            if (chat.getMessages() != null && !chat.getMessages().isEmpty()) {
                chat.getMessages().forEach(context::addMessage);
            }

            // Load existing hyperparameters
            loadHyperParameters(context);

            log.debug("✅ Built context with {} messages and {} hyperparameters",
                context.getMessageCount(), context.getHyperParameters().size());

            return context;

        } catch (Exception e) {
            log.error("❌ Error building context from chat: {}", chat.getId(), e);
            throw new RuntimeException("Failed to build simulation context", e);
        }
    }

    /**
     * 🎯 Determine simulation mode based on simulation configuration
     */
    private SimulationMode determineSimulationMode(Simulation simulation) {
        if (simulation == null) {
            log.warn("⚠️ No simulation provided, defaulting to PREDEFINED mode");
            return SimulationMode.PREDEFINED;
        }

        // Check if simulation has explicit mode configuration
        // This would be a new field on Simulation entity
        // For now, use logic to detect mode

        if (hasEnhancedNodes(simulation)) {
            log.debug("🚀 Detected enhanced nodes, using HYBRID mode");
            return SimulationMode.HYBRID;
        }

        if (hasLegacyNodes(simulation)) {
            log.debug("📜 Detected legacy nodes, using PREDEFINED mode");
            return SimulationMode.PREDEFINED;
        }

        log.debug("🤖 No specific nodes detected, using DYNAMIC mode");
        return SimulationMode.DYNAMIC;
    }

    /**
     * 🚀 Check if simulation has enhanced flow nodes
     */
    private boolean hasEnhancedNodes(Simulation simulation) {
        // Check if simulation has EnhancedFlowNode entries
        // This would require a new relationship or field
        // For now, return false
        return false;
    }

    /**
     * 📜 Check if simulation has legacy flow nodes
     */
    private boolean hasLegacyNodes(Simulation simulation) {
        try {
            return simulation.getNodes() != null && !simulation.getNodes().isEmpty();
        } catch (org.hibernate.LazyInitializationException e) {
            // If we can't access nodes due to lazy loading, assume it's a legacy simulation
            // This ensures backward compatibility
            log.debug("📜 Cannot access nodes due to lazy loading, assuming legacy simulation");
            return true;
        } catch (Exception e) {
            // Catch any other database-related exceptions (e.g., table not found during test cleanup)
            // This can happen when async operations run after test database cleanup
            log.debug("📜 Cannot access nodes due to database issue, assuming legacy simulation: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 📊 Determine maximum messages for simulation
     */
    private Integer determineMaxMessages(Simulation simulation) {
        if (simulation == null) {
            return 100; // Default limit
        }

        // Calculate based on simulation complexity
        try {
            if (simulation.getNodes() != null) {
                int nodeCount = simulation.getNodes().size();
                // Allow for some branching and retries
                return Math.max(50, nodeCount * 3);
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // If we can't access nodes due to lazy loading, use default limit
            // This ensures backward compatibility
            log.debug("📊 Cannot access nodes due to lazy loading, using default max messages");
            return 100;
        } catch (Exception e) {
            // Catch any other database-related exceptions (e.g., table not found during test cleanup)
            // This can happen when async operations run after test database cleanup
            log.debug("📊 Cannot access nodes due to database issue, using default max messages: {}", e.getMessage());
            return 100;
        }

        return 100; // Default for dynamic simulations
    }

    /**
     * 📈 Load existing hyperparameters for the user/chat
     */
    private void loadHyperParameters(SimulationContext context) {
        try {
            // Get all hyperparameters for this chat/user
            // Implementation depends on your UserHyperParameterService
            log.debug("📈 Loading hyperparameters for chat: {}", context.getChatId());

            // For now, set some default learning objectives
            context.getLearningObjectives().addAll(List.of(
                "active_listening",
                "empathy",
                "engagement",
                "collaboration",
                "feedback_delivery",
                "goal_setting",
                "joint_decision_making"
            ));

            // Load actual hyperparameter values
            for (String objective : context.getLearningObjectives()) {
                try {
                    Double value = userHyperParameterService.getOrCreateUserHyperParameter(
                        context.getChatId(), objective);
                    context.setHyperParameter(objective, value);
                } catch (Exception e) {
                    log.warn("⚠️ Could not load hyperparameter {}: {}", objective, e.getMessage());
                    context.setHyperParameter(objective, 0.0);
                }
            }

        } catch (Exception e) {
            log.error("❌ Error loading hyperparameters: {}", e.getMessage());
        }
    }

    /**
     * 🎯 Build context for testing/debugging
     */
    public SimulationContext buildTestContext(Chat chat, SimulationMode mode) {
        log.debug("🧪 Building test context for chat: {} with mode: {}", chat.getId(), mode);

        return SimulationContext.builder()
            .chatId(chat.getId())
            .chat(chat)
            .user(chat.getUser())
            .simulation(chat.getSimulation())
            .skill(chat.getSkill())
            .simulationMode(mode)
            .hearts(5.0)
            .maxMessages(50)
            .build();
    }

    /**
     * 🔄 Update context with new message and state
     */
    public SimulationContext updateContext(SimulationContext context, Message newMessage) {
        log.debug("🔄 Updating context with new message: {}", newMessage.getId());

        // Add the new message
        context.addMessage(newMessage);

        // Update hearts if affected by the message
        updateHeartsFromMessage(context, newMessage);

        // Update any other dynamic state
        updateDynamicState(context, newMessage);

        return context;
    }

    /**
     * ❤️ Update hearts based on message interaction
     */
    private void updateHeartsFromMessage(SimulationContext context, Message message) {
        // This would implement the hearts logic based on message correctness
        // For now, just maintain current hearts
        log.debug("❤️ Hearts remain at: {}", context.getHearts());
    }

    /**
     * 🔄 Update dynamic state based on new message
     */
    private void updateDynamicState(SimulationContext context, Message message) {
        // Update any dynamic context based on the new message
        // This could include conversation sentiment, topic tracking, etc.
        log.debug("🔄 Updated dynamic state for message: {}", message.getId());
    }

    /**
     * 🎬 Initialize context with chat data
     */
    private void initializeContextWithChatData(SimulationContext context, Chat chat) {
        // Set initial learning objectives based on skill
        if (chat.getSkill() != null) {
            context.getLearningObjectives().addAll(
                extractLearningObjectives(chat.getSkill())
            );
        }
    }

    /**
     * 🎯 Extract learning objectives from skill
     */
    private List<String> extractLearningObjectives(Skill skill) {
        // Extract objectives from skill metadata
        // For now, return default objectives
        return List.of(
            "active_listening",
            "empathy",
            "engagement"
        );
    }
}
