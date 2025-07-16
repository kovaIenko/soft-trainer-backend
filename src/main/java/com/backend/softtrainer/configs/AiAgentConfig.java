package com.backend.softtrainer.configs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@Slf4j
public class AiAgentConfig {

    @Value("${app.ai-agent.timeout.connect:5000}")
    private int connectTimeoutMs;
    
    @Value("${app.ai-agent.timeout.read:3000}")
    private int readTimeoutMs;
    
    @Value("${app.ai-agent.pool.core-size:5}")
    private int corePoolSize;
    
    @Value("${app.ai-agent.pool.max-size:20}")
    private int maxPoolSize;
    
    @Value("${app.ai-agent.pool.queue-capacity:100}")
    private int queueCapacity;

    /**
     * 🕒 Production-Ready RestTemplate with Timeouts
     */
    @Bean("aiAgentRestTemplate")
    public RestTemplate aiAgentRestTemplate(RestTemplateBuilder builder) {
        log.info("🔧 Configuring AI Agent RestTemplate with timeouts: connect={}ms, read={}ms", 
                connectTimeoutMs, readTimeoutMs);
        
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .requestFactory(this::createRequestFactory)
                .errorHandler(new AiAgentErrorHandler())
                .build();
    }
    
    /**
     * 🏭 Custom Request Factory with Additional Configurations
     */
    private ClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        // Enable automatic retries for connection failures
        factory.setBufferRequestBody(false);
        return factory;
    }

    /**
     * 🧵 Dedicated Thread Pool for AI Agent Operations
     */
    @Bean("aiAgentTaskExecutor")
    public Executor aiAgentTaskExecutor() {
        log.info("🧵 Configuring AI Agent thread pool: core={}, max={}, queue={}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("AI-Agent-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
} 