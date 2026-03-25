package com.agentbanking.onboarding.infrastructure.web;

import com.agentbanking.onboarding.domain.model.*;
import com.agentbanking.onboarding.domain.service.KycDecisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class OnboardingController {

    private final KycDecisionService kycDecisionService;

    public OnboardingController(KycDecisionService kycDecisionService) {
        this.kycDecisionService = kycDecisionService;
    }

    @PostMapping("/verify-mykad")
    public ResponseEntity<Map<String, Object>> verifyMyKad(@RequestBody Map<String, String> request) {
        String mykad = request.get("mykadNumber");
        
        // Validate MyKad format (12 digits)
        if (mykad == null || !mykad.matches("^\\d{12}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_INVALID_MYKAD_FORMAT", "message", "MyKad must be exactly 12 digits")
            ));
        }

        // Simulate JPN verification (would call JPN API in production)
        // For now, return a mock response
        UUID verificationId = UUID.randomUUID();
        return ResponseEntity.ok(Map.of(
            "verificationId", verificationId.toString(),
            "status", "FOUND",
            "fullName", "MOCK CUSTOMER",
            "dateOfBirth", "1990-01-01",
            "age", 35,
            "amlStatus", "CLEAN"
        ));
    }

    @PostMapping("/biometric")
    public ResponseEntity<Map<String, Object>> biometricMatch(@RequestBody Map<String, String> request) {
        String verificationId = request.get("verificationId");
        String biometricData = request.get("biometricData");

        if (verificationId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_MISSING_VERIFICATION_ID", "message", "verificationId is required")
            ));
        }

        // Simulate biometric match (would call HSM/match-on-card in production)
        BiometricResult match = BiometricResult.MATCH;
        AmlStatus amlStatus = AmlStatus.CLEAN;
        int age = 35;

        // Apply decision matrix
        KycStatus decision = kycDecisionService.decide(match, amlStatus, age);

        return ResponseEntity.ok(Map.of(
            "verificationId", verificationId,
            "status", decision.name(),
            "biometricMatch", match.name(),
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
