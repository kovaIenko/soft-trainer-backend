package com.backend.softtrainer.controllers;

import com.backend.softtrainer.services.notifications.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Health check controller for monitoring system components
 */
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final TelegramService telegramService;

    /**
     * General application health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthStatus = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "application", "soft-trainer-backend"
        );
        
        return ResponseEntity.ok(healthStatus);
    }

    /**
     * Telegram service specific health check
     */
    @GetMapping("/telegram")
    public ResponseEntity<Map<String, Object>> telegramHealth() {
        boolean isHealthy = telegramService.isHealthy();
        String status = isHealthy ? "UP" : "DOWN";
        
        Map<String, Object> telegramStatus = Map.of(
            "status", status,
            "service", "telegram",
            "timestamp", LocalDateTime.now(),
            "healthy", isHealthy
        );
        
        if (isHealthy) {
            log.debug("Telegram service health check: UP");
            return ResponseEntity.ok(telegramStatus);
        } else {
            log.warn("Telegram service health check: DOWN");
            return ResponseEntity.status(503).body(telegramStatus);
        }
    }

    /**
     * Test Telegram notification (only for testing purposes)
     * This endpoint should be secured or removed in production
     */
    @GetMapping("/telegram/test")
    public ResponseEntity<Map<String, Object>> testTelegramNotification() {
        try {
            String testMessage = "🔧 Health Check Test\n" +
                                "⏰ Time: " + LocalDateTime.now() + "\n" +
                                "✅ Telegram service is operational";
            
            telegramService.sendMessage(testMessage);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Test notification sent successfully",
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to send test Telegram notification", e);
            
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to send test notification: " + e.getMessage(),
                "timestamp", LocalDateTime.now()
            );
            
            return ResponseEntity.status(500).body(response);
        }
    }
}
