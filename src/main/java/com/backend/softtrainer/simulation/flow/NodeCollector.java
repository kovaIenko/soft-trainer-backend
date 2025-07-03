package com.backend.softtrainer.simulation.flow;

import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.simulation.context.SimulationContext;
import com.backend.softtrainer.services.FlowService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 📦 Node Collector - Retrieves initial and subsequent nodes for simulation flow
 */
@Service
@AllArgsConstructor
@Slf4j
public class NodeCollector {
    
    private final FlowService flowService;
    
    /**
     * 🎬 Get initial nodes for simulation startup
     */
    public List<Object> getInitialNodes(SimulationContext context) {
        log.debug("🎬 Collecting initial nodes for simulation: {}", context.getSimulationId());
        
        try {
            // Get the first actionable flow nodes
            List<FlowNode> initialFlowNodes = flowService.getFirstFlowNodesUntilActionable(
                context.getSimulationId()
            );
            
            log.debug("✅ Found {} initial flow nodes", initialFlowNodes.size());
            
            // Convert to generic Object list for unified handling
            return initialFlowNodes.stream()
                .map(node -> (Object) node)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("❌ Error collecting initial nodes: {}", e.getMessage());
            return List.of(); // Return empty list as fallback
        }
    }
} 