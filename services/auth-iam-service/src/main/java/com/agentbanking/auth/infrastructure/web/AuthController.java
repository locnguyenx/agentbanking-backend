package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.AuthenticateUserUseCaseImpl;
import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.infrastructure.web.dto.AuthRequestDto;
import com.agentbanking.auth.infrastructure.web.dto.AuthResponseDto;
import com.agentbanking.auth.infrastructure.web.dto.RefreshTokenDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUserUseCaseImpl authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCaseImpl authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/token")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequestDto authRequest) {
        String username = authRequest.getUsername();
        String password = authRequest.getPassword();

        AuthenticationResult result = authenticateUserUseCase.authenticate(username, password);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", "ERR_INVALID_CREDENTIALS",
                    "message", "Invalid username or password",
                    "action_code", "DECLINE",
                    "trace_id", UUID.randomUUID().toString(),
                    "timestamp", java.time.Instant.now().toString()
                )
            ));
        }

        AuthResponseDto response = new AuthResponseDto();
        response.setAccessToken(result.accessToken());
        response.setRefreshToken(result.refreshToken());
        response.setExpiresIn(result.expiresIn());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenDto request) {
        String refreshToken = request.refreshToken();

        AuthenticationResult result = authenticateUserUseCase.refreshToken(refreshToken);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", "ERR_INVALID_REFRESH_TOKEN",
                    "message", "Invalid or expired refresh token",
                    "action_code", "DECLINE",
                    "trace_id", UUID.randomUUID().toString(),
                    "timestamp", java.time.Instant.now().toString()
                )
            ));
        }

        AuthResponseDto response = new AuthResponseDto();
        response.setAccessToken(result.accessToken());
        response.setRefreshToken(result.refreshToken());
        response.setExpiresIn(result.expiresIn());

        return ResponseEntity.ok(response);
    }
}