package com.agentbanking.orchestrator.infrastructure.web;

import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.SagaResult;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.WithdrawalSagaCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrchestratorController {

    private final ExecuteWithdrawalSagaUseCase withdrawalSagaUseCase;

    public OrchestratorController(ExecuteWithdrawalSagaUseCase withdrawalSagaUseCase) {
        this.withdrawalSagaUseCase = withdrawalSagaUseCase;
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@Valid @RequestBody WithdrawalRequest request) {
        WithdrawalSagaCommand command = new WithdrawalSagaCommand(
            request.agentId(),
            request.amount(),
            request.pan(),
            request.customerCardMasked(),
            request.idempotencyKey(),
            request.geofenceLat(),
            request.geofenceLng()
        );

        SagaResult result = withdrawalSagaUseCase.executeSaga(command);

        if ("COMPLETED".equals(result.status())) {
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "transactionId", result.transactionId().toString(),
                "message", result.message()
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of(
                    "code", "ERR_BIZ_WITHDRAWAL_FAILED",
                    "message", result.message()
                )
            ));
        }
    }

    public record WithdrawalRequest(
        @NotNull UUID agentId,
        @NotNull BigDecimal amount,
        @NotNull String pan,
        String customerCardMasked,
        String idempotencyKey,
        BigDecimal geofenceLat,
        BigDecimal geofenceLng
    ) {}
}
