package com.agentbanking.onboarding.infrastructure.web;

import com.agentbanking.onboarding.domain.port.in.VerifyMyKadUseCase;
import com.agentbanking.onboarding.domain.port.in.BiometricMatchUseCase;
import com.agentbanking.onboarding.domain.port.in.GetReviewQueueUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class OnboardingController {

    private final VerifyMyKadUseCase verifyMyKadUseCase;
    private final BiometricMatchUseCase biometricMatchUseCase;
    private final GetReviewQueueUseCase getReviewQueueUseCase;

    public OnboardingController(VerifyMyKadUseCase verifyMyKadUseCase,
                                BiometricMatchUseCase biometricMatchUseCase,
                                GetReviewQueueUseCase getReviewQueueUseCase) {
        this.verifyMyKadUseCase = verifyMyKadUseCase;
        this.biometricMatchUseCase = biometricMatchUseCase;
        this.getReviewQueueUseCase = getReviewQueueUseCase;
    }

    @PostMapping("/verify-mykad")
    public ResponseEntity<Map<String, Object>> verifyMyKad(@RequestBody Map<String, String> request) {
        String mykad = request.get("mykadNumber");

        try {
            VerifyMyKadUseCase.VerifyMyKadResult result = verifyMyKadUseCase.verifyMyKad(mykad);
            return ResponseEntity.ok(Map.of(
                "verificationId", result.verificationId().toString(),
                "status", result.status(),
                "fullName", result.fullName(),
                "dateOfBirth", result.dateOfBirth(),
                "age", result.age(),
                "amlStatus", result.amlStatus()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_INVALID_MYKAD_FORMAT", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/biometric")
    public ResponseEntity<Map<String, Object>> biometricMatch(@RequestBody Map<String, String> request) {
        String verificationId = request.get("verificationId");
        String biometricData = request.get("biometricData");

        try {
            BiometricMatchUseCase.BiometricMatchResult result = biometricMatchUseCase.matchBiometric(verificationId, biometricData);
            return ResponseEntity.ok(Map.of(
                "verificationId", result.verificationId(),
                "status", result.status(),
                "biometricMatch", result.biometricMatch(),
                "timestamp", result.timestamp()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_MISSING_VERIFICATION_ID", "message", e.getMessage())
            ));
        }
    }

    @GetMapping("/kyc/review-queue")
    public ResponseEntity<Map<String, Object>> getKycReviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        GetReviewQueueUseCase.ReviewQueueResult result = getReviewQueueUseCase.getReviewQueue(page, size);

        return ResponseEntity.ok(Map.of(
            "content", result.content().stream()
                .map(item -> Map.of(
                    "verificationId", item.verificationId(),
                    "mykadMasked", item.mykadMasked(),
                    "fullName", item.fullName(),
                    "amlStatus", item.amlStatus(),
                    "biometricMatch", item.biometricMatch(),
                    "rejectionReason", item.rejectionReason()
                ))
                .toList(),
            "totalElements", result.totalElements(),
            "totalPages", result.totalPages(),
            "page", result.page(),
            "size", result.size()
        ));
    }
}