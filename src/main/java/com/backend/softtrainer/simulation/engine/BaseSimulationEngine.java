package com.backend.softtrainer.simulation.engine;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.dtos.ChatDataDto;
import com.backend.softtrainer.dtos.messages.MessageRequestDto;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;

import java.util.List;

/**
 * 🎯 Base Simulation Engine Interface
 * 
 * Defines the contract that both Legacy and Modern simulation engines must implement.
 * This ensures consistent behavior across different simulation formats while allowing
 * each engine to optimize for its specific format.
 */
public interface BaseSimulationEngine {
    
    /**
     * 🔧 Get the simulation type this engine handles
     */
    SimulationType getSupportedType();
    
    /**
     * 🚀 Process user message and generate response
     * 
     * This is the main execution method that handles user interactions
     * and generates appropriate responses based on the simulation logic.
     * 
     * @param messageRequest The user's message/response
     * @param context Current simulation context
     * @return Chat data with next messages and updated parameters
     */
    ChatDataDto processUserMessage(MessageRequestDto messageRequest, SimulationContext context);
    
    /**
     * 🎬 Initialize simulation with first messages
     * 
     * Called when a new chat is created to generate the initial
     * set of messages that start the simulation.
     * 
     * @param context Simulation context for the new chat
     * @return List of initial messages to display
     */
    List<Message> initializeSimulation(SimulationContext context);
    
    /**
     * 🎯 Generate final message when simulation cannot continue
     * 
     * Called when hearts reach 0 or simulation needs to end
     * with a final message explaining the outcome.
     * 
     * @param context Simulation context for the chat
     * @return Final message to display
     */
    Message generateFinalMessage(SimulationContext context);
    
    /**
     * ✅ Check if this engine can handle the given simulation
     * 
     * @param context Simulation context to validate
     * @return true if this engine can process the simulation
     */
    boolean canHandle(SimulationContext context);
    
    /**
     * 🔍 Validate simulation format compatibility
     * 
     * @param context Simulation context to validate
     * @return List of validation issues (empty if valid)
     */
    List<String> validateSimulation(SimulationContext context);
    
    /**
     * 📊 Get engine performance metrics
     * 
     * @return Engine performance and usage statistics
     */
    EngineMetrics getMetrics();
    
    /**
     * 🏁 Check if simulation is complete
     * 
     * @param context Current simulation context
     * @return true if simulation has reached an end state
     */
    boolean isSimulationComplete(SimulationContext context);
    
    /**
     * 📈 Engine Performance Metrics
     */
    interface EngineMetrics {
        long getTotalProcessedMessages();
        long getTotalInitializedChats();
        double getAverageProcessingTimeMs();
        long getErrorCount();
        String getEngineVersion();
    }
} 