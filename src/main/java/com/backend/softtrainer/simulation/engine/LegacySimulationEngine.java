package com.backend.softtrainer.simulation.engine;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.services.InputMessageServiceFactory;
import com.backend.softtrainer.services.InputMessageServiceInterface;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🏛️ Legacy Simulation Engine - show_predicate Processor
 * 
 * This engine wraps the existing InputMessageService to provide seamless
 * backward compatibility for all existing show_predicate simulations.
 * 
 * Key Features:
 * ✅ 100% backward compatibility with existing simulations
 * ✅ Zero changes to show_predicate evaluation logic
 * ✅ Wraps InputMessageService for message processing
 * ✅ Maintains all existing behavior and performance
 * ✅ Performance monitoring and metrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LegacySimulationEngine implements BaseSimulationEngine {
    
    private final InputMessageServiceFactory inputMessageServiceFactory;
    private final FlowService flowService;
    
    // Performance metrics
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong initializedChats = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private long totalProcessingTime = 0;
    
    @Override
    public SimulationType getSupportedType() {
        return SimulationType.LEGACY;
    }
    
    @Override
    public ChatDataDto processUserMessage(MessageRequestDto messageRequest, SimulationContext context) {
        log.debug("🏛️ Processing user message {} with legacy engine", messageRequest.getId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Get appropriate service from factory
            InputMessageServiceInterface service = inputMessageServiceFactory.getServiceForChat(messageRequest.getChatId());
            ChatDataDto result = service.buildResponse(messageRequest).get();
            
            // Update metrics
            processedMessages.incrementAndGet();
            totalProcessingTime += (System.currentTimeMillis() - startTime);
            
            log.debug("✅ Legacy processing completed for message {} in {}ms", 
                    messageRequest.getId(), System.currentTimeMillis() - startTime);
            
            return result;
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ Legacy engine error processing message {}: {}", 
                    messageRequest.getId(), e.getMessage(), e);
            throw new RuntimeException("Legacy processing failed", e);
        }
    }
    
    @Override
    public List<Message> initializeSimulation(SimulationContext context) {
        log.debug("🎬 Initializing legacy simulation: {} (ID: {})", 
                context.getSimulation().getName(), context.getSimulationId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Use existing FlowService logic - no changes needed!
            var flowTillActions = flowService.getFirstFlowNodesUntilActionable(context.getSimulationId());
            
            // Get appropriate service from factory
            InputMessageServiceInterface service = inputMessageServiceFactory.getServiceForSimulation(context.getSimulation());
            List<Message> initialMessages = service.getAndStoreMessageByFlow(
                    flowTillActions, context.getChat());
            
            // Update metrics
            initializedChats.incrementAndGet();
            totalProcessingTime += (System.currentTimeMillis() - startTime);
            
            log.info("✅ Legacy initialization completed with {} messages in {}ms", 
                    initialMessages.size(), System.currentTimeMillis() - startTime);
            
            return initialMessages;
            
        } catch (Exception e) {
            errorCount.incrementAndGet();
            log.error("❌ Legacy engine error initializing simulation {}: {}", 
                    context.getSimulationId(), e.getMessage(), e);
            throw new RuntimeException("Legacy initialization failed", e);
        }
    }
    
    @Override
    public boolean canHandle(SimulationContext context) {
        if (context == null || context.getSimulation() == null) {
            return false;
        }
        
        // Legacy engine can handle any simulation with FlowNodes
        return context.getSimulation().getNodes() != null && 
               !context.getSimulation().getNodes().isEmpty();
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
        
        var simulation = context.getSimulation();
        
        if (simulation.getNodes() == null || simulation.getNodes().isEmpty()) {
            issues.add("Simulation has no flow nodes");
        }
        
        // Check for basic legacy simulation requirements
        boolean hasPredicates = simulation.getNodes().stream()
                .anyMatch(node -> node.getShowPredicate() != null);
        
        if (!hasPredicates) {
            log.info("ℹ️ Simulation {} has no show_predicate fields, but legacy engine can handle it", 
                    simulation.getName());
        }
        
        // Check for actionable nodes
        boolean hasActionableNodes = simulation.getNodes().stream()
                .anyMatch(node -> node.getMessageType() != null && 
                         node.getMessageType().name().contains("QUESTION"));
        
        if (!hasActionableNodes) {
            issues.add("Simulation has no actionable nodes (questions)");
        }
        
        return issues;
    }
    
    @Override
    public boolean isSimulationComplete(SimulationContext context) {
        // Use existing completion logic
        return context.isMarkedAsCompleted() || 
               context.getHearts() <= 0.0;
    }
    
    @Override
    public EngineMetrics getMetrics() {
        return new LegacyEngineMetrics();
    }
    
    /**
     * 📊 Legacy Engine Metrics Implementation
     */
    private class LegacyEngineMetrics implements EngineMetrics {
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
            return "Legacy-1.0.0 (show_predicate compatible)";
        }
    }
} 