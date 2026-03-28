package com.agentbanking.onboarding.infrastructure.web;

import com.agentbanking.onboarding.domain.model.AgentOnboardingRecord;
import com.agentbanking.onboarding.domain.model.OnboardingDecision;
import com.agentbanking.onboarding.domain.port.in.EvaluateMicroAgentOnboardingUseCase;
import com.agentbanking.onboarding.domain.port.in.StartMicroAgentOnboardingUseCase;
import com.agentbanking.onboarding.domain.port.in.StartStandardPremierOnboardingUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for agent onboarding endpoints
 */
@RestController
@RequestMapping("/internal/onboarding/agent")
public class AgentOnboardingController {

    private final StartMicroAgentOnboardingUseCase startMicroAgentOnboardingUseCase;
    private final EvaluateMicroAgentOnboardingUseCase evaluateMicroAgentOnboardingUseCase;
    private final StartStandardPremierOnboardingUseCase startStandardPremierOnboardingUseCase;

    public AgentOnboardingController(StartMicroAgentOnboardingUseCase startMicroAgentOnboardingUseCase,
                                     EvaluateMicroAgentOnboardingUseCase evaluateMicroAgentOnboardingUseCase,
                                     StartStandardPremierOnboardingUseCase startStandardPremierOnboardingUseCase) {
        this.startMicroAgentOnboardingUseCase = startMicroAgentOnboardingUseCase;
        this.evaluateMicroAgentOnboardingUseCase = evaluateMicroAgentOnboardingUseCase;
        this.startStandardPremierOnboardingUseCase = startStandardPremierOnboardingUseCase;
    }

    /**
     * Start micro-agent onboarding (Conditional STP)
     * POST /internal/onboarding/agent/micro/start
     */
    @PostMapping("/micro/start")
    public ResponseEntity<Map<String, Object>> startMicroAgentOnboarding(@RequestBody Map<String, String> request) {
        try {
            String mykadNumber = request.get("mykadNumber");
            
            AgentOnboardingRecord record = startMicroAgentOnboardingUseCase.start(mykadNumber);
            
            return ResponseEntity.ok(Map.of(
                "onboardingId", record.onboardingId().toString(),
                "mykadNumber", record.mykadNumber(),
                "agentTier", record.agentTier(),
                "status", "STARTED",
                "message", "Micro-agent onboarding initiated. Perform evaluation to get decision."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_ONBOARDING_START_FAILED", "message", e.getMessage())
            ));
        }
    }

    /**
     * Evaluate micro-agent onboarding (auto-approval or manual review)
     * POST /internal/onboarding/agent/micro/evaluate
     */
    @PostMapping("/micro/evaluate")
    public ResponseEntity<Map<String, Object>> evaluateMicroAgentOnboarding(@RequestBody Map<String, String> request) {
        try {
            UUID onboardingId = UUID.fromString(request.get("onboardingId"));
            
            OnboardingDecision decision = evaluateMicroAgentOnboardingUseCase.evaluate(onboardingId);
            
            return ResponseEntity.ok(Map.of(
                "decisionId", decision.decisionId().toString(),
                "onboardingId", decision.onboardingId().toString(),
                "decisionType", decision.decisionType().toString(),
                "reason", decision.reason(),
                "reviewerId", decision.reviewerId(),
                "decidedAt", decision.decidedAt().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_ONBOARDING_EVALUATION_FAILED", "message", e.getMessage())
            ));
        }
    }

    /**
     * Start standard/premier agent onboarding (Non-STP - for human review)
     * POST /internal/onboarding/agent/standard/start
     */
    @PostMapping("/standard/start")
    public ResponseEntity<Map<String, Object>> startStandardPremierOnboarding(@RequestBody Map<String, String> request) {
        try {
            String mykadNumber = request.get("mykadNumber");
            String agentTier = request.get("agentTier"); // STANDARD or PREMIER
            
            if (!"STANDARD".equals(agentTier) && !"PREMIER".equals(agentTier)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", Map.of("code", "ERR_INVALID_AGENT_TIER", "message", "Agent tier must be STANDARD or PREMIER")
                ));
            }
            
            AgentOnboardingRecord record = startStandardPremierOnboardingUseCase.start(mykadNumber, agentTier);
            
            return ResponseEntity.ok(Map.of(
                "onboardingId", record.onboardingId().toString(),
                "mykadNumber", record.mykadNumber(),
                "agentTier", record.agentTier(),
                "status", "PENDING_REVIEW",
                "message", "Standard/Premier agent onboarding record created. Requires human review (Maker-Checker)."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_ONBOARDING_START_FAILED", "message", e.getMessage())
            ));
        }
    }
}