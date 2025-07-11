package com.backend.softtrainer.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ⏱️ AI Agent Timeout and Circuit Breaker Configuration
 * 
 * Provides robust timeout handling and fault tolerance for AI agent service calls:
 * - Connection timeouts
 * - Read timeouts  
 * - Circuit breaker pattern
 * - Retry mechanisms
 * - Health monitoring
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai-agent.timeout")
@Data
public class AiAgentTimeoutConfig {

    // Timeout configurations (in milliseconds)
    private int connectionTimeout = 5000;  // 5 seconds to establish connection
    private int readTimeout = 30000;       // 30 seconds to read response
    private int totalTimeout = 35000;      // 35 seconds total timeout

    // Circuit breaker configurations
    private int failureThreshold = 5;      // Open circuit after 5 failures
    private long resetTimeoutMs = 60000;   // Try to close circuit after 1 minute
    private int maxRetries = 3;            // Maximum retry attempts

    // Health check configurations
    private int healthCheckIntervalMs = 30000;  // Health check every 30 seconds
    private int consecutiveSuccessToClose = 3;  // Successes needed to close circuit

    // RestTemplate configuration moved to AiAgentConfig.java to avoid duplicate bean definition

    /**
     * Circuit breaker state management
     */
    @Bean
    public AiAgentCircuitBreaker aiAgentCircuitBreaker() {
        return new AiAgentCircuitBreaker(this);
    }

    /**
     * Simple circuit breaker implementation for AI agent service
     */
    public static class AiAgentCircuitBreaker {
        
        public enum State {
            CLOSED,     // Normal operation
            OPEN,       // Circuit is open, failing fast
            HALF_OPEN   // Testing if service is back up
        }

        private final AiAgentTimeoutConfig config;
        private volatile State state = State.CLOSED;
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;

        public AiAgentCircuitBreaker(AiAgentTimeoutConfig config) {
            this.config = config;
        }

        /**
         * Checks if the circuit allows calls through
         */
        public boolean allowRequest() {
            if (state == State.CLOSED) {
                return true;
            }
            
            if (state == State.OPEN) {
                // Check if enough time has passed to try half-open
                if (System.currentTimeMillis() - lastFailureTime > config.getResetTimeoutMs()) {
                    state = State.HALF_OPEN;
                    successCount.set(0);
                    return true;
                }
                return false; // Circuit is open, fail fast
            }
            
            // HALF_OPEN state - allow limited requests
            return true;
        }

        /**
         * Records a successful call
         */
        public void recordSuccess() {
            failureCount.set(0);
            
            if (state == State.HALF_OPEN) {
                if (successCount.incrementAndGet() >= config.getConsecutiveSuccessToClose()) {
                    state = State.CLOSED;
                }
            }
        }

        /**
         * Records a failed call
         */
        public void recordFailure() {
            lastFailureTime = System.currentTimeMillis();
            
            if (failureCount.incrementAndGet() >= config.getFailureThreshold()) {
                state = State.OPEN;
                successCount.set(0);
            } else if (state == State.HALF_OPEN) {
                // If we fail in half-open, go back to open
                state = State.OPEN;
                successCount.set(0);
            }
        }

        /**
         * Gets current circuit state
         */
        public State getState() {
            return state;
        }

        /**
         * Gets current failure count
         */
        public int getFailureCount() {
            return failureCount.get();
        }

        /**
         * Gets current success count (relevant in HALF_OPEN state)
         */
        public int getSuccessCount() {
            return successCount.get();
        }

        /**
         * Manually resets the circuit breaker
         */
        public void reset() {
            state = State.CLOSED;
            failureCount.set(0);
            successCount.set(0);
            lastFailureTime = 0;
        }
    }
} 