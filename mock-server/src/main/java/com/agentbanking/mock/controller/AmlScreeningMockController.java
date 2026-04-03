package com.agentbanking.mock.controller;

import com.agentbanking.mock.data.TestDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

enum AmlStatus {
    CLEAN, HIGH_RISK, MEDIUM_RISK, PENDING_REVIEW
}

/**
 * Mock controller for AML screening service.
 */
@RestController
@RequestMapping("/aml")
public class AmlScreeningMockController {

    private final TestDataService dataService;

    public AmlScreeningMockController(TestDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/screen")
    public Map<String, Object> screen(
            @RequestParam("mykadNumber") String mykadNumber,
            @RequestParam("fullName") String fullName) {
        // For simplicity, we ignore the fullName and just check if the mykad exists in our test data.
        // In a real scenario, we might want to match both.
        var citizen = dataService.findCitizenByMykad(mykadNumber);
        if (citizen == null) {
            // If not found, return a default status (CLEAN) or maybe NOT_FOUND?
            // The onboarding service expects a status in the result, and it will map to AmlStatus.
            // Let's return CLEAN for unknown citizens to allow the onboarding to proceed.
            return Map.of("status", "CLEAN");
        }
        // Return the amlStatus from the citizen data.
        return Map.of("status", citizen.amlStatus());
    }
}