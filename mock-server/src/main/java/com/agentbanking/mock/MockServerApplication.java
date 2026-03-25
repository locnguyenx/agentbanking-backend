package com.agentbanking.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.agentbanking.mock.config.MockConfig;

@SpringBootApplication
@EnableConfigurationProperties(MockConfig.class)
public class MockServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockServerApplication.class, args);
    }
}
