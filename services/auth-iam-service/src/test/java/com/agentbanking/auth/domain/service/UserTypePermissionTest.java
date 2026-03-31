package com.agentbanking.auth.domain.service;

import com.agentbanking.auth.domain.model.UserRecord;
import com.agentbanking.auth.domain.model.UserStatus;
import com.agentbanking.auth.domain.model.UserType;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserTypePermissionTest {

    @Mock
    private UserRepository userRepository;

    private AuthorizationService authorizationService;

    private static final Set<String> INTERNAL_PERMISSIONS = Set.of(
            "user:create",
            "user:read",
            "user:update",
            "role:assign",
            "backoffice:access",
            "audit:view"
    );

    private static final Set<String> EXTERNAL_PERMISSIONS = Set.of(
            "transaction:create",
            "transaction:read",
            "agent:view",
            "agent:update",
            "kyc:submit"
    );

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(null, null, userRepository);
    }

    @Test
    void internalUser_withInternalPermission_shouldHaveAccess() {
        UUID userId = UUID.randomUUID();
        UserRecord internalUser = new UserRecord(
                userId,
                "admin001",
                "admin@bank.com",
                "+60123456789",
                "hashedPassword",
                "Admin User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                INTERNAL_PERMISSIONS,
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(internalUser));

        boolean hasPermission = authorizationService.hasPermission(userId, "user:create");

        assertTrue(hasPermission);
    }

    @Test
    void internalUser_withExternalPermission_shouldNotHaveAccess() {
        UUID userId = UUID.randomUUID();
        UserRecord internalUser = new UserRecord(
                userId,
                "admin001",
                "admin@bank.com",
                "+60123456789",
                "hashedPassword",
                "Admin User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                INTERNAL_PERMISSIONS,
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(internalUser));

        boolean hasPermission = authorizationService.hasPermission(userId, "transaction:create");

        assertFalse(hasPermission);
    }

    @Test
    void externalUser_withExternalPermission_shouldHaveAccess() {
        UUID userId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UserRecord externalUser = new UserRecord(
                userId,
                "AGENT001",
                "agent@business.com",
                "+60123456789",
                "hashedPassword",
                "Agent Business",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                "AGENT001",
                false,
                null,
                EXTERNAL_PERMISSIONS,
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(externalUser));

        boolean hasPermission = authorizationService.hasPermission(userId, "transaction:create");

        assertTrue(hasPermission);
    }

    @Test
    void externalUser_withInternalPermission_shouldNotHaveAccess() {
        UUID userId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UserRecord externalUser = new UserRecord(
                userId,
                "AGENT001",
                "agent@business.com",
                "+60123456789",
                "hashedPassword",
                "Agent Business",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                "AGENT001",
                false,
                null,
                EXTERNAL_PERMISSIONS,
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(externalUser));

        boolean hasPermission = authorizationService.hasPermission(userId, "user:create");

        assertFalse(hasPermission);
    }

    @Test
    void userTypeValidation_internalUserCannotHaveAgentPermissions() {
        UUID userId = UUID.randomUUID();
        UserRecord internalUser = new UserRecord(
                userId,
                "staff001",
                "staff@bank.com",
                "+60123456789",
                "hashedPassword",
                "Staff User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of("user:create", "user:read", "role:assign"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(internalUser));

        assertFalse(authorizationService.hasPermission(userId, "agent:view"));
        assertFalse(authorizationService.hasPermission(userId, "agent:update"));
        assertFalse(authorizationService.hasPermission(userId, "kyc:submit"));
    }

    @Test
    void userTypeValidation_externalUserCannotHaveInternalPermissions() {
        UUID userId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UserRecord externalUser = new UserRecord(
                userId,
                "AGENT002",
                "agent2@business.com",
                "+60123456789",
                "hashedPassword",
                "Agent Business 2",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                "AGENT002",
                false,
                null,
                Set.of("transaction:create", "transaction:read"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(externalUser));

        assertFalse(authorizationService.hasPermission(userId, "user:create"));
        assertFalse(authorizationService.hasPermission(userId, "user:read"));
        assertFalse(authorizationService.hasPermission(userId, "role:assign"));
        assertFalse(authorizationService.hasPermission(userId, "audit:view"));
    }

    @Test
    void nonExistentUser_shouldReturnFalse() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        boolean hasPermission = authorizationService.hasPermission(userId, "user:create");

        assertFalse(hasPermission);
    }

    @Test
    void userWithNullPermissions_shouldReturnFalse() {
        UUID userId = UUID.randomUUID();
        UserRecord userWithNullPerms = new UserRecord(
                userId,
                "testuser",
                "test@bank.com",
                "+60123456789",
                "hashedPassword",
                "Test User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                null,
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithNullPerms));

        boolean hasPermission = authorizationService.hasPermission(userId, "user:create");

        assertFalse(hasPermission);
    }

    @Test
    void internalUser_userTypeFieldShouldBeSet() {
        UUID userId = UUID.randomUUID();
        UserRecord internalUser = new UserRecord(
                userId,
                "internal001",
                "internal@bank.com",
                "+60123456789",
                "hashedPassword",
                "Internal User",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of("user:read"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );

        assertEquals(UserType.INTERNAL, internalUser.userType());
    }

    @Test
    void externalUser_userTypeFieldShouldBeSet() {
        UUID userId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UserRecord externalUser = new UserRecord(
                userId,
                "AGENT003",
                "agent3@business.com",
                "+60123456789",
                "hashedPassword",
                "Agent Business 3",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                "AGENT003",
                false,
                null,
                Set.of("transaction:read"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        assertEquals(UserType.EXTERNAL, externalUser.userType());
        assertEquals(agentId, externalUser.agentId());
    }

    @Test
    void externalUser_shouldHaveAgentIdSet() {
        UUID userId = UUID.randomUUID();
        UUID agentId = UUID.randomUUID();
        UserRecord externalUser = new UserRecord(
                userId,
                "AGENT004",
                "agent4@business.com",
                "+60123456789",
                "hashedPassword",
                "Agent Business 4",
                UserStatus.ACTIVE,
                UserType.EXTERNAL,
                agentId,
                "AGENT004",
                false,
                null,
                Set.of("agent:view"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "SYSTEM"
        );

        assertNotNull(externalUser.agentId());
        assertEquals(agentId, externalUser.agentId());
        assertEquals("AGENT004", externalUser.agentCode());
    }

    @Test
    void internalUser_shouldHaveNullAgentId() {
        UUID userId = UUID.randomUUID();
        UserRecord internalUser = new UserRecord(
                userId,
                "staff002",
                "staff2@bank.com",
                "+60123456789",
                "hashedPassword",
                "Staff User 2",
                UserStatus.ACTIVE,
                UserType.INTERNAL,
                null,
                null,
                false,
                null,
                Set.of("backoffice:access"),
                0,
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(90),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                "system"
        );

        assertNull(internalUser.agentId());
    }
}
