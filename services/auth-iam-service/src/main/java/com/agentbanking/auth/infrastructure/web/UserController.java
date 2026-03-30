package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.ManageUserUseCaseImpl;
import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.infrastructure.web.dto.PasswordResetDto;
import com.agentbanking.auth.infrastructure.web.dto.UserCreateDto;
import com.agentbanking.auth.infrastructure.web.dto.UserResponseDto;
import com.agentbanking.auth.infrastructure.web.dto.UserUpdateDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth/users")
public class UserController {

    private final ManageUserUseCaseImpl manageUserUseCase;

    public UserController(ManageUserUseCaseImpl manageUserUseCase) {
        this.manageUserUseCase = manageUserUseCase;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<UserResponseDto> bootstrap(@Valid @RequestBody UserCreateDto userDto) {
        // Only allow bootstrap if no users exist yet (first-time setup)
        UserRecord existingUser = manageUserUseCase.getUserByUsername(userDto.username());
        if (existingUser != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        
        // For bootstrap, pass "system" as createdBy to satisfy NOT NULL constraint
        UserRecord userRecord = new UserRecord(
                null,
                userDto.username(),
                userDto.email(),
                userDto.password(),
                userDto.fullName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "system"
        );
        UserRecord created = manageUserUseCase.createUser(userRecord);
        return new ResponseEntity<>(toResponseDto(created), HttpStatus.CREATED);
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody UserCreateDto userDto) {
        // For now, use "admin" as createdBy - in production this would come from the authenticated user
        UserRecord userRecord = new UserRecord(
                null,
                userDto.username(),
                userDto.email(),
                userDto.password(),
                userDto.fullName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "admin"
        );
        UserRecord created = manageUserUseCase.createUser(userRecord);
        return new ResponseEntity<>(toResponseDto(created), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable UUID id) {
        UserRecord user = manageUserUseCase.getUserById(id);
        return user != null ? ResponseEntity.ok(toResponseDto(user)) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateDto userDto) {
        UserRecord userRecord = new UserRecord(
                id,
                userDto.username(),
                userDto.email(),
                null,
                userDto.fullName(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
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
    public ResponseEntity<Void> lockUser(@PathVariable UUID id) {
        boolean locked = manageUserUseCase.lockUser(id);
        return locked ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable UUID id) {
        boolean unlocked = manageUserUseCase.unlockUser(id);
        return unlocked ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody PasswordResetDto passwordDto) {
        boolean reset = manageUserUseCase.resetPassword(id, passwordDto.newPassword());
        return reset ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private UserResponseDto toResponseDto(UserRecord user) {
        return new UserResponseDto(
                user.userId(),
                user.username(),
                user.email(),
                user.fullName(),
                user.status(),
                user.permissions(),
                user.createdAt(),
                user.lastLoginAt()
        );
    }
}
