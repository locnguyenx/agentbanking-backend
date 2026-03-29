package com.agentbanking.auth.infrastructure.web;

import com.agentbanking.auth.application.usecase.ManagePermissionUseCaseImpl;
import com.agentbanking.auth.domain.model.PermissionRecord;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for permission management endpoints
 */
@RestController
@RequestMapping("/auth/permission")
public class PermissionController {

    private final ManagePermissionUseCaseImpl managePermissionUseCase;

    public PermissionController(ManagePermissionUseCaseImpl managePermissionUseCase) {
        this.managePermissionUseCase = managePermissionUseCase;
    }

    @PostMapping
    public ResponseEntity<PermissionRecord> createPermission(@RequestBody PermissionRecord permissionRecord) {
        PermissionRecord created = managePermissionUseCase.createPermission(permissionRecord);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionRecord> getPermissionById(@PathVariable UUID id) {
        PermissionRecord permission = managePermissionUseCase.getPermissionById(id);
        return permission != null ? ResponseEntity.ok(permission) : ResponseEntity.notFound().build();
    }

    @GetMapping("/key/{key}")
    public ResponseEntity<PermissionRecord> getPermissionByKey(@PathVariable String key) {
        PermissionRecord permission = managePermissionUseCase.getPermissionByKey(key);
        return permission != null ? ResponseEntity.ok(permission) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionRecord> updatePermission(@PathVariable UUID id, @RequestBody PermissionRecord permissionRecord) {
        PermissionRecord updated = managePermissionUseCase.updatePermission(id, permissionRecord);
        return updated != null ? ResponseEntity.ok(updated) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        boolean deleted = managePermissionUseCase.deletePermission(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}