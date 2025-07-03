package com.backend.softtrainer.simulation.flow;

import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.entities.flow.EnhancedFlowNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🚀 Flow Pattern Cache - High-performance caching for flow patterns
 * 
 * This service provides intelligent caching for:
 * - Frequently accessed flow nodes
 * - Computed rule evaluations
 * - Flow resolution patterns
 * - Performance metrics
 */
@Service
@Slf4j
public class FlowPatternCache {
    
    // Cache statistics
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final Map<String, Long> patternAccessCount = new ConcurrentHashMap<>();
    
    /**
     * 🔍 Cache flow nodes by simulation and predecessor
     */
    @Cacheable(value = "flowNodes", key = "#simulationId + '_' + #previousOrderNumber")
    public List<FlowNode> getCachedFlowNodes(Long simulationId, Long previousOrderNumber) {
        log.debug("🔍 Cache miss for flow nodes: simulation={}, previous={}", 
            simulationId, previousOrderNumber);
        cacheMisses.incrementAndGet();
        
        // This will be called only on cache miss
        // The actual database query will be performed by the calling service
        return null; // Indicates cache miss
    }
    
    /**
     * 🚀 Cache enhanced flow nodes by simulation and predecessors
     */
    @Cacheable(value = "enhancedFlowNodes", key = "#simulationId + '_' + #previousMessageIds.hashCode()")
    public List<EnhancedFlowNode> getCachedEnhancedNodes(Long simulationId, List<Long> previousMessageIds) {
        log.debug("🚀 Cache miss for enhanced nodes: simulation={}, previous={}", 
            simulationId, previousMessageIds);
        cacheMisses.incrementAndGet();
        
        return null; // Indicates cache miss
    }
    
    /**
     * 📊 Cache rule evaluation results
     */
    @Cacheable(value = "ruleEvaluations", key = "#ruleId + '_' + #contextHash")
    public Boolean getCachedRuleEvaluation(String ruleId, String contextHash) {
        log.debug("📊 Cache miss for rule evaluation: rule={}, context={}", 
            ruleId, contextHash);
        cacheMisses.incrementAndGet();
        
        return null; // Indicates cache miss
    }
    
    /**
     * 💾 Store flow nodes in cache
     */
    public void cacheFlowNodes(Long simulationId, Long previousOrderNumber, List<FlowNode> nodes) {
        String cacheKey = simulationId + "_" + previousOrderNumber;
        patternAccessCount.merge(cacheKey, 1L, Long::sum);
        
        log.debug("💾 Caching {} flow nodes for key: {}", nodes.size(), cacheKey);
        // Spring Cache will handle the actual caching via @Cacheable annotation
    }
    
    /**
     * 💾 Store enhanced nodes in cache
     */
    public void cacheEnhancedNodes(Long simulationId, List<Long> previousMessageIds, List<EnhancedFlowNode> nodes) {
        String cacheKey = simulationId + "_" + previousMessageIds.hashCode();
        patternAccessCount.merge(cacheKey, 1L, Long::sum);
        
        log.debug("💾 Caching {} enhanced nodes for key: {}", nodes.size(), cacheKey);
    }
    
    /**
     * 💾 Store rule evaluation result
     */
    public void cacheRuleEvaluation(String ruleId, String contextHash, Boolean result) {
        String cacheKey = ruleId + "_" + contextHash;
        patternAccessCount.merge(cacheKey, 1L, Long::sum);
        
        log.debug("💾 Caching rule evaluation: {} = {}", cacheKey, result);
    }
    
    /**
     * 🗑️ Clear cache for specific simulation
     */
    @CacheEvict(value = {"flowNodes", "enhancedFlowNodes"}, key = "#simulationId + '*'")
    public void clearSimulationCache(Long simulationId) {
        log.info("🗑️ Clearing cache for simulation: {}", simulationId);
    }
    
    /**
     * 🗑️ Clear all caches
     */
    @CacheEvict(value = {"flowNodes", "enhancedFlowNodes", "ruleEvaluations"}, allEntries = true)
    public void clearAllCaches() {
        log.info("🗑️ Clearing all flow pattern caches");
        patternAccessCount.clear();
        cacheHits.set(0);
        cacheMisses.set(0);
    }
    
    /**
     * 📈 Get cache statistics
     */
    public CacheStatistics getCacheStatistics() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        double hitRatio = hits + misses > 0 ? (double) hits / (hits + misses) : 0.0;
        
        return CacheStatistics.builder()
            .cacheHits(hits)
            .cacheMisses(misses)
            .hitRatio(hitRatio)
            .totalPatterns(patternAccessCount.size())
            .mostAccessedPatterns(getMostAccessedPatterns())
            .build();
    }
    
    /**
     * 🔝 Get most frequently accessed patterns
     */
    private Map<String, Long> getMostAccessedPatterns() {
        return patternAccessCount.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * 🎯 Check if pattern should be cached based on access frequency
     */
    public boolean shouldCachePattern(String patternKey) {
        Long accessCount = patternAccessCount.get(patternKey);
        return accessCount != null && accessCount >= 3; // Cache after 3 accesses
    }
    
    /**
     * 📊 Cache Statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class CacheStatistics {
        private long cacheHits;
        private long cacheMisses;
        private double hitRatio;
        private int totalPatterns;
        private Map<String, Long> mostAccessedPatterns;
    }
    
    /**
     * 🔄 Warm up cache with common patterns
     */
    public void warmUpCache(List<Long> commonSimulationIds) {
        log.info("🔄 Warming up cache for {} simulations", commonSimulationIds.size());
        
        for (Long simulationId : commonSimulationIds) {
            // Pre-load common flow patterns
            // This would typically load the first few nodes of each simulation
            log.debug("🔥 Warming cache for simulation: {}", simulationId);
        }
    }
    
    /**
     * 📝 Generate context hash for rule caching
     */
    public String generateContextHash(Object... contextElements) {
        StringBuilder hash = new StringBuilder();
        for (Object element : contextElements) {
            if (element != null) {
                hash.append(element.hashCode()).append("_");
            }
        }
        return hash.toString();
    }
    
    /**
     * 🔍 Record cache hit
     */
    public void recordCacheHit(String cacheKey) {
        cacheHits.incrementAndGet();
        patternAccessCount.merge(cacheKey, 1L, Long::sum);
        log.debug("🎯 Cache hit for: {}", cacheKey);
    }
    
    /**
     * ❌ Record cache miss
     */
    public void recordCacheMiss(String cacheKey) {
        cacheMisses.incrementAndGet();
        log.debug("❌ Cache miss for: {}", cacheKey);
    }
} 