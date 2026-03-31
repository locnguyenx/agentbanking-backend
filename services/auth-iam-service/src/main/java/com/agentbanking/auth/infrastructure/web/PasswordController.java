package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.domain.port.in.ManageUserUseCase;
import com.agentbanking.auth.domain.service.PasswordResetService;
import com.agentbanking.auth.infrastructure.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/password")
public class PasswordController {

    private final PasswordResetService passwordResetService;
    private final ManageUserUseCase manageUserUseCase;

    public PasswordController(PasswordResetService passwordResetService,
                              ManageUserUseCase manageUserUseCase) {
        this.passwordResetService = passwordResetService;
        this.manageUserUseCase = manageUserUseCase;
    }

    @PostMapping("/forgot")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getUsername());
        return ResponseEntity.ok(new ForgotPasswordResponse("If the user exists, an OTP has been sent"));
    }

    @PostMapping("/reset")
    public ResponseEntity<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.verifyReset(request.getUsername(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(new ResetPasswordResponse("Password reset successfully"));
    }

    @PostMapping("/change")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        manageUserUseCase.changePassword(java.util.UUID.fromString(userId), 
                                         request.getCurrentPassword(), 
                                         request.getNewPassword());
        return ResponseEntity.ok(new ChangePasswordResponse("Password changed successfully"));
    }
}
