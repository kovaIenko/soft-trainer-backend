package com.backend.softtrainer.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for logging chat sessions and AI interactions to dedicated log files
 * Provides comprehensive tracking of chat flows for debugging and analysis
 */
@Service
@Slf4j
public class ChatSessionLogger {

    private static final String LOGS_DIR = "logs";
    private static final String CHAT_LOG_FILE = "generated_chats.log";
    private static final String AI_INTERACTION_LOG_FILE = "ai_interactions.log";
    private static final String ERROR_LOG_FILE = "chat_errors.log";
    
    private final ObjectMapper objectMapper;
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public ChatSessionLogger() {
        // Initialize object mapper for JSON serialization
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        
        // Ensure logs directory exists
        initializeLogDirectory();
    }

    /**
     * 🎬 Log chat session creation/initialization
     */
    public void logChatCreation(Long chatId, Long simulationId, String simulationType, 
                               String userId, int messageCount, boolean success, String error) {
        try {
            String timestamp = LocalDateTime.now().format(timestampFormatter);
            
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("\n").append("=".repeat(80)).append("\n");
            logEntry.append("CHAT CREATION: ").append(chatId).append("\n");
            logEntry.append("Simulation ID: ").append(simulationId).append("\n");
            logEntry.append("Simulation Type: ").append(simulationType).append("\n");
            logEntry.append("User ID: ").append(userId).append("\n");
            logEntry.append("Timestamp: ").append(timestamp).append("\n");
            logEntry.append("Success: ").append(success).append("\n");
            if (error != null) {
                logEntry.append("Error: ").append(error).append("\n");
            }
            logEntry.append("Initial Messages Count: ").append(messageCount).append("\n");
            logEntry.append("=".repeat(80)).append("\n\n");

            writeToLogFile(CHAT_LOG_FILE, logEntry.toString());
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to log chat creation for chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 💬 Log user message and AI response interaction
     */
    public void logMessageInteraction(Long chatId, String userMessageType, String userContent, 
                                    List<String> aiMessages, boolean success, String error,
                                    long processingTimeMs) {
        try {
            String timestamp = LocalDateTime.now().format(timestampFormatter);
            
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("[").append(timestamp).append("] INTERACTION | Chat: ").append(chatId);
            logEntry.append(" | User Type: ").append(userMessageType);
            if (userContent != null && !userContent.trim().isEmpty()) {
                logEntry.append(" | User Content: ").append(truncateContent(userContent, 100));
            }
            logEntry.append(" | Success: ").append(success);
            logEntry.append(" | Processing Time: ").append(processingTimeMs).append("ms");
            
            if (error != null) {
                logEntry.append(" | Error: ").append(error);
            }
            
            logEntry.append(" | AI Messages: ").append(aiMessages != null ? aiMessages.size() : 0);
            logEntry.append("\n");

            // Add AI messages details
            if (aiMessages != null && !aiMessages.isEmpty()) {
                for (int i = 0; i < aiMessages.size(); i++) {
                    logEntry.append("  ├─ AI Message ").append(i + 1).append(": ")
                            .append(truncateContent(aiMessages.get(i), 150)).append("\n");
                }
            }
            
            logEntry.append("\n");

            writeToLogFile(CHAT_LOG_FILE, logEntry.toString());
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to log message interaction for chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 🤖 Log AI agent interactions (requests/responses)
     */
    public void logAiAgentInteraction(Long chatId, String endpoint, Map<String, Object> request, 
                                    Map<String, Object> response, boolean success, String error,
                                    long responseTimeMs) {
        try {
            String timestamp = LocalDateTime.now().format(timestampFormatter);
            
            Map<String, Object> logData = new HashMap<>();
            logData.put("timestamp", timestamp);
            logData.put("chatId", chatId);
            logData.put("endpoint", endpoint);
            logData.put("success", success);
            logData.put("responseTimeMs", responseTimeMs);
            
            if (error != null) {
                logData.put("error", error);
            }
            
            // Include request/response data (truncated for large payloads)
            if (request != null) {
                logData.put("request", truncateJsonObject(request));
            }
            
            if (response != null) {
                logData.put("response", truncateJsonObject(response));
            }

            String logEntry = objectMapper.writeValueAsString(logData) + "\n";
            writeToLogFile(AI_INTERACTION_LOG_FILE, logEntry);
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to log AI agent interaction for chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * ❌ Log chat errors and exceptions
     */
    public void logChatError(Long chatId, String operation, String errorType, String errorMessage, 
                           String stackTrace) {
        try {
            String timestamp = LocalDateTime.now().format(timestampFormatter);
            
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("timestamp", timestamp);
            errorData.put("chatId", chatId);
            errorData.put("operation", operation);
            errorData.put("errorType", errorType);
            errorData.put("errorMessage", errorMessage);
            
            if (stackTrace != null) {
                errorData.put("stackTrace", truncateContent(stackTrace, 2000));
            }

            String logEntry = objectMapper.writeValueAsString(errorData) + "\n";
            writeToLogFile(ERROR_LOG_FILE, logEntry);
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to log chat error for chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * 🏁 Log chat completion
     */
    public void logChatCompletion(Long chatId, int totalMessages, long totalDurationMs, 
                                String completionReason) {
        try {
            String timestamp = LocalDateTime.now().format(timestampFormatter);
            
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("[").append(timestamp).append("] CHAT COMPLETION | Chat: ").append(chatId);
            logEntry.append(" | Total Messages: ").append(totalMessages);
            logEntry.append(" | Duration: ").append(totalDurationMs).append("ms");
            logEntry.append(" | Reason: ").append(completionReason);
            logEntry.append("\n").append("-".repeat(80)).append("\n\n");

            writeToLogFile(CHAT_LOG_FILE, logEntry.toString());
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to log chat completion for chat {}: {}", chatId, e.getMessage());
        }
    }

    // Helper methods

    private void initializeLogDirectory() {
        try {
            Path logsPath = Paths.get(LOGS_DIR);
            if (!Files.exists(logsPath)) {
                Files.createDirectories(logsPath);
                log.info("📁 Created logs directory: {}", logsPath.toAbsolutePath());
            }
            
            // Create log files if they don't exist
            createLogFileIfNotExists(CHAT_LOG_FILE);
            createLogFileIfNotExists(AI_INTERACTION_LOG_FILE);
            createLogFileIfNotExists(ERROR_LOG_FILE);
            
        } catch (IOException e) {
            log.error("❌ Failed to initialize log directory: {}", e.getMessage());
        }
    }

    private void createLogFileIfNotExists(String fileName) throws IOException {
        Path logFile = Paths.get(LOGS_DIR, fileName);
        if (!Files.exists(logFile)) {
            Files.createFile(logFile);
            log.info("📄 Created log file: {}", logFile.toAbsolutePath());
        }
    }

    private void writeToLogFile(String fileName, String content) {
        try {
            Path logFile = Paths.get(LOGS_DIR, fileName);
            Files.write(logFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("❌ Failed to write to log file {}: {}", fileName, e.getMessage());
        }
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "null";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private Map<String, Object> truncateJsonObject(Map<String, Object> obj) {
        // For large objects, we might want to truncate or summarize
        // For now, return as-is but could implement size-based truncation
        return obj;
    }
} 