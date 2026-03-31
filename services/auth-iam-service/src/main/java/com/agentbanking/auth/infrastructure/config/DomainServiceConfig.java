package com.agentbanking.auth.infrastructure.config;

import com.agentbanking.auth.application.usecase.AuthenticateUserUseCaseImpl;
import com.agentbanking.auth.application.usecase.CheckPermissionUseCaseImpl;
import com.agentbanking.auth.application.usecase.ManagePermissionUseCaseImpl;
import com.agentbanking.auth.application.usecase.ManageRoleUseCaseImpl;
import com.agentbanking.auth.application.usecase.ManageSessionUseCaseImpl;
import com.agentbanking.auth.application.usecase.ManageUserUseCaseImpl;
import com.agentbanking.auth.application.usecase.ValidateTokenUseCaseImpl;
import com.agentbanking.auth.domain.service.AuthenticationService;
import com.agentbanking.auth.domain.service.AuthorizationService;
import com.agentbanking.auth.domain.service.AuditService;
import com.agentbanking.auth.domain.service.PasswordResetService;
import com.agentbanking.auth.domain.service.TemporaryPasswordGenerator;
import com.agentbanking.auth.domain.service.UserManagementService;
import com.agentbanking.auth.domain.port.out.NotificationPublisher;
import com.agentbanking.auth.domain.port.out.OtpStore;
import com.agentbanking.auth.domain.port.out.AuditLogRepository;
import com.agentbanking.auth.domain.port.out.PasswordHasher;
import com.agentbanking.auth.domain.port.out.PermissionRepository;
import com.agentbanking.auth.domain.port.out.RoleRepository;
import com.agentbanking.auth.domain.port.out.SessionRepository;
import com.agentbanking.auth.domain.port.out.TokenProvider;
import com.agentbanking.auth.domain.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for registering domain services as Spring beans.
 * This follows the hexagonal architecture requirement that domain services
 * must be registered as beans in the infrastructure layer.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public AuthenticationService authenticationService(UserRepository userRepository,
                                                      TokenProvider tokenProvider,
                                                      PasswordHasher passwordHasher) {
        return new AuthenticationService(userRepository, tokenProvider, passwordHasher);
    }

    @Bean
    public UserManagementService userManagementService(UserRepository userRepository,
                                                    PasswordHasher passwordHasher,
                                                    TemporaryPasswordGenerator temporaryPasswordGenerator) {
        return new UserManagementService(userRepository, passwordHasher, temporaryPasswordGenerator);
    }

    @Bean
    public AuditService auditService(AuditLogRepository auditLogRepository) {
        return new AuditService(auditLogRepository);
    }

    @Bean
    public TemporaryPasswordGenerator temporaryPasswordGenerator() {
        return new TemporaryPasswordGenerator();
    }

    @Bean
    public PasswordResetService passwordResetService(OtpStore otpStore,
                                                       NotificationPublisher notificationPublisher,
                                                       PasswordHasher passwordHasher,
                                                       UserRepository userRepository,
                                                       TemporaryPasswordGenerator temporaryPasswordGenerator) {
        return new PasswordResetService(otpStore, notificationPublisher, passwordHasher, userRepository, temporaryPasswordGenerator);
    }

    // Use case beans
    @Bean
    public AuthenticateUserUseCaseImpl authenticateUserUseCase(AuthenticationService authenticationService) {
        return new AuthenticateUserUseCaseImpl(authenticationService);
    }

    @Bean
    public ManageUserUseCaseImpl manageUserUseCase(UserRepository userRepository,
                                                   PasswordHasher passwordHasher) {
        return new ManageUserUseCaseImpl(userRepository, passwordHasher);
    }

    @Bean
    public AuthorizationService authorizationService(RoleRepository roleRepository,
                                                    PermissionRepository permissionRepository,
                                                    UserRepository userRepository) {
        return new AuthorizationService(roleRepository, permissionRepository, userRepository);
    }

    @Bean
    public ManagePermissionUseCaseImpl managePermissionUseCase(PermissionRepository permissionRepository) {
        return new ManagePermissionUseCaseImpl(permissionRepository);
    }

    @Bean
    public ValidateTokenUseCaseImpl validateTokenUseCase(TokenProvider tokenProvider) {
        return new ValidateTokenUseCaseImpl(tokenProvider);
    }

    @Bean
    public CheckPermissionUseCaseImpl checkPermissionUseCase(UserRepository userRepository) {
        return new CheckPermissionUseCaseImpl(userRepository);
    }

    @Bean
    public ManageSessionUseCaseImpl manageSessionUseCase(SessionRepository sessionRepository) {
        return new ManageSessionUseCaseImpl(sessionRepository);
    }

    @Bean
    public ManageRoleUseCaseImpl manageRoleUseCase(RoleRepository roleRepository,
                                                   PermissionRepository permissionRepository) {
        return new ManageRoleUseCaseImpl(roleRepository, permissionRepository);
    }
}