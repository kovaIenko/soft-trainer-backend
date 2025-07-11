package com.backend.softtrainer.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 🚨 Production-Ready AI Agent Error Handler
 * 
 * Handles various failure scenarios:
 * - Network timeouts
 * - Rate limiting (429)
 * - Server errors (5xx)
 * - Circuit breaker pattern
 */
@Slf4j
public class AiAgentErrorHandler implements ResponseErrorHandler {
    
    private static final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private static final AtomicLong lastFailureTime = new AtomicLong(0);
    private static final int CIRCUIT_BREAKER_THRESHOLD = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 60000; // 1 minute
    
    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        HttpStatus httpStatus = HttpStatus.resolve(response.getStatusCode().value());
        if (httpStatus == null) return true;
        HttpStatus.Series series = httpStatus.series();
        return series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR;
    }
    
    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        HttpStatus statusCode = HttpStatus.resolve(response.getStatusCode().value());
        if (statusCode == null) statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
        String responseBody = readResponseBody(response);
        
        // Track consecutive failures for circuit breaker
        int failures = consecutiveFailures.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());
        
        log.error("🚨 AI Agent API error: status={}, body={}, consecutiveFailures={}", 
                statusCode, responseBody, failures);
        
        // Handle specific error cases
        switch (statusCode) {
            case TOO_MANY_REQUESTS:
                handleRateLimitError(responseBody, failures);
                break;
            case INTERNAL_SERVER_ERROR:
            case BAD_GATEWAY:
            case SERVICE_UNAVAILABLE:
            case GATEWAY_TIMEOUT:
                handleServerError(statusCode, responseBody, failures);
                break;
            case BAD_REQUEST:
                handleClientError(statusCode, responseBody);
                break;
            default:
                handleGenericError(statusCode, responseBody, failures);
        }
        
        // Check circuit breaker
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            log.error("🔥 CIRCUIT BREAKER ACTIVATED: {} consecutive failures. AI Agent will be bypassed for {} ms", 
                    failures, CIRCUIT_BREAKER_TIMEOUT_MS);
        }
    }
    
    /**
     * 🚦 Check if circuit breaker is active
     */
    public static boolean isCircuitBreakerActive() {
        if (consecutiveFailures.get() < CIRCUIT_BREAKER_THRESHOLD) {
            return false;
        }
        
        long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
        if (timeSinceLastFailure > CIRCUIT_BREAKER_TIMEOUT_MS) {
            // Reset circuit breaker
            consecutiveFailures.set(0);
            log.info("🔄 Circuit breaker reset - retrying AI Agent");
            return false;
        }
        
        return true;
    }
    
    /**
     * 🎯 Reset circuit breaker on successful response
     */
    public static void recordSuccess() {
        if (consecutiveFailures.get() > 0) {
            log.info("✅ AI Agent recovered - resetting failure count from {}", consecutiveFailures.get());
            consecutiveFailures.set(0);
        }
    }
    
    private void handleRateLimitError(String responseBody, int failures) {
        log.warn("⏰ AI Agent rate limit exceeded. Failure #{}: {}", failures, responseBody);
        throw new AiAgentRateLimitException("AI Agent rate limit exceeded: " + responseBody);
    }
    
    private void handleServerError(HttpStatus statusCode, String responseBody, int failures) {
        log.error("🔥 AI Agent server error {}: {}. Failure #{}", statusCode, responseBody, failures);
        throw new AiAgentServerException("AI Agent server error: " + statusCode + " - " + responseBody);
    }
    
    private void handleClientError(HttpStatus statusCode, String responseBody) {
        log.error("❌ AI Agent client error {}: {}", statusCode, responseBody);
        throw new AiAgentClientException("AI Agent client error: " + statusCode + " - " + responseBody);
    }
    
    private void handleGenericError(HttpStatus statusCode, String responseBody, int failures) {
        log.error("⚠️ AI Agent generic error {}: {}. Failure #{}", statusCode, responseBody, failures);
        throw new AiAgentException("AI Agent error: " + statusCode + " - " + responseBody);
    }
    
    private String readResponseBody(ClientHttpResponse response) {
        try (InputStream inputStream = response.getBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to read error response body: {}", e.getMessage());
            return "Unable to read response body";
        }
    }
    
    // Custom exception classes for different error types
    public static class AiAgentException extends RuntimeException {
        public AiAgentException(String message) { super(message); }
    }
    
    public static class AiAgentRateLimitException extends AiAgentException {
        public AiAgentRateLimitException(String message) { super(message); }
    }
    
    public static class AiAgentServerException extends AiAgentException {
        public AiAgentServerException(String message) { super(message); }
    }
    
    public static class AiAgentClientException extends AiAgentException {
        public AiAgentClientException(String message) { super(message); }
    }
} 