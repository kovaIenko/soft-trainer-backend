package com.backend.softtrainer.simulation.engine;

import com.backend.softtrainer.simulation.DualModeSimulationRuntime.SimulationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 🏭 Simulation Engine Factory - Engine Creation and Management
 * 
 * Creates and manages the appropriate simulation engine based on the detected
 * simulation type. Provides caching and lifecycle management for engines.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationEngineFactory {
    
    private final LegacySimulationEngine legacyEngine;
    private final ModernSimulationEngine modernEngine;
    private final AiGeneratedSimulationEngine aiGeneratedEngine;
    
    // Engine cache for performance
    private final Map<SimulationType, BaseSimulationEngine> engineCache = new HashMap<>();
    
    /**
     * 🎯 Create Engine for Simulation Type
     */
    public BaseSimulationEngine createEngine(SimulationType simulationType) {
        log.debug("🏭 Creating engine for simulation type: {}", simulationType);
        
        // Use cache for performance
        if (engineCache.containsKey(simulationType)) {
            return engineCache.get(simulationType);
        }
        
        BaseSimulationEngine engine = switch (simulationType) {
            case LEGACY -> {
                log.debug("🏛️ Creating legacy simulation engine");
                yield legacyEngine;
            }
            case MODERN -> {
                log.debug("🚀 Creating modern simulation engine");
                yield modernEngine;
            }
            case HYBRID -> {
                log.debug("🔄 Creating hybrid simulation engine (using legacy for compatibility)");
                // For hybrid simulations, use legacy engine for maximum compatibility
                yield legacyEngine;
            }
            case AI_GENERATED -> {
                log.debug("🤖 Creating AI-generated simulation engine");
                yield aiGeneratedEngine;
            }
            case UNKNOWN -> {
                log.warn("⚠️ Unknown simulation type, defaulting to legacy engine");
                yield legacyEngine;
            }
        };
        
        // Cache the engine
        engineCache.put(simulationType, engine);
        
        log.info("✅ Created {} engine for type: {}", 
                engine.getClass().getSimpleName(), simulationType);
        
        return engine;
    }
    
    /**
     * 🔍 Get Engine Class Name
     */
    public String getEngineClass(SimulationType simulationType) {
        return switch (simulationType) {
            case LEGACY -> LegacySimulationEngine.class.getSimpleName();
            case MODERN -> ModernSimulationEngine.class.getSimpleName();
            case HYBRID -> LegacySimulationEngine.class.getSimpleName() + " (Hybrid Mode)";
            case AI_GENERATED -> AiGeneratedSimulationEngine.class.getSimpleName();
            case UNKNOWN -> LegacySimulationEngine.class.getSimpleName() + " (Fallback)";
        };
    }
    
    /**
     * 📊 Get All Engine Metrics
     */
    public Map<SimulationType, BaseSimulationEngine.EngineMetrics> getAllEngineMetrics() {
        Map<SimulationType, BaseSimulationEngine.EngineMetrics> metrics = new HashMap<>();
        
        for (Map.Entry<SimulationType, BaseSimulationEngine> entry : engineCache.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue().getMetrics());
        }
        
        return metrics;
    }
    
    /**
     * 🔄 Clear Engine Cache (for testing or configuration changes)
     */
    public void clearCache() {
        log.info("🔄 Clearing simulation engine cache");
        engineCache.clear();
    }
    
    /**
     * 📋 Get Factory Status
     */
    public FactoryStatus getFactoryStatus() {
        return FactoryStatus.builder()
                .cachedEngineCount(engineCache.size())
                .availableTypes(SimulationType.values())
                .defaultType(SimulationType.LEGACY)
                .build();
    }
    
    /**
     * 📊 Factory Status Information
     */
    @lombok.Builder
    @lombok.Data
    public static class FactoryStatus {
        private int cachedEngineCount;
        private SimulationType[] availableTypes;
        private SimulationType defaultType;
    }
} 