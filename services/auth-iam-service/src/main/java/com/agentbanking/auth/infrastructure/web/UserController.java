package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.AuditLogServiceImpl;
import com.agentbanking.auth.application.usecase.ManageUserUseCaseImpl;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;
import com.agentbanking.common.audit.AuditAction;
import com.agentbanking.common.audit.AuditLogRecord;
import com.agentbanking.common.audit.AuditOutcome;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.infrastructure.web.dto.MyProfileResponse;
import com.agentbanking.auth.infrastructure.web.dto.PasswordResetDto;
import com.agentbanking.auth.infrastructure.web.dto.UserCreateDto;
import com.agentbanking.auth.infrastructure.web.dto.UserResponseDto;
import com.agentbanking.auth.infrastructure.web.dto.UserUpdateDto;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/users")
public class UserController {

    private final ManageUserUseCaseImpl manageUserUseCase;
    private final AuditLogServiceImpl auditLogService;

    public UserController(ManageUserUseCaseImpl manageUserUseCase, AuditLogServiceImpl auditLogService) {
        this.manageUserUseCase = manageUserUseCase;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/me")
    public ResponseEntity<MyProfileResponse> getCurrentUserProfile(
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

    @PostMapping("/bootstrap")
    public ResponseEntity<UserResponseDto> bootstrap(@Valid @RequestBody UserCreateDto userDto) {
        // Only allow bootstrap if no users exist yet (first-time setup)
        UserRecord existingUser = manageUserUseCase.getUserByUsername(userDto.username());
        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // 21 fields (0-20): userId, username, email, phone, passwordHash, fullName, status, userType, agentId, agentCode, mustChangePassword, temporaryPasswordExpiresAt, permissions, failedLoginAttempts, lockedUntil, passwordChangedAt, passwordExpiresAt, createdAt, updatedAt, lastLoginAt, createdBy
        // Indices: 0:userId, 1:username, 2:email, 3:phone, 4:passwordHash, 5:fullName, 6:status, 7:userType, 8:agentId, 9:agentCode, 10:mustChangePassword, 11:tempExpiry, 12:perms, 13:failed, 14:locked, 15:changedAt, 16:expireAt, 17:created, 18:updated, 19:lastLogin, 20:createdBy
        UserRecord userRecord = new UserRecord(
                null,                // 0
                userDto.username(),  // 1
                userDto.email(),     // 2
                null,                // 3
                userDto.password(),  // 4
                userDto.fullName(),  // 5
                UserStatus.ACTIVE,   // 6
                userDto.userType() != null ? UserType.valueOf(userDto.userType()) : UserType.INTERNAL, // 7
                userDto.agentId(),   // 8
                null,                // 9
                false,               // 10: mustChangePassword
                null,                // 11
                null,                // 12
                0,                   // 13
                null,                // 14
                null,                // 15
                null,                // 16
                null,                // 17
                null,                // 18
                null,                // 19
                "system"             // 20
        );
        UserRecord created = manageUserUseCase.createUser(userRecord);
        return new ResponseEntity<>(toResponseDto(created), HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserCreateDto userDto) {
        // 21 fields (0-20): userId, username, email, phone, passwordHash, fullName, status, userType, agentId, agentCode, mustChangePassword, temporaryPasswordExpiresAt, permissions, failedLoginAttempts, lockedUntil, passwordChangedAt, passwordExpiresAt, createdAt, updatedAt, lastLoginAt, createdBy
        // Indices: 0:userId, 1:username, 2:email, 3:phone, 4:passwordHash, 5:fullName, 6:status, 7:userType, 8:agentId, 9:agentCode, 10:mustChangePassword, 11:tempExpiry, 12:perms, 13:failed, 14:locked, 15:changedAt, 16:expireAt, 17:created, 18:updated, 19:lastLogin, 20:createdBy
        UserRecord userRecord = new UserRecord(
                null,                // 0
                userDto.username(),  // 1
                userDto.email(),     // 2
                null,                // 3
                userDto.password(),  // 4
                userDto.fullName(),  // 5
                UserStatus.ACTIVE,   // 6
                userDto.userType() != null ? UserType.valueOf(userDto.userType()) : UserType.EXTERNAL, // 7
                userDto.agentId(),   // 8
                null,                // 9
                false,               // 10: mustChangePassword
                null,                // 11
                null,                // 12
                0,                   // 13
                null,                // 14
                null,                // 15
                null,                // 16
                null,                // 17
                null,                // 18
                null,                // 19
                "admin"              // 20
        );
        UserRecord created = manageUserUseCase.createUser(userRecord);
        
        auditLogService.log(new AuditLogRecord(
            UUID.randomUUID(),
            "User",
            created.userId(),
            AuditAction.USER_CREATED,
            "admin",
            null,
            "unknown",
            LocalDateTime.now(),
            AuditOutcome.SUCCESS,
            null,
            null,
            null,
            "auth-iam-service",
            null,
            null
        ));
        
        return new ResponseEntity<>(toResponseDto(created), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        UserRecord user = manageUserUseCase.getUserById(id);
        return user != null ? ResponseEntity.ok(toResponseDto(user)) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserRecord> users = manageUserUseCase.getAllUsers();
        List<UserResponseDto> userDtos = users.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateDto userDto) {
        UserRecord userRecord = new UserRecord(
                id,
                userDto.username(),
                userDto.email(),
                null, // phone
                null, // passwordHash (will be set by service)
                userDto.fullName(),
                null, // status
                null, // userType
                null, // agentId
                null, // agentCode
                null, // mustChangePassword
                null, // temporaryPasswordExpiresAt
                null, // permissions
                null, // failedLoginAttempts
                null, // lockedUntil
                null, // passwordChangedAt
                null, // passwordExpiresAt
                null, // createdAt
                null, // updatedAt
                null, // lastLoginAt
                null // createdBy (will be set by service)
        );
        UserRecord updated = manageUserUseCase.updateUser(id, userRecord);
        return updated != null ? ResponseEntity.ok(toResponseDto(updated)) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        boolean deleted = manageUserUseCase.deleteUser(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<Map<String, Object>> lockUser(@PathVariable UUID id) {
        boolean locked = manageUserUseCase.lockUser(id);
        if (!locked) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "userId", id,
            "status", "LOCKED"
        ));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Map<String, Object>> unlockUser(@PathVariable UUID id) {
        boolean unlocked = manageUserUseCase.unlockUser(id);
        if (!unlocked) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "userId", id,
            "status", "ACTIVE"
        ));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@PathVariable UUID id, @Valid @RequestBody PasswordResetDto passwordDto) {
        boolean reset = manageUserUseCase.resetPassword(id, passwordDto.newPassword());
        if (!reset) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of(
            "userId", id,
            "temporaryPassword", passwordDto.newPassword(),
            "mustChangePassword", true
        ));
    }

    private UserResponseDto toResponseDto(UserRecord user) {
        return new UserResponseDto(
                user.userId(),
                user.username(),
                user.email(),
                user.fullName(),
                user.status(),
                user.userType(),
                user.agentId(),
                user.permissions(),
                user.createdAt(),
                user.lastLoginAt()
        );
    }
}
