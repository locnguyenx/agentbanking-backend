package com.agentbanking.audit.infrastructure.web;

import com.agentbanking.audit.service.HealthAggregationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class HealthAggregationController {

    private final HealthAggregationService healthAggregationService;

    public HealthAggregationController(HealthAggregationService healthAggregationService) {
        this.healthAggregationService = healthAggregationService;
    }

    @GetMapping("/health/all")
    public ResponseEntity<Map<String, Object>> getAllHealth() {
        return ResponseEntity.ok(healthAggregationService.aggregateHealth());
    }
}
