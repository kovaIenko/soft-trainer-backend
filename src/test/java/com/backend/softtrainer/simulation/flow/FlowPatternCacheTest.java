package com.backend.softtrainer.simulation.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FlowPatternCache Tests")
class FlowPatternCacheTest {
    
    private FlowPatternCache cache;
    
    @BeforeEach
    void setUp() {
        cache = new FlowPatternCache();
    }
    
    @Test
    @DisplayName("Should generate consistent context hash")
    void shouldGenerateConsistentContextHash() {
        String hash1 = cache.generateContextHash("test", 123, true);
        String hash2 = cache.generateContextHash("test", 123, true);
        String hash3 = cache.generateContextHash("test", 124, true);
        
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
    }
    
    @Test
    @DisplayName("Should record cache hits and misses")
    void shouldRecordCacheHitsAndMisses() {
        cache.recordCacheHit("test_key_1");
        cache.recordCacheHit("test_key_1");
        cache.recordCacheMiss("test_key_2");
        
        FlowPatternCache.CacheStatistics stats = cache.getCacheStatistics();
        
        assertEquals(2, stats.getCacheHits());
        assertEquals(1, stats.getCacheMisses());
        assertTrue(stats.getHitRatio() > 0.6); // 2/3 = 0.67
    }
    
    @Test
    @DisplayName("Should track pattern access frequency")
    void shouldTrackPatternAccessFrequency() {
        String patternKey = "simulation_1_previous_5";
        
        assertFalse(cache.shouldCachePattern(patternKey));
        
        // Simulate multiple accesses
        cache.recordCacheHit(patternKey);
        cache.recordCacheHit(patternKey);
        cache.recordCacheHit(patternKey);
        
        assertTrue(cache.shouldCachePattern(patternKey));
    }
    
    @Test
    @DisplayName("Should handle cache warming")
    void shouldHandleCacheWarming() {
        List<Long> simulationIds = Arrays.asList(1L, 2L, 3L);
        
        assertDoesNotThrow(() -> cache.warmUpCache(simulationIds));
    }
    
    @Test
    @DisplayName("Should clear caches without errors")
    void shouldClearCaches() {
        cache.recordCacheHit("test_key");
        
        assertDoesNotThrow(() -> {
            cache.clearSimulationCache(1L);
            cache.clearAllCaches();
        });
        
        FlowPatternCache.CacheStatistics stats = cache.getCacheStatistics();
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
    }
    
    @Test
    @DisplayName("Should provide accurate statistics")
    void shouldProvideAccurateStatistics() {
        cache.recordCacheHit("pattern_1");
        cache.recordCacheHit("pattern_1");
        cache.recordCacheHit("pattern_2");
        cache.recordCacheMiss("pattern_3");
        
        FlowPatternCache.CacheStatistics stats = cache.getCacheStatistics();
        
        assertEquals(3, stats.getCacheHits());
        assertEquals(1, stats.getCacheMisses());
        assertEquals(0.75, stats.getHitRatio(), 0.01);
        assertTrue(stats.getTotalPatterns() >= 2);
        assertNotNull(stats.getMostAccessedPatterns());
    }
} 