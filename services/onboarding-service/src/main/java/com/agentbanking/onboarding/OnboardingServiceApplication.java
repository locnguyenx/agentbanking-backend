package com.agentbanking.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.agentbanking.onboarding.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.agentbanking.onboarding.infrastructure.persistence.repository")
@ComponentScan(basePackages = {"com.agentbanking.onboarding", "com.agentbanking.common"})
public class OnboardingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OnboardingServiceApplication.class, args);
    }
}
