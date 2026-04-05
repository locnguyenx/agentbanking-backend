package com.agentbanking.audit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    // Spring Cloud Stream handles consumer binding automatically.
    // This class exists to enable Kafka annotation processing if needed.
}
