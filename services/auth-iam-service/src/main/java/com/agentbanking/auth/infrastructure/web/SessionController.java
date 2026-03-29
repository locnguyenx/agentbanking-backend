package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.ManageSessionUseCaseImpl;
import com.agentbanking.auth.domain.model.SessionRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for session management endpoints
 */
@RestController
@RequestMapping("/auth/session")
public class SessionController {

    private final ManageSessionUseCaseImpl manageSessionUseCase;

    public SessionController(ManageSessionUseCaseImpl manageSessionUseCase) {
        this.manageSessionUseCase = manageSessionUseCase;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<SessionRecord> getSessionByUserId(@PathVariable UUID userId) {
        SessionRecord session = manageSessionUseCase.getSessionByUserId(userId);
        return session != null ? ResponseEntity.ok(session) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID sessionId) {
        boolean revoked = manageSessionUseCase.revokeSession(sessionId);
        return revoked ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> revokeAllSessionsForUser(@PathVariable UUID userId) {
        boolean revoked = manageSessionUseCase.revokeAllSessionsForUser(userId);
        return revoked ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{sessionId}/validate")
    public ResponseEntity<Map<String, Object>> validateSession(@PathVariable UUID sessionId) {
        boolean isValid = manageSessionUseCase.isValidSession(sessionId);
        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "session_id", sessionId.toString()
        ));
    }
}