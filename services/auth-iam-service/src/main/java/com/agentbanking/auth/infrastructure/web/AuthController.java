package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.AuthenticateUserUseCaseImpl;
import com.agentbanking.auth.application.usecase.ManageUserUseCaseImpl;
import com.agentbanking.auth.domain.model.AuthenticationResult;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.port.in.ManageSessionUseCase;
import com.agentbanking.auth.infrastructure.web.dto.AuthRequestDto;
import com.agentbanking.auth.infrastructure.web.dto.AuthResponseDto;
import com.agentbanking.auth.infrastructure.web.dto.MyProfileResponse;
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
    private final ManageUserUseCaseImpl manageUserUseCase;

    public AuthController(AuthenticateUserUseCaseImpl authenticateUserUseCase,
                          ManageSessionUseCase manageSessionUseCase,
                          ManageUserUseCaseImpl manageUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.manageSessionUseCase = manageSessionUseCase;
        this.manageUserUseCase = manageUserUseCase;
    }

    @PostMapping("/token")
    public ResponseEntity<?> authenticate(@Valid @RequestBody AuthRequestDto authRequest) {
        try {
            String username = authRequest.getUsername();
            String password = authRequest.getPassword();

            AuthenticationResult result = authenticateUserUseCase.authenticate(username, password);

            if (result == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of("ERR_AUTH_INVALID_CREDENTIALS", "Invalid username or password", "DECLINE", UUID.randomUUID().toString()));
            }

            AuthResponseDto response = new AuthResponseDto();
            response.setAccessToken(result.accessToken());
            response.setRefreshToken(result.refreshToken());
            response.setExpiresIn(result.expiresIn());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("ERR_AUTH_INVALID_CREDENTIALS", "Invalid username or password", "DECLINE", UUID.randomUUID().toString()));
        }
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

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        UserRecord user = manageUserUseCase.getProfile(
            java.util.UUID.fromString(userId));
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        MyProfileResponse profile = new MyProfileResponse(
            user.userId() != null ? user.userId().toString() : null,
            user.username(),
            user.email(),
            user.fullName(),
            user.userType() != null ? user.userType().name() : null,
            user.status() != null ? user.status().name() : null,
            user.agentId() != null ? user.agentId().toString() : null,
            user.mustChangePassword(),
            user.temporaryPasswordExpiresAt(),
            user.createdAt(),
            user.lastLoginAt(),
            user.permissions()
        );
        return ResponseEntity.ok(profile);
    }
}