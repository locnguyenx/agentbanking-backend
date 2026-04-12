package com.agentbanking.orchestrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.agentbanking.orchestrator")
@EnableFeignClients
@ComponentScan(basePackages = {"com.agentbanking.orchestrator", "com.agentbanking.common"})
public class OrchestratorServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorServiceApplication.class);

    public static void main(String[] args) {
        log.info("======================================================");
        log.info("ORCHESTRATOR SERVICE STARTING");
        log.info("Using Temporal Spring Boot Auto-Configuration");
        log.info("======================================================");
        SpringApplication.run(OrchestratorServiceApplication.class, args);
    }
}