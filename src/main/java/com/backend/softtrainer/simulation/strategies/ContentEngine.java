package com.backend.softtrainer.simulation.strategies;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.EnhancedFlowNode;
import com.backend.softtrainer.entities.enums.SimulationMode;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.simulation.ContentStrategy;
import com.backend.softtrainer.services.MessageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 🎨 Content Engine - Orchestrates content generation for different node types
 * 
 * This service determines the appropriate content generation strategy based on:
 * - Node type (FlowNode vs EnhancedFlowNode)
 * - Simulation mode (PREDEFINED, DYNAMIC, HYBRID)
 * - Content complexity and AI requirements
 */
@Service
@AllArgsConstructor
@Slf4j
public class ContentEngine {
    
    private final MessageService messageService;
    
    /**
     * Get strategy for the given mode (simplified implementation)
     */
    private String getStrategyName(SimulationMode mode) {
        return switch (mode) {
            case PREDEFINED -> "predefined";
            case DYNAMIC -> "dynamic";
            case HYBRID -> "hybrid";
            default -> "predefined";
        };
    }
    
    /**
     * 🎯 Generate content for a list of nodes (main method used by FlowExecutor)
     */
    public List<Message> generateContentForNodes(
        SimulationContext context, 
        List<Object> nodes
    ) {
        log.debug("🎨 Generating content for {} nodes in mode: {}", 
            nodes.size(), context.getSimulationMode());
        
        List<Message> generatedMessages = new ArrayList<>();
        
        for (Object node : nodes) {
            try {
                List<Message> nodeMessages = generateContentForSingleNode(context, node);
                generatedMessages.addAll(nodeMessages);
                
                log.debug("✅ Generated {} messages for node", nodeMessages.size());
                
            } catch (Exception e) {
                log.error("❌ Error generating content for node: {}", node, e);
                
                // Generate fallback content
                Message fallbackMessage = generateFallbackMessage(context, node);
                if (fallbackMessage != null) {
                    generatedMessages.add(fallbackMessage);
                }
            }
        }
        
        log.info("🎨 Generated total of {} messages for {} nodes", 
            generatedMessages.size(), nodes.size());
        
        return generatedMessages;
    }
    
    /**
     * 🎯 Generate content for a single node
     */
    private List<Message> generateContentForSingleNode(
        SimulationContext context, 
        Object node
    ) {
        if (node instanceof FlowNode flowNode) {
            return generateContentForFlowNode(context, flowNode);
        } else if (node instanceof EnhancedFlowNode enhancedNode) {
            return generateContentForEnhancedNode(context, enhancedNode);
        } else {
            log.warn("⚠️ Unknown node type: {}", node.getClass().getSimpleName());
            return List.of();
        }
    }
    
    /**
     * 📜 Generate content for legacy FlowNode
     */
    private List<Message> generateContentForFlowNode(
        SimulationContext context, 
        FlowNode flowNode
    ) {
        log.debug("📜 Generating content for legacy FlowNode: {}", flowNode.getOrderNumber());
        
        // For now, return empty list - will be implemented with proper Message creation
        // TODO: Implement proper FlowNode to Message conversion
        return List.of();
    }
    
    /**
     * 🚀 Generate content for modern EnhancedFlowNode
     */
    private List<Message> generateContentForEnhancedNode(
        SimulationContext context, 
        EnhancedFlowNode enhancedNode
    ) {
        log.debug("🚀 Generating content for EnhancedFlowNode: {}", enhancedNode.getMessageId());
        
        // Determine appropriate strategy based on node configuration
        String strategyName = getStrategyName(determineNodeGenerationMode(context, enhancedNode));
        
        log.debug("Using strategy: {} for enhanced node", strategyName);
        
        // For now, return empty list - will be implemented with proper Message creation
        // TODO: Implement proper EnhancedFlowNode to Message conversion
        return List.of();
    }
    
    /**
     * 🤖 Determine the generation mode for an enhanced node
     */
    private SimulationMode determineNodeGenerationMode(
        SimulationContext context, 
        EnhancedFlowNode node
    ) {
        // Check if node has AI enhancement flags
        if (hasAiEnhancement(node)) {
            return context.getSimulationMode() == SimulationMode.DYNAMIC ? 
                SimulationMode.DYNAMIC : SimulationMode.HYBRID;
        }
        
        // Check if node has complex branching requiring dynamic generation
        if (hasComplexBranching(node)) {
            return SimulationMode.HYBRID;
        }
        
        // Default to predefined for simple nodes
        return SimulationMode.PREDEFINED;
    }
    
    /**
     * 🤖 Check if node has AI enhancement capabilities
     */
    private boolean hasAiEnhancement(EnhancedFlowNode node) {
        return node.getInteractionType() != null && 
               node.getInteractionType().isAIGenerated();
    }
    
    /**
     * 🌳 Check if node has complex branching logic
     */
    private boolean hasComplexBranching(EnhancedFlowNode node) {
        return node.getFlowRules() != null && 
               node.getFlowRules().size() > 0;
    }
    
    /**
     * 🆘 Generate fallback message when content generation fails
     */
    private Message generateFallbackMessage(SimulationContext context, Object node) {
        log.warn("🆘 Generating fallback message for node: {}", node);
        
        try {
            // Create a simple text message as fallback
            // In a real implementation, this would create a proper Message entity
            // For now, returning null to avoid compilation issues
            return null;
            
        } catch (Exception e) {
            log.error("❌ Error generating fallback message", e);
            return null;
        }
    }
    
    /**
     * 🎭 Generate character-appropriate content
     */
    public List<Message> generateCharacterContent(
        SimulationContext context,
        Object node,
        Long characterId
    ) {
        log.debug("🎭 Generating character-specific content for character: {}", characterId);
        
        // Add character-specific logic here
        // For now, delegate to main generation method
        return generateContentForSingleNode(context, node);
    }
    
    /**
     * 📊 Generate assessment or result content
     */
    public List<Message> generateAssessmentContent(
        SimulationContext context,
        Map<String, Double> hyperParameters
    ) {
        log.debug("📊 Generating assessment content based on hyperparameters");
        
        // For now, return empty list - will be implemented with AI integration
        // TODO: Implement assessment content generation using AI
        return List.of();
    }
    
    /**
     * 🎨 Batch content generation for multiple nodes
     */
    public List<Message> generateBatchContent(
        SimulationContext context,
        List<Object> nodes,
        Map<String, Object> generationOptions
    ) {
        log.debug("🎨 Batch generating content for {} nodes with options", nodes.size());
        
        // Add parallel processing for large batches
        if (nodes.size() > 10) {
            return generateContentForNodesParallel(context, nodes);
        } else {
            return generateContentForNodes(context, nodes);
        }
    }
    
    /**
     * ⚡ Parallel content generation for large node sets
     */
    private List<Message> generateContentForNodesParallel(
        SimulationContext context, 
        List<Object> nodes
    ) {
        log.debug("⚡ Using parallel processing for {} nodes", nodes.size());
        
        // For now, use sequential processing
        // In a full implementation, this would use CompletableFuture.allOf()
        return generateContentForNodes(context, nodes);
    }
} 