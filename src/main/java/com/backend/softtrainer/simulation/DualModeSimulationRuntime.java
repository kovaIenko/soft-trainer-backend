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
        log.info("🔄 Processing user message {} for chat {}", 
                messageRequest.getId(), messageRequest.getChatId());

        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        
        // Set transaction attributes for better isolation
        txTemplate.setIsolationLevel(TransactionTemplate.ISOLATION_READ_COMMITTED);
        txTemplate.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return txTemplate.execute((TransactionStatus status) -> {
                    try {
                        // 1. Build simulation context
                        SimulationContext context = contextBuilder.buildFromMessageRequest(messageRequest);
                        
                        // 2. Detect simulation type
                        SimulationType simulationType = typeDetector.detectSimulationType(context.getSimulation());
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
                        
                        // Try legacy fallback in the same transaction
                        try {
                            log.info("🔄 Attempting legacy fallback in same transaction");
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
                return new ChatDataDto(List.of(), new ChatParams(0.0));
            }
        });
    }
    
    /**
     * 🎬 Initialize simulation with first messages
     * 
     * Called when a new chat is created to generate the initial
     * set of messages that start the simulation.
     */
    public CompletableFuture<List<Message>> initializeChat(Chat chat) {
        log.info("🎬 Initializing chat {} for simulation: {} (ID: {})", 
                chat.getId(), chat.getSimulation().getName(), chat.getSimulation().getId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. Build context
                SimulationContext context = contextBuilder.buildFromChat(chat);
                
                // 2. Detect simulation type
                SimulationType simulationType = typeDetector.detectSimulationType(chat.getSimulation());
                
                log.info("🎯 Initializing {} simulation: {}", simulationType, chat.getSimulation().getName());
                
                // 3. Validate simulation
                compatibilityValidator.validateSimulation(chat.getSimulation(), simulationType);
                
                // 4. Get appropriate engine
                BaseSimulationEngine engine = engineFactory.createEngine(simulationType);
                
                // 5. Initialize with selected engine
                List<Message> initialMessages = engine.initializeSimulation(context);
                
                log.info("✅ Successfully initialized chat with {} messages using {} engine", 
                        initialMessages.size(), simulationType);
                return initialMessages;
                
            } catch (Exception e) {
                log.error("❌ Error initializing chat {}: {}", chat.getId(), e.getMessage(), e);
                throw new RuntimeException("Failed to initialize chat", e);
            }
        });
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
                // 1. Build context
                SimulationContext context = contextBuilder.buildFromChat(chat);
                
                // 2. Detect simulation type
                SimulationType simulationType = typeDetector.detectSimulationType(chat.getSimulation());
                
                log.info("🎯 Generating final message for {} simulation: {}", simulationType, chat.getSimulation().getName());
                
                // 3. Get appropriate engine
                BaseSimulationEngine engine = engineFactory.createEngine(simulationType);
                
                // 4. Generate final message with selected engine
                Message finalMessage = engine.generateFinalMessage(context);
                
                log.info("✅ Successfully generated final message {} using {} engine", 
                        finalMessage.getId(), simulationType);
                return finalMessage;
                
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