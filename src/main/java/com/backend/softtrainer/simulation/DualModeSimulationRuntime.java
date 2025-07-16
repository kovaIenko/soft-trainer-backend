package com.backend.softtrainer.simulation;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.context.SimulationContextBuilder;
import com.backend.softtrainer.simulation.engine.BaseSimulationEngine;
import com.backend.softtrainer.simulation.engine.LegacySimulationEngine;
import com.backend.softtrainer.simulation.engine.ModernSimulationEngine;
import com.backend.softtrainer.simulation.engine.SimulationEngineFactory;
import com.backend.softtrainer.simulation.detection.SimulationTypeDetector;
import com.backend.softtrainer.simulation.validation.SimulationCompatibilityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 Dual-Mode Simulation Runtime - Universal Simulation Processor
 * 
 * This is the core engine that provides seamless support for both:
 * - Legacy show_predicate simulations (100% backward compatible)
 * - Modern rule-based simulations (new enhanced features)
 * 
 * Key Features:
 * ✅ Automatic simulation type detection
 * ✅ Zero-regression routing to appropriate engine
 * ✅ Unified API for all simulation formats
 * ✅ Transparent execution model
 * ✅ Comprehensive error handling with fallbacks
 * ✅ Performance monitoring and metrics
 * 
 * Architecture:
 * 1. SimulationTypeDetector - Analyzes simulation structure
 * 2. SimulationEngineFactory - Creates appropriate processor
 * 3. LegacySimulationEngine - Handles show_predicate flows
 * 4. ModernSimulationEngine - Handles rule-based flows
 * 5. SimulationCompatibilityValidator - Ensures format compliance
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DualModeSimulationRuntime {
    
    private final SimulationTypeDetector typeDetector;
    private final SimulationEngineFactory engineFactory;
    private final SimulationContextBuilder contextBuilder;
    private final SimulationCompatibilityValidator compatibilityValidator;
    private final PlatformTransactionManager transactionManager;
    
    /**
     * 🚀 Main Entry Point: Process User Message (Universal API)
     * 
     * This method replaces all direct calls to InputMessageService.buildResponse()
     * and provides transparent dual-mode processing.
     */
    public CompletableFuture<ChatDataDto> processUserMessage(MessageRequestDto messageRequest) {
        log.info("🚀 Starting dual-mode message processing for chat {} on thread: {}", 
                messageRequest.getChatId(), Thread.currentThread().getName());
        
        return CompletableFuture.supplyAsync(() -> {
            // Log async thread information
            String asyncThreadName = Thread.currentThread().getName();
            log.debug("🔄 Async processing started on thread: {} for chat: {}", 
                    asyncThreadName, messageRequest.getChatId());
            
            try {
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                
                // Set transaction attributes for better isolation
                txTemplate.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
                txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                
                return txTemplate.execute((TransactionStatus status) -> {
                    SimulationType simulationType = null; // Declare outside try block
                    SimulationContext context = null;
                    
                    try {
                        // 1. Build simulation context
                        context = contextBuilder.buildFromMessageRequest(messageRequest);
                        
                        // 2. Detect simulation type
                        simulationType = typeDetector.detectSimulationType(context.getSimulation());
                        log.info("📊 Detected simulation type: {} for simulation: {} (ID: {})", 
                                simulationType, context.getSimulation().getName(), context.getSimulationId());
                        
                        // 3. Validate compatibility
                        compatibilityValidator.validateSimulation(context.getSimulation(), simulationType);
                        
                        // 4. Get appropriate engine
                        BaseSimulationEngine engine = engineFactory.createEngine(simulationType);
                        
                        // 5. Process with selected engine
                        ChatDataDto result = engine.processUserMessage(messageRequest, context);
                        log.info("✅ Successfully processed message with {} engine", simulationType);
                        return result;
                        
                    } catch (Exception e) {
                        log.error("❌ Error in dual-mode processing for chat {}: {}", 
                                messageRequest.getChatId(), e.getMessage(), e);
                        
                        // Determine simulation type for fallback decision
                        SimulationType finalSimulationType = simulationType;
                        if (finalSimulationType == null && context != null) {
                            try {
                                finalSimulationType = typeDetector.detectSimulationType(context.getSimulation());
                            } catch (Exception typeDetectionError) {
                                log.warn("🚨 Failed to detect simulation type for fallback decision", typeDetectionError);
                            }
                        }
                        
                        // For AI-generated simulations, NEVER fallback to legacy
                        // This ensures proper error handling without masking issues
                        if (finalSimulationType == SimulationType.AI_GENERATED) {
                            log.error("🚫 AI-generated simulation failed - no legacy fallback allowed");
                            status.setRollbackOnly();
                            throw new RuntimeException("AI-generated simulation failed: " + e.getMessage(), e);
                        }
                        
                        // Try legacy fallback only for non-AI simulations
                        try {
                            log.info("🔄 Attempting legacy fallback for {} simulation", finalSimulationType);
                            return fallbackToLegacyProcessing(messageRequest, e);
                        } catch (Exception fallbackError) {
                            log.error("❌ Legacy fallback failed: {}", fallbackError.getMessage(), fallbackError);
                            status.setRollbackOnly();
                            throw fallbackError;
                        }
                    }
                });
            } catch (Exception e) {
                log.error("❌ All processing attempts failed for chat {}", messageRequest.getChatId(), e);
                throw new RuntimeException("Dual-mode processing failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 🎬 Initialize simulation with first messages
     * 
     * Called when a new chat is created to generate the initial
     * set of messages that start the simulation.
     * 
     * ⚠️ CRITICAL FIX: This method now runs SYNCHRONOUSLY in the same thread/transaction
     * as chat creation to prevent race conditions and ensure chat visibility.
     */
    public CompletableFuture<List<Message>> initializeChat(Chat chat) {
        log.info("🎬 Initializing chat {} for simulation: {} (ID: {}) on thread: {}", 
                chat.getId(), chat.getSimulation().getName(), chat.getSimulation().getId(), 
                Thread.currentThread().getName());
        
        // 🚨 CRITICAL FIX: Always run synchronously to prevent race conditions
        // The chat creation and initialization must happen in the same transaction context
        // to ensure the chat is visible during initialization
        try {
            // Use the same transaction context as the chat creation
            // This ensures the chat is visible to the initialization process
            SimulationContext context = contextBuilder.buildFromChat(chat);
            
            // Detect simulation type
            SimulationType simulationType = typeDetector.detectSimulationType(chat.getSimulation());
            
            log.info("🎯 Initializing {} simulation: {}", simulationType, chat.getSimulation().getName());
            
            // Validate simulation
            compatibilityValidator.validateSimulation(chat.getSimulation(), simulationType);
            
            // Get appropriate engine
            BaseSimulationEngine engine = engineFactory.createEngine(simulationType);
            
            // Initialize with selected engine (synchronously)
            List<Message> initialMessages = engine.initializeSimulation(context);
            
            log.info("✅ Successfully initialized chat with {} messages using {} engine", 
                    initialMessages.size(), simulationType);
            
            return CompletableFuture.completedFuture(initialMessages);
            
        } catch (Exception e) {
            log.error("❌ Error initializing chat {}: {}", chat.getId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(new RuntimeException("Failed to initialize chat", e));
        }
    }
    
    /**
     * 🎯 Generate Last Simulation Message (Universal API)
     * 
     * This method replaces direct calls to InputMessageService.generateLastSimulationMessage()
     * and provides dual-mode support for final message generation.
     */
    public CompletableFuture<Message> generateLastSimulationMessage(Chat chat) {
        log.info("🎯 Generating last simulation message for chat {} (hearts exhausted)", chat.getId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
                txTemplate.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
                txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
                
                return txTemplate.execute((TransactionStatus status) -> {
                    try {
                        // 🚨 CRITICAL FIX: Reload chat with messages in new transaction context
                        // This prevents LazyInitializationException when accessing chat.getMessages()
                        Chat reloadedChat = contextBuilder.loadChatWithRetry(chat.getId());
                        
                        // 1. Build context with reloaded chat
                        SimulationContext context = contextBuilder.buildFromChat(reloadedChat);
                        
                        // 2. Detect simulation type
                        SimulationType simulationType = typeDetector.detectSimulationType(reloadedChat.getSimulation());
                        
                        log.info("🎯 Generating final message for {} simulation: {}", simulationType, reloadedChat.getSimulation().getName());
                        
                        // 3. Get appropriate engine
                        BaseSimulationEngine engine = engineFactory.createEngine(simulationType);
                        
                        // 4. Generate final message with selected engine
                        Message finalMessage = engine.generateFinalMessage(context);
                        
                        log.info("✅ Successfully generated final message {} using {} engine", 
                                finalMessage.getId(), simulationType);
                        return finalMessage;
                        
                    } catch (Exception e) {
                        log.error("❌ Error generating final message for chat {}: {}", chat.getId(), e.getMessage(), e);
                        status.setRollbackOnly();
                        throw new RuntimeException("Failed to generate final message", e);
                    }
                });
                
            } catch (Exception e) {
                log.error("❌ Error generating final message for chat {}: {}", chat.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to generate final message", e);
            }
        });
    }
    
    /**
     * 🔍 Get Simulation Runtime Info (Diagnostic API)
     * 
     * Provides detailed information about how a simulation will be processed
     */
    public SimulationRuntimeInfo getSimulationRuntimeInfo(Simulation simulation) {
        log.debug("🔍 Analyzing runtime info for simulation: {} (ID: {})", 
                simulation.getName(), simulation.getId());
        
        try {
            SimulationType type = typeDetector.detectSimulationType(simulation);
            boolean isCompatible = compatibilityValidator.isCompatible(simulation, type);
            
            return SimulationRuntimeInfo.builder()
                    .simulationId(simulation.getId())
                    .simulationName(simulation.getName())
                    .detectedType(type)
                    .isCompatible(isCompatible)
                    .engineClass(engineFactory.getEngineClass(type))
                    .nodeCount(simulation.getNodes().size())
                    .hasLegacyPredicates(hasLegacyPredicates(simulation))
                    .hasModernRules(hasModernRules(simulation))
                    .compatibilityIssues(compatibilityValidator.validateAndGetIssues(simulation, type))
                    .build();
                    
        } catch (Exception e) {
            log.error("❌ Error analyzing simulation {}: {}", simulation.getId(), e.getMessage());
            return SimulationRuntimeInfo.builder()
                    .simulationId(simulation.getId())
                    .simulationName(simulation.getName())
                    .detectedType(SimulationType.UNKNOWN)
                    .isCompatible(false)
                    .compatibilityIssues(List.of("Analysis failed: " + e.getMessage()))
                    .build();
        }
    }
    
    /**
     * 🚨 Fallback to Legacy Processing
     * 
     * When modern processing fails, use the battle-tested legacy system
     */
    private ChatDataDto fallbackToLegacyProcessing(MessageRequestDto messageRequest, Exception originalError) {
        log.warn("🚨 Falling back to legacy processing for chat {} due to error: {}", 
                messageRequest.getChatId(), originalError.getMessage());
        
        // Get legacy engine directly
        LegacySimulationEngine legacyEngine = (LegacySimulationEngine) engineFactory.createEngine(SimulationType.LEGACY);
        
        // Build fresh context for legacy processing to avoid any stale entity references
        SimulationContext context = contextBuilder.buildFromMessageRequest(messageRequest);
        
        return legacyEngine.processUserMessage(messageRequest, context);
    }
    
    /**
     * 🔍 Helper: Check for legacy predicates
     */
    private boolean hasLegacyPredicates(Simulation simulation) {
        return simulation.getNodes().stream()
                .anyMatch(node -> node.getShowPredicate() != null && !node.getShowPredicate().isEmpty());
    }
    
    /**
     * 🔍 Helper: Check for modern rules
     */
    private boolean hasModernRules(Simulation simulation) {
        // This would check for modern rule structures when EnhancedFlowNode is enabled
        // For now, return false since we're focusing on legacy compatibility
        return false;
    }
    
    /**
     * 📊 Simulation Type Enumeration
     */
    public enum SimulationType {
        LEGACY("Legacy show_predicate simulation"),
        MODERN("Modern rule-based simulation"),
        HYBRID("Hybrid simulation with both legacy and modern elements"),
        AI_GENERATED("AI-generated simulation with real-time content"),
        UNKNOWN("Unknown or invalid simulation format");
        
        private final String description;
        
        SimulationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 📋 Runtime Information DTO
     */
    @lombok.Builder
    @lombok.Data
    public static class SimulationRuntimeInfo {
        private Long simulationId;
        private String simulationName;
        private SimulationType detectedType;
        private boolean isCompatible;
        private String engineClass;
        private int nodeCount;
        private boolean hasLegacyPredicates;
        private boolean hasModernRules;
        private List<String> compatibilityIssues;
    }
} 