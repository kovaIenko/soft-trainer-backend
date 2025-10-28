package com.backend.softtrainer.services.notifications;

import com.backend.softtrainer.configs.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class TelegramService extends TelegramLongPollingBot {

    private final TelegramProperties telegramProperties;
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile long lastFailureTime = 0;

    private TelegramBotsApi botsApi;
    
    // Circuit breaker settings
    private static final int MAX_CONSECUTIVE_FAILURES = 5;
    private static final long CIRCUIT_BREAKER_TIMEOUT = 30000; // 30 seconds

    public TelegramService(TelegramProperties telegramProperties) {
        this.telegramProperties = telegramProperties;
    }

    @PostConstruct
    public void initialize() {
        if (!telegramProperties.isEnabled()) {
            log.info("Telegram notifications are disabled");
            return;
        }

        try {
            log.info("Initializing Telegram bot service...");
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            isInitialized.set(true);
            log.info("Telegram bot service initialized successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to initialize Telegram bot service. Notifications will be disabled.", e);
            isInitialized.set(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Telegram bot service...");
        isShuttingDown.set(true);
        isInitialized.set(false);
    }

    @Override
    public void onUpdateReceived(org.telegram.telegrambots.meta.api.objects.Update update) {
        // Not needed for sending messages only
        log.debug("Received update from Telegram: {}", update.getUpdateId());
    }

    @Override
    public String getBotUsername() {
        return telegramProperties.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return telegramProperties.getBotToken();
    }

    /**
     * Sends a Telegram message asynchronously with circuit breaker and retry patterns
     * @param text Message text to send
     * @return CompletableFuture that completes when message is sent or fails
     */
    public CompletableFuture<Void> sendMessageAsync(String text) {
        return CompletableFuture.runAsync(() -> sendMessage(text));
    }

    /**
     * Sends a Telegram message synchronously with circuit breaker and retry patterns
     * @param text Message text to send
     */
    public void sendMessage(String text) {
        sendMessageWithRetry(text, 0);
    }

    /**
     * Internal method that implements retry logic manually
     * @param text Message text to send
     * @param attempt Current attempt number (0-based)
     */
    private void sendMessageWithRetry(String text, int attempt) {
        // Pre-flight checks
        if (!telegramProperties.isEnabled()) {
            log.debug("Telegram notifications are disabled, skipping message");
            return;
        }

        if (isShuttingDown.get()) {
            log.warn("Telegram service is shutting down, skipping message");
            return;
        }

        // Circuit breaker logic
        if (isCircuitOpen()) {
            log.warn("Telegram service circuit breaker is OPEN, skipping message");
            return;
        }

        if (!isInitialized.get()) {
            log.warn("Telegram bot service is not initialized, attempting to reinitialize...");
            initialize();
            if (!isInitialized.get()) {
                recordFailure();
                throw new TelegramServiceException("Telegram bot service is not available");
            }
        }

        // Validate input
        if (text == null || text.trim().isEmpty()) {
            log.warn("Empty message text provided to Telegram service");
            return;
        }

        // Truncate message if too long (Telegram limit is 4096 characters)
        if (text.length() > 4000) {
            text = text.substring(0, 3997) + "...";
            log.warn("Message truncated to fit Telegram limits");
        }

        try {
            log.debug("Sending Telegram message: {}", text.substring(0, Math.min(text.length(), 100)));

            SendMessage message = new SendMessage();
            message.setChatId(telegramProperties.getChatId());
            message.setText(text);
            message.setParseMode("HTML"); // Allow basic HTML formatting
            
            execute(message);
            log.debug("Telegram message sent successfully");
            recordSuccess();
            
        } catch (TelegramApiException e) {
            recordFailure();
            handleRetryOrFail(text, attempt, e, "Telegram API error");
        } catch (Exception e) {
            recordFailure();
            handleRetryOrFail(text, attempt, e, "Unexpected error");
        }
    }

    /**
     * Handles retry logic or final failure
     * @param text Original message text
     * @param attempt Current attempt number
     * @param exception The exception that occurred
     * @param errorType Type of error for logging
     */
    private void handleRetryOrFail(String text, int attempt, Exception exception, String errorType) {
        final int maxAttempts = 3;
        if (attempt < maxAttempts - 1) {
            int delayMs = (int) (5000 * Math.pow(2, attempt)); // Exponential backoff
            log.warn("{} occurred on attempt {} of {}, retrying in {}ms: {}", 
                    errorType, attempt + 1, maxAttempts, delayMs, exception.getMessage());
            
            try {
                Thread.sleep(delayMs);
                sendMessageWithRetry(text, attempt + 1);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Retry interrupted for Telegram message");
                throw new TelegramServiceException("Telegram message retry interrupted", ie);
            }
        } else {
            log.error("Failed to send Telegram message after {} attempts: {}", maxAttempts, exception.getMessage(), exception);
            throw new TelegramServiceException("Failed to send Telegram message after " + maxAttempts + " attempts", exception);
        }
    }

    /**
     * Fallback method for circuit breaker when Telegram service is unavailable
     * @param text Original message text
     * @param ex Exception that triggered the fallback
     */
    public void fallbackSendMessage(String text, Exception ex) {
        log.warn("Telegram service is unavailable, message will be logged instead: {}", 
                text.substring(0, Math.min(text.length(), 200)));
        log.warn("Telegram service failure reason: {}", ex.getMessage());
        
        // Could implement alternative notification methods here
        // For example: store in database, send to queue, email notification, etc.
    }

    /**
     * Circuit breaker logic - check if circuit is open
     * @return true if circuit is open (should not attempt calls)
     */
    private boolean isCircuitOpen() {
        int failures = consecutiveFailures.get();
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime;
            if (timeSinceLastFailure < CIRCUIT_BREAKER_TIMEOUT) {
                return true; // Circuit is still open
            } else {
                // Circuit should move to half-open state
                log.info("Telegram service circuit breaker moving to HALF-OPEN state");
                return false;
            }
        }
        return false;
    }

    /**
     * Record a successful operation
     */
    private void recordSuccess() {
        consecutiveFailures.set(0);
        lastFailureTime = 0;
    }

    /**
     * Record a failed operation
     */
    private void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();
        
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("Telegram service circuit breaker is now OPEN after {} consecutive failures", failures);
        }
    }

    /**
     * Health check method to verify if the service is operational
     * @return true if service is healthy, false otherwise
     */
    public boolean isHealthy() {
        return telegramProperties.isEnabled() && 
               isInitialized.get() && 
               !isShuttingDown.get() &&
               botsApi != null &&
               !isCircuitOpen();
    }

    /**
     * Custom exception class for Telegram service errors
     */
    public static class TelegramServiceException extends RuntimeException {
        public TelegramServiceException(String message) {
            super(message);
        }
        
        public TelegramServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
