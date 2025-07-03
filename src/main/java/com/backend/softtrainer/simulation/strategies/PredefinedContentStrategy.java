package com.backend.softtrainer.simulation.strategies;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.simulation.ContentStrategy;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.services.FlowService;
import com.backend.softtrainer.services.InputMessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy for handling predefined/static simulations using FlowNodes
 * This maintains backwards compatibility with existing content
 */
@Service
@AllArgsConstructor
@Slf4j
public class PredefinedContentStrategy implements ContentStrategy {
    
    private final FlowService flowService;
    private final InputMessageService inputMessageService; // Legacy service for now
    
    @Override
    public SimulationMode getMode() {
        return SimulationMode.PREDEFINED;
    }
    
    @Override
    public CompletableFuture<List<Message>> generateResponse(
        SimulationContext context, 
        Message userMessage
    ) {
        log.debug("Generating predefined response for chat: {}", context.getChatId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // For now, use a simple placeholder
                // TODO: Replace with new rule-based flow logic
                return List.of(userMessage); // Placeholder implementation
                
            } catch (Exception e) {
                log.error("Error generating predefined response", e);
                throw new RuntimeException("Failed to generate predefined response", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<List<Message>> initializeSimulation(SimulationContext context) {
        log.debug("Initializing predefined simulation for chat: {}", context.getChatId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get initial flow nodes
                var initialNodes = flowService.getFirstFlowNodesUntilActionable(
                    context.getSimulationId()
                );
                
                // Convert to messages
                return inputMessageService.getAndStoreMessageByFlow(
                    initialNodes, 
                    context.getChat()
                );
                
            } catch (Exception e) {
                log.error("Error initializing predefined simulation", e);
                throw new RuntimeException("Failed to initialize predefined simulation", e);
            }
        });
    }
    
    @Override
    public boolean canHandle(SimulationContext context) {
        // Can handle if simulation has FlowNodes
        return context.getSimulation().getNodes() != null && 
               !context.getSimulation().getNodes().isEmpty();
    }
    
    @Override
    public boolean isSimulationComplete(SimulationContext context) {
        // Check if we've reached the end of the flow
        return context.getChat().isFinished() ||
               !hasMoreNodes(context);
    }
    
    private boolean hasMoreNodes(SimulationContext context) {
        // Simple check - in real implementation would check for next FlowNode
        return context.getMessageCount() < 10; // Arbitrary limit for demo
    }
    
    @Override
    public int getPriority() {
        return 100; // High priority for backwards compatibility
    }
} 