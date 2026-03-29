package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.ManageRoleUseCaseImpl;
import com.agentbanking.auth.domain.model.RoleRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for role management endpoints
 */
@RestController
@RequestMapping("/auth/role")
public class RoleController {

    private final ManageRoleUseCaseImpl manageRoleUseCase;

    public RoleController(ManageRoleUseCaseImpl manageRoleUseCase) {
        this.manageRoleUseCase = manageRoleUseCase;
    }

    @PostMapping
    public ResponseEntity<RoleRecord> createRole(@RequestBody RoleRecord roleRecord) {
        RoleRecord created = manageRoleUseCase.createRole(roleRecord);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleRecord> getRoleById(@PathVariable UUID id) {
        RoleRecord role = manageRoleUseCase.getRoleById(id);
        return role != null ? ResponseEntity.ok(role) : ResponseEntity.notFound().build();
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RoleRecord> getRoleByName(@PathVariable String name) {
        RoleRecord role = manageRoleUseCase.getRoleByName(name);
        return role != null ? ResponseEntity.ok(role) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleRecord> updateRole(@PathVariable UUID id, @RequestBody RoleRecord roleRecord) {
        RoleRecord updated = manageRoleUseCase.updateRole(id, roleRecord);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        boolean deleted = manageRoleUseCase.deleteRole(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}