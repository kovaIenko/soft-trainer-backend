package com.backend.softtrainer.simulation;

import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.ChatParams;
import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.context.SimulationContextBuilder;
import com.backend.softtrainer.simulation.rules.RuleEngine;
import com.backend.softtrainer.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main simulation engine that orchestrates content strategies and flow control
 * Replaces the complex logic in InputMessageService
 */
@Service
@AllArgsConstructor
@Slf4j
public class SimulationEngine {
    
    private final Map<SimulationMode, ContentStrategy> contentStrategies;
    private final RuleEngine ruleEngine;
    private final MessageService messageService;
    private final SimulationContextBuilder contextBuilder;
    
    /**
     * Process user input and generate next messages
     */
    public CompletableFuture<ChatDataDto> processUserInput(
        Chat chat, 
        Message userMessage
    ) {
        log.info("Processing user input for chat: {}, message: {}", 
            chat.getId(), userMessage.getId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build simulation context
                SimulationContext context = contextBuilder.buildFromChat(chat);
                
                // Get appropriate content strategy
                ContentStrategy strategy = getContentStrategy(context);
                
                // Generate response
                List<Message> responseMessages = strategy.generateResponse(context, userMessage)
                    .get();
                
                // Save messages
                List<Message> savedMessages = responseMessages.stream()
                    .map(messageService::save)
                    .toList();
                
                // Check if simulation is complete
                if (strategy.isSimulationComplete(context)) {
                    chat.setFinished(true);
                    // Save chat state
                }
                
                return new ChatDataDto(savedMessages, new ChatParams(chat.getHearts()));
                
            } catch (Exception e) {
                log.error("Error processing user input for chat: {}", chat.getId(), e);
                throw new RuntimeException("Failed to process user input", e);
            }
        });
    }
    
    /**
     * Initialize a new simulation
     */
    public CompletableFuture<ChatDataDto> initializeSimulation(Chat chat) {
        log.info("Initializing simulation for chat: {}", chat.getId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build initial context
                SimulationContext context = contextBuilder.buildInitialContext(chat);
                
                // Get appropriate content strategy
                ContentStrategy strategy = getContentStrategy(context);
                
                // Generate initial messages
                List<Message> initialMessages = strategy.initializeSimulation(context)
                    .get();
                
                // Save messages
                List<Message> savedMessages = initialMessages.stream()
                    .map(messageService::save)
                    .toList();
                
                return new ChatDataDto(savedMessages, new ChatParams(chat.getHearts()));
                
            } catch (Exception e) {
                log.error("Error initializing simulation for chat: {}", chat.getId(), e);
                throw new RuntimeException("Failed to initialize simulation", e);
            }
        });
    }
    
    /**
     * Get the appropriate content strategy for the given context
     */
    private ContentStrategy getContentStrategy(SimulationContext context) {
        SimulationMode mode = determineSimulationMode(context);
        
        ContentStrategy strategy = contentStrategies.get(mode);
        if (strategy == null) {
            log.warn("No strategy found for mode: {}, falling back to PREDEFINED", mode);
            strategy = contentStrategies.get(SimulationMode.PREDEFINED);
        }
        
        if (strategy == null) {
            throw new IllegalStateException("No content strategy available");
        }
        
        log.debug("Selected content strategy: {} for mode: {}", 
            strategy.getClass().getSimpleName(), mode);
        return strategy;
    }
    
    /**
     * Determine which simulation mode to use based on context
     */
    private SimulationMode determineSimulationMode(SimulationContext context) {
        // Check skill behavior setting first
        SimulationMode skillMode = context.getSimulationMode();
        
        // Apply any runtime overrides or conditions
        if (skillMode == SimulationMode.HYBRID) {
            // For hybrid mode, additional logic to decide when to use AI vs predefined
            return shouldUseAIGeneration(context) ? 
                SimulationMode.DYNAMIC : SimulationMode.PREDEFINED;
        }
        
        return skillMode;
    }
    
    /**
     * Determine if AI generation should be used in hybrid mode
     */
    private boolean shouldUseAIGeneration(SimulationContext context) {
        // Example logic - customize based on your needs
        return context.getSimulationMode() == SimulationMode.DYNAMIC && 
               context.getMessageCount() > 2; // Use AI after initial exchanges
    }
} 