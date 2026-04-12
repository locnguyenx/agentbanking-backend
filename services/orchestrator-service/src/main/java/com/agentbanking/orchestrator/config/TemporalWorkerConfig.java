package com.agentbanking.orchestrator.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {
    // Temporal worker configuration is now handled by Spring Boot auto-configuration
    // Activities and workflows are auto-discovered via @ActivityImpl and @WorkflowImpl annotations
}