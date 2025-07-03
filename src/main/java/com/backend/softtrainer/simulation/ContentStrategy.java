package com.backend.softtrainer.simulation;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.simulation.context.SimulationContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Strategy interface for handling different types of simulation content generation
 */
public interface ContentStrategy {
    
    /**
     * @return The simulation mode this strategy handles
     */
    SimulationMode getMode();
    
    /**
     * Generate the next message(s) in response to user input
     * 
     * @param context Current simulation context
     * @param userMessage The user's input message
     * @return Future containing the generated message(s)
     */
    CompletableFuture<List<Message>> generateResponse(
        SimulationContext context, 
        Message userMessage
    );
    
    /**
     * Initialize the simulation with starting messages
     * 
     * @param context Simulation context
     * @return Future containing initial messages
     */
    CompletableFuture<List<Message>> initializeSimulation(SimulationContext context);
    
    /**
     * Check if this strategy can handle the given context
     * 
     * @param context Simulation context
     * @return true if this strategy can handle the context
     */
    boolean canHandle(SimulationContext context);
    
    /**
     * Determine if the simulation is complete
     * 
     * @param context Current simulation context
     * @return true if simulation should end
     */
    boolean isSimulationComplete(SimulationContext context);
    
    /**
     * Get priority for strategy selection (higher = more priority)
     * Used when multiple strategies can handle the same context
     * 
     * @return priority value
     */
    default int getPriority() {
        return 0;
    }
} 