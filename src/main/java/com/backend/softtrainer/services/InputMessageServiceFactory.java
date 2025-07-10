package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.repositories.ChatRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 🏭 Input Message Service Factory
 * 
 * Routes message processing between legacy show_predicate simulations and 
 * modern flow_rules simulations. This factory provides a clean abstraction
 * that allows both systems to coexist and be used transparently.
 * 
 * Key Features:
 * ✅ Automatic detection of simulation type
 * ✅ Seamless routing between legacy and modern services
 * ✅ Backward compatibility with all existing simulations
 * ✅ Future-proof design for gradual migration
 * 
 * Routing Logic:
 * - Modern: FlowNodes with flow_rules → ModernInputMessageService
 * - Legacy: FlowNodes with show_predicate → LegacyInputMessageService
 * - Fallback: Unknown/mixed → LegacyInputMessageService (safe default)
 */
@Service
@AllArgsConstructor
@Slf4j
public class InputMessageServiceFactory {

    private final InputMessageService originalInputMessageService; // Use original for legacy cases for now
    private final ModernInputMessageService modernService;
    private final ChatRepository chatRepository;

    /**
     * 🎯 Get the appropriate service for a chat
     */
    public InputMessageServiceInterface getServiceForChat(Long chatId) {
        try {
            Optional<Chat> chatOpt = chatRepository.findByIdWithMessages(chatId);
            if (chatOpt.isEmpty()) {
                log.warn("⚠️ Chat {} not found, defaulting to original service", chatId);
                return originalInputMessageService;
            }

            Chat chat = chatOpt.get();
            Simulation simulation = chat.getSimulation();
            
            if (simulation == null || simulation.getNodes() == null) {
                log.warn("⚠️ No simulation/nodes found for chat {}, defaulting to original service", chatId);
                return originalInputMessageService;
            }

            List<FlowNode> flowNodes = simulation.getNodes();
            return determineService(flowNodes);
            
        } catch (Exception e) {
            log.error("❌ Error determining service for chat {}: {}", chatId, e.getMessage());
            log.debug("Full error details:", e);
            return originalInputMessageService; // Safe fallback
        }
    }

    /**
     * 🎯 Get the appropriate service for a simulation
     */
    public InputMessageServiceInterface getServiceForSimulation(Simulation simulation) {
        try {
            if (simulation == null || simulation.getNodes() == null) {
                log.warn("⚠️ No simulation/nodes provided, defaulting to original service");
                return originalInputMessageService;
            }

            List<FlowNode> flowNodes = simulation.getNodes();
            return determineService(flowNodes);
            
        } catch (Exception e) {
            log.error("❌ Error determining service for simulation {}: {}", 
                    simulation != null ? simulation.getId() : "null", e.getMessage());
            log.debug("Full error details:", e);
            return originalInputMessageService; // Safe fallback
        }
    }

    /**
     * 🎯 Get the appropriate service for flow nodes
     */
    public InputMessageServiceInterface getServiceForFlowNodes(List<FlowNode> flowNodes) {
        return determineService(flowNodes);
    }

    /**
     * 🔍 Determine which service to use based on FlowNode characteristics
     */
    private InputMessageServiceInterface determineService(List<FlowNode> flowNodes) {
        if (flowNodes == null || flowNodes.isEmpty()) {
            log.debug("📄 Empty flow nodes, using original service");
            return originalInputMessageService;
        }

        // Check for modern flow_rules
        boolean hasModernRules = flowNodes.stream()
                .anyMatch(node -> node.getFlowRules() != null && !node.getFlowRules().isEmpty());

        // Check for legacy show_predicate
        boolean hasLegacyPredicates = flowNodes.stream()
                .anyMatch(node -> node.getShowPredicate() != null && !node.getShowPredicate().isEmpty());

        if (hasModernRules && !hasLegacyPredicates) {
            log.debug("🚀 Pure modern simulation detected (flow_rules only)");
            return modernService;
        } else if (hasLegacyPredicates && !hasModernRules) {
            log.debug("🏛️ Pure legacy simulation detected (show_predicate only)");
            return originalInputMessageService;
        } else if (hasModernRules && hasLegacyPredicates) {
            log.debug("🔄 Hybrid simulation detected (both flow_rules and show_predicate), using original service");
            return originalInputMessageService; // Original service can handle both
        } else {
            log.debug("❓ No specific rules detected, defaulting to original service");
            return originalInputMessageService; // Safe default
        }
    }

    /**
     * 📊 Get simulation type information for debugging
     */
    public SimulationTypeInfo getSimulationTypeInfo(List<FlowNode> flowNodes) {
        if (flowNodes == null || flowNodes.isEmpty()) {
            return new SimulationTypeInfo("EMPTY", "No flow nodes provided", originalInputMessageService.getClass().getSimpleName());
        }

        long modernCount = flowNodes.stream()
                .filter(node -> node.getFlowRules() != null && !node.getFlowRules().isEmpty())
                .count();

        long legacyCount = flowNodes.stream()
                .filter(node -> node.getShowPredicate() != null && !node.getShowPredicate().isEmpty())
                .count();

        String type;
        String description;
        if (modernCount > 0 && legacyCount == 0) {
            type = "MODERN";
            description = String.format("Pure modern simulation (%d nodes with flow_rules)", modernCount);
        } else if (legacyCount > 0 && modernCount == 0) {
            type = "LEGACY";
            description = String.format("Pure legacy simulation (%d nodes with show_predicate)", legacyCount);
        } else if (modernCount > 0 && legacyCount > 0) {
            type = "HYBRID";
            description = String.format("Hybrid simulation (%d modern, %d legacy)", modernCount, legacyCount);
        } else {
            type = "UNKNOWN";
            description = "No recognizable flow rules detected";
        }

        InputMessageServiceInterface service = determineService(flowNodes);
        return new SimulationTypeInfo(type, description, service.getClass().getSimpleName());
    }

    /**
     * 📋 Simulation type information for debugging
     */
    public static class SimulationTypeInfo {
        public final String type;
        public final String description;
        public final String serviceUsed;

        public SimulationTypeInfo(String type, String description, String serviceUsed) {
            this.type = type;
            this.description = description;
            this.serviceUsed = serviceUsed;
        }

        @Override
        public String toString() {
            return String.format("SimulationTypeInfo{type='%s', description='%s', serviceUsed='%s'}", 
                    type, description, serviceUsed);
        }
    }
} 