package com.agentbanking.biller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.agentbanking.biller.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.agentbanking.biller.infrastructure.persistence.repository")
public class BillerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BillerServiceApplication.class, args);
    }
}
