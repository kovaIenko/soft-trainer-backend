package com.backend.softtrainer.simulation.flow;

import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.EnhancedFlowNode;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.services.FlowService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 🔍 Flow Resolver - Determines next nodes in simulation flow
 * 
 * Handles both legacy FlowNode navigation and modern EnhancedFlowNode resolution
 */
@Service
@AllArgsConstructor
@Slf4j
public class FlowResolver {
    
    private final FlowService flowService;
    private final RuleUnifier ruleUnifier;
    
    /**
     * 🎯 Main method to resolve next nodes based on current context
     */
    public List<Object> resolveNextNodes(SimulationContext context, Message userMessage) {
        log.debug("🔍 Resolving next nodes for chat: {}", context.getChatId());
        
        // Determine the previous order/message ID
        Long previousOrderNumber = determinePreviousOrder(context, userMessage);
        
        // Get candidate nodes from database
        List<Object> candidateNodes = getCandidateNodes(context, previousOrderNumber);
        
        // Filter nodes based on rules/predicates
        List<Object> validNodes = filterValidNodes(context, candidateNodes);
        
        log.debug("✅ Resolved {} valid nodes from {} candidates", 
            validNodes.size(), candidateNodes.size());
        
        return validNodes;
    }
    
    /**
     * 📊 Determine the previous order number for node resolution
     */
    private Long determinePreviousOrder(SimulationContext context, Message userMessage) {
        if (userMessage != null && userMessage.getFlowNode() != null) {
            return userMessage.getFlowNode().getOrderNumber();
        }
        
        // For initial flow, start from 0
        return 0L;
    }
    
    /**
     * 🗃️ Get candidate nodes from database
     */
    private List<Object> getCandidateNodes(SimulationContext context, Long previousOrderNumber) {
        // Get legacy FlowNodes
        List<FlowNode> legacyNodes = flowService.findAllBySimulationIdAndPreviousOrderNumber(
            context.getSimulationId(), 
            previousOrderNumber
        );
        
        // TODO: Add support for EnhancedFlowNode when implemented
        // List<EnhancedFlowNode> enhancedNodes = enhancedFlowService.findByPreviousMessageId(...)
        
        // For now, return legacy nodes cast to Object
        return legacyNodes.stream()
            .map(node -> (Object) node)
            .collect(Collectors.toList());
    }
    
    /**
     * ✅ Filter nodes based on their rules/predicates
     */
    private List<Object> filterValidNodes(SimulationContext context, List<Object> candidateNodes) {
        return candidateNodes.stream()
            .filter(node -> ruleUnifier.evaluateNode(context, node))
            .collect(Collectors.toList());
    }
} 