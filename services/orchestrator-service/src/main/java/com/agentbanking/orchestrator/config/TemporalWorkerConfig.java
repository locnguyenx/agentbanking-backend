package com.agentbanking.orchestrator.config;

import com.agentbanking.orchestrator.infrastructure.temporal.DeserializationErrorInterceptor;
import io.temporal.worker.WorkerFactoryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporal Worker Configuration.
 * Registers interceptors for fail-fast error handling.
 */
@Configuration
public class TemporalWorkerConfig {

    /**
     * Worker factory options with deserialization error interceptor.
     * Fails workflows fast on serialization errors instead of infinite retries.
     */
    @Bean
    public WorkerFactoryOptions workerFactoryOptions() {
        return WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new DeserializationErrorInterceptor())
                .build();
    }
}