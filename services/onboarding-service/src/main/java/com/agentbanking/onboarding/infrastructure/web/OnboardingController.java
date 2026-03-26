package com.agentbanking.onboarding.infrastructure.web;

import com.agentbanking.onboarding.domain.model.*;
import com.agentbanking.onboarding.domain.service.KycDecisionService;
import com.agentbanking.onboarding.domain.port.out.KycVerificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class OnboardingController {

    private final KycDecisionService kycDecisionService;
    private final KycVerificationRepository kycVerificationRepository;

    public OnboardingController(KycDecisionService kycDecisionService, KycVerificationRepository kycVerificationRepository) {
        this.kycDecisionService = kycDecisionService;
        this.kycVerificationRepository = kycVerificationRepository;
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

    @GetMapping("/kyc/review-queue")
    public ResponseEntity<Map<String, Object>> getKycReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<KycVerificationRecord> pending = kycVerificationRepository.findByVerificationStatusOrderByCreatedAtDesc(KycStatus.MANUAL_REVIEW);
        
        List<Map<String, Object>> content = pending.stream()
            .map(k -> {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("verificationId", k.verificationId().toString());
                item.put("mykadMasked", maskMykad(k.mykadNumber()));
                item.put("fullName", k.fullName() != null ? k.fullName() : "");
                item.put("amlStatus", k.amlStatus() != null ? k.amlStatus().name() : "UNKNOWN");
                item.put("biometricMatch", k.biometricMatch() != null ? k.biometricMatch().name() : "UNKNOWN");
                item.put("rejectionReason", k.rejectionReason() != null ? k.rejectionReason() : "");
                return item;
            })
            .toList();
        
        return ResponseEntity.ok(Map.of(
            "content", content,
            "totalElements", pending.size(),
            "totalPages", 1,
            "page", page,
            "size", size
        ));
    }
    
    private String maskMykad(String mykad) {
        if (mykad == null || mykad.length() < 4) return "****";
        return mykad.substring(0, 4) + "********";
    }
}
