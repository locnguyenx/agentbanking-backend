package com.agentbanking.switchadapter.config;

import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public SwitchAdapterService switchAdapterService(SwitchTransactionRepository repository) {
        return new SwitchAdapterService(repository);
    }
}
