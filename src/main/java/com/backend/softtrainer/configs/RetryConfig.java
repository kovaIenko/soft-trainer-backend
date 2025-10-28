package com.backend.softtrainer.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Configuration class to enable Spring Retry functionality
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // Spring Retry configuration is enabled via @EnableRetry annotation
    // Retry behavior is configured via @Retryable annotations on methods
}
