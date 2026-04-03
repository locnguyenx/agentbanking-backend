package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.AuthenticateUserUseCaseImpl;
import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.domain.port.in.ManageSessionUseCase;
import com.agentbanking.auth.infrastructure.web.dto.AuthRequestDto;
import com.agentbanking.auth.infrastructure.web.dto.AuthResponseDto;
import com.agentbanking.auth.infrastructure.web.dto.RefreshTokenDto;
import com.agentbanking.common.exception.ErrorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUserUseCaseImpl authenticateUserUseCase;
    private final ManageSessionUseCase manageSessionUseCase;

    public AuthController(AuthenticateUserUseCaseImpl authenticateUserUseCase,
                         ManageSessionUseCase manageSessionUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.manageSessionUseCase = manageSessionUseCase;
    }

    @PostMapping("/token")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequestDto authRequest) {
        String username = authRequest.getUsername();
        String password = authRequest.getPassword();

        AuthenticationResult result = authenticateUserUseCase.authenticate(username, password);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("ERR_INVALID_CREDENTIALS", "Invalid username or password", "DECLINE", UUID.randomUUID().toString()));
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("ERR_INVALID_REFRESH_TOKEN", "Invalid or expired refresh token", "DECLINE", UUID.randomUUID().toString()));
        }

        AuthResponseDto response = new AuthResponseDto();
        response.setAccessToken(result.accessToken());
        response.setRefreshToken(result.refreshToken());
        response.setExpiresIn(result.expiresIn());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<?> revoke(@RequestHeader("X-User-Id") String userIdHeader) {
        try {
            UUID userId = UUID.fromString(userIdHeader);
            boolean revoked = manageSessionUseCase.revokeAllSessionsForUser(userId);
            
            if (revoked) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of("ERR_USER_NOT_FOUND", "User not found or no active sessions", "REVIEW", UUID.randomUUID().toString()));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("ERR_INVALID_USER_ID", "Invalid user ID format", "DECLINE", UUID.randomUUID().toString()));
        }
    }
}