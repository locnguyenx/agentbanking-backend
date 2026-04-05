package com.agentbanking.audit.config;

import com.agentbanking.audit.service.HealthAggregationService;
import com.agentbanking.audit.service.MetricsAggregationService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

@Configuration
public class ServiceRegistryConfig {

    @Bean
    @ConfigurationProperties(prefix = "admin")
    public AdminServicesConfig adminServicesConfig() {
        return new AdminServicesConfig();
    }

    @Bean
    public HealthAggregationService healthAggregationService(AdminServicesConfig config) {
        return new HealthAggregationService(config.getServices());
    }

    @Bean
    public MetricsAggregationService metricsAggregationService(AdminServicesConfig config) {
        return new MetricsAggregationService(config.getServices());
    }

    public static class AdminServicesConfig {
        private Map<String, String> services = Map.of();
        public Map<String, String> getServices() { return services; }
        public void setServices(Map<String, String> services) { this.services = services; }
    }
}
