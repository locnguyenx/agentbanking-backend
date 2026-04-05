package com.agentbanking.audit.infrastructure.web;

import com.agentbanking.audit.service.MetricsAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class MetricsAggregationController {

    private final MetricsAggregationService metricsAggregationService;

    public MetricsAggregationController(MetricsAggregationService metricsAggregationService) {
        this.metricsAggregationService = metricsAggregationService;
    }

    @GetMapping("/metrics/{service}")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable String service) {
        return ResponseEntity.ok(metricsAggregationService.getMetrics(service));
    }
}
