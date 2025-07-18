package com.backend.softtrainer.simulation.engine;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.FlowRule;
import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.SingleChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.dtos.messages.EnterTextAnswerMessageDto;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import com.backend.softtrainer.simulation.rules.ModernRuleEvaluator;
import com.backend.softtrainer.dtos.messages.MultiChoiceTaskAnswerMessageDto;
import com.backend.softtrainer.entities.messages.EnterTextQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceQuestionMessage;
import com.backend.softtrainer.entities.messages.SingleChoiceTaskQuestionMessage;
import com.backend.softtrainer.entities.messages.MultiChoiceTaskQuestionMessage;
import com.backend.softtrainer.repositories.MessageRepository;
import com.backend.softtrainer.entities.enums.ChatRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🚀 Modern Simulation Engine - Rule-Based Processor
 *
 * This engine handles new rule-based simulations with structured flows,
 * modern message types, and enhanced capabilities.
 *
 * Key Features:
 * ✅ Rule-based flow evaluation
 * ✅ Enhanced message processing
 * ✅ Structured transitions and effects
 * ✅ Modern JSON format support
 * ✅ Advanced AI integration
 *
 * Note: This is currently a framework placeholder. It will be fully
 * implemented when we add support for modern JSON simulation formats.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModernSimulationEngine implements BaseSimulationEngine {

    private final ModernRuleEvaluator ruleEvaluator;
    private final MessageRepository messageRepository;

    // Mock dependencies for compilation - will be implemented later
    // private final RuleEngine ruleEngine;
    // private final FlowExecutor flowExecutor;

    // Performance metrics
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong initializedChats = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private long totalProcessingTime = 0;

    @Override
    public SimulationType getSupportedType() {
        return SimulationType.MODERN;
    }

    @Override
    public ChatDataDto processUserMessage(MessageRequestDto messageRequest, SimulationContext context) {
        log.debug("🚀 Processing user message {} with modern engine", messageRequest.getId());

        long startTime = System.currentTimeMillis();

        try {
            // TODO: Add updateUserAnsweredMessage() call here similar to AiGeneratedSimulationEngine
            // to ensure message type conversion works correctly in UserMessageService
            updateUserAnsweredMessage(messageRequest, context);

            // Fetch the updated message to include in the response
            Message updatedInteractedMessage = messageRepository.findById(messageRequest.getId()).orElse(null);

            // Extract user response data
            String selectedOptionId = extractSelectedOptionId(messageRequest);
            String userAnswer = extractUserAnswer(messageRequest);

            // Find current message in flow
            FlowNode currentNode = findCurrentNode(messageRequest, context);
            if (currentNode == null) {
                log.warn("⚠️ Could not find current node for message processing");
                return createErrorResponse(messageRequest.getChatId(), "Current node not found");
            }

            // Process rules for the current node
            processNodeRules(currentNode, context, selectedOptionId, userAnswer);

            // Find and process next messages
            List<Message> nextMessages = findNextMessages(context, currentNode);

            // Update metrics
            processedMessages.incrementAndGet();
            totalProcessingTime += (System.currentTimeMillis() - startTime);

            // Create response
            List<Message> responseMessages = new ArrayList<>();
            if (updatedInteractedMessage != null) {
                responseMessages.add(updatedInteractedMessage);
            }
            responseMessages.addAll(nextMessages);
            ChatDataDto response = new ChatDataDto(responseMessages, new ChatParams(context.getHearts()));

            log.info("✅ Modern engine processed message successfully. Next messages: {}", nextMessages.size());
            return response;

        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ Modern engine error processing message {}: {}",
                    messageRequest.getId(), e.getMessage(), e);
            throw new RuntimeException("Modern processing failed", e);
        }
    }

    /**
     * 🔍 Extract selected option ID from message request
     */
    private String extractSelectedOptionId(MessageRequestDto messageRequest) {
        if (messageRequest instanceof SingleChoiceAnswerMessageDto singleChoice) {
            return singleChoice.getAnswer();
        } else if (messageRequest instanceof SingleChoiceTaskAnswerMessageDto singleChoiceTask) {
            return singleChoiceTask.getAnswer();
        }
        return null;
    }

    /**
     * 📝 Extract user text answer from message request
     */
    private String extractUserAnswer(MessageRequestDto messageRequest) {
        if (messageRequest instanceof EnterTextAnswerMessageDto enterText) {
            return enterText.getAnswer();
        }
        return null;
    }

    /**
     * 🔍 Find current flow node
     */
    private FlowNode findCurrentNode(MessageRequestDto messageRequest, SimulationContext context) {
        // Find the node that corresponds to the current message
        Long messageId = Long.parseLong(messageRequest.getId());
        return context.getSimulation().getNodes().stream()
                .filter(node -> messageId.equals(node.getOrderNumber()))
                .findFirst()
                .orElse(null);
    }

    /**
     * ❌ Create error response
     */
    private ChatDataDto createErrorResponse(Long chatId, String errorMessage) {
        ChatDataDto response = new ChatDataDto(null, new ChatParams(0.0));
        // Set basic error information - exact implementation depends on ChatDataDto structure
        log.error("❌ {}", errorMessage);
        return response;
    }

    /**
     * ⚡ Process rules for a node
     */
    private void processNodeRules(FlowNode node, SimulationContext context, String selectedOptionId, String userAnswer) {
        if (node.getFlowRules() == null || node.getFlowRules().isEmpty()) {
            return;
        }

        log.debug("⚡ Processing {} rules for node {}", node.getFlowRules().size(), node.getOrderNumber());

        for (FlowRule rule : node.getFlowRules()) {
            if (ruleEvaluator.evaluateRule(rule, context, selectedOptionId, userAnswer)) {
                log.debug("✅ Rule {} triggered", rule.getType());
                ruleEvaluator.executeActions(rule.getActions(), context);
            }
        }
    }

    /**
     * 🔍 Find next messages to show
     */
    private List<Message> findNextMessages(SimulationContext context, FlowNode currentNode) {
        // Find nodes that should be shown next based on rules
        List<FlowNode> nextNodes = context.getSimulation().getNodes().stream()
                .filter(node -> shouldShowNode(node, context, currentNode))
                .toList();

        // Convert nodes to messages (this would need proper implementation)
        return List.of(); // Placeholder
    }

    /**
     * 🎯 Check if a node should be shown
     */
    private boolean shouldShowNode(FlowNode node, SimulationContext context, FlowNode previousNode) {
        if (node.getFlowRules() == null || node.getFlowRules().isEmpty()) {
            return false;
        }

        for (FlowRule rule : node.getFlowRules()) {
            if (ruleEvaluator.evaluateRule(rule, context, null, null)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Message> initializeSimulation(SimulationContext context) {
        log.debug("🚀 Initializing modern simulation: {} (ID: {})", 
                context.getSimulation().getName(), context.getSimulationId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // TODO: Implement modern initialization logic
            // For now, create a placeholder message
            Message welcomeMessage = createWelcomeMessage(context);
            
            // Update metrics
            initializedChats.incrementAndGet();
            totalProcessingTime += (System.currentTimeMillis() - startTime);
            
            log.info("✅ Modern initialization completed with 1 message in {}ms", 
                    System.currentTimeMillis() - startTime);
            
            return List.of(welcomeMessage);
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ Modern engine error initializing simulation {}: {}", 
                    context.getSimulationId(), e.getMessage(), e);
            throw new RuntimeException("Modern initialization failed", e);
        }
    }
    
    @Override
    public Message generateFinalMessage(SimulationContext context) {
        log.debug("🎯 Generating final message for modern simulation: {} (ID: {})", 
                context.getSimulation().getName(), context.getSimulationId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Create a modern final message
            Message finalMessage = createFinalMessage(context);
            
            // Update metrics
            totalProcessingTime += (System.currentTimeMillis() - startTime);
            
            log.info("✅ Modern final message generated in {}ms", 
                    System.currentTimeMillis() - startTime);
            
            return finalMessage;
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ Modern engine error generating final message for simulation {}: {}", 
                    context.getSimulationId(), e.getMessage(), e);
            throw new RuntimeException("Modern final message generation failed", e);
        }
    }

    /**
     * 🎯 Create welcome message for modern simulation
     */
    private Message createWelcomeMessage(SimulationContext context) {
        // TODO: Implement proper welcome message creation
        // For now, return a placeholder that would be replaced with actual implementation
        throw new UnsupportedOperationException("Modern simulation welcome message creation not yet implemented");
    }
    
    /**
     * 🎯 Create final message for modern simulation
     */
    private Message createFinalMessage(SimulationContext context) {
        // TODO: Implement proper final message creation
        // For now, return a placeholder that would be replaced with actual implementation
        throw new UnsupportedOperationException("Modern simulation final message creation not yet implemented");
    }

    /**
     * 🎬 Check if node should be shown initially
     */
    private boolean shouldShowInitially(FlowNode node) {
        if (node.getFlowRules() == null || node.getFlowRules().isEmpty()) {
            return false;
        }

        return node.getFlowRules().stream()
                .anyMatch(rule -> "ALWAYS_SHOW".equals(rule.getType()));
    }

    @Override
    public boolean canHandle(SimulationContext context) {
        if (context == null || context.getSimulation() == null) {
            return false;
        }

        // Check if simulation has modern flow rules
        return context.getSimulation().getNodes().stream()
                .anyMatch(node -> node.getFlowRules() != null && !node.getFlowRules().isEmpty());
    }

    @Override
    public List<String> validateSimulation(SimulationContext context) {
        List<String> issues = new java.util.ArrayList<>();

        if (context == null) {
            issues.add("Simulation context is null");
            return issues;
        }

        if (context.getSimulation() == null) {
            issues.add("Simulation is null");
            return issues;
        }

        // TODO: Implement modern simulation validation
        // This would check for:
        // 1. Required rule structures
        // 2. Valid transition definitions
        // 3. Proper message type configurations
        // 4. Compatible metadata format

        issues.add("Modern simulation validation not yet implemented");

        return issues;
    }

    @Override
    public boolean isSimulationComplete(SimulationContext context) {
        // TODO: Implement modern completion logic
        // This would use rule-based completion criteria

        // For now, use basic completion logic
        return context.isMarkedAsCompleted() ||
               context.getHearts() <= 0.0;
    }

    @Override
    public EngineMetrics getMetrics() {
        return new ModernEngineMetrics();
    }

    /**
     * 📊 Modern Engine Metrics Implementation
     */
    private class ModernEngineMetrics implements EngineMetrics {
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
            long totalMessages = processedMessages.get() + initializedChats.get();
            if (totalMessages == 0) {
                return 0.0;
            }
            return (double) totalProcessingTime / totalMessages;
        }

        @Override
        public long getErrorCount() {
            return errorCount.get();
        }

        @Override
        public String getEngineVersion() {
            return "Modern-1.0.0 (rule-based framework)";
        }
    }

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
}
