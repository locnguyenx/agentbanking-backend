package com.agentbanking.orchestrator.domain.service;

import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.SagaResult;
import com.agentbanking.orchestrator.domain.port.in.ExecuteWithdrawalSagaUseCase.WithdrawalSagaCommand;
import com.agentbanking.orchestrator.domain.port.out.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransactionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransactionOrchestrator.class);

    private final IdempotencyService idempotencyService;
    private final RulesServicePort rulesServicePort;
    private final LedgerServicePort ledgerServicePort;
    private final SwitchAdapterPort switchAdapterPort;
    private final EventPublisherPort eventPublisherPort;

    public TransactionOrchestrator(
            IdempotencyService idempotencyService,
            RulesServicePort rulesServicePort,
            LedgerServicePort ledgerServicePort,
            SwitchAdapterPort switchAdapterPort,
            EventPublisherPort eventPublisherPort) {
        this.idempotencyService = idempotencyService;
        this.rulesServicePort = rulesServicePort;
        this.ledgerServicePort = ledgerServicePort;
        this.switchAdapterPort = switchAdapterPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    public SagaResult executeSaga(WithdrawalSagaCommand command) {
        log.info("Starting withdrawal saga for agent: {}, amount: {}", command.agentId(), command.amount());

        IdempotencyService.SagaResult cached = idempotencyService.getCachedResponse(command.idempotencyKey());
        if (cached != null) {
            log.info("Returning cached response for idempotency key: {}", command.idempotencyKey());
            return new SagaResult(cached.status(), null, cached.message());
        }

        try {
            Map<String, Object> velocityResult = checkVelocity(command);
            if (!Boolean.TRUE.equals(velocityResult.get("approved"))) {
                String reason = (String) velocityResult.getOrDefault("reason", "Velocity check failed");
                SagaResult result = new SagaResult("FAILED", null, reason);
                idempotencyService.cacheResponse(command.idempotencyKey(), 
                    new IdempotencyService.SagaResult(result.status(), result.message()));
                return result;
            }

            Map<String, Object> fees = calculateFees(command);

            Map<String, Object> blockResult = blockFloat(command, fees);
            UUID transactionId = (UUID) blockResult.get("transactionId");

            Map<String, Object> authResult = authorizeTransaction(command, transactionId);
            String authStatus = (String) authResult.get("status");

            if ("APPROVED".equals(authStatus) || "00".equals(authResult.get("responseCode"))) {
                ledgerServicePort.commitFloat(transactionId);
                
                Map<String, Object> event = buildCompletedEvent(command, transactionId, fees, authResult);
                eventPublisherPort.publishTransactionCompleted(event);
                
                SagaResult result = new SagaResult("COMPLETED", transactionId, "Withdrawal successful");
                idempotencyService.cacheResponse(command.idempotencyKey(),
                    new IdempotencyService.SagaResult(result.status(), result.message()));
                return result;
            } else {
                ledgerServicePort.rollbackFloat(transactionId);
                
                Map<String, Object> event = buildFailedEvent(command, transactionId, authResult);
                eventPublisherPort.publishTransactionFailed(event);
                
                SagaResult result = new SagaResult("FAILED", transactionId, "Authorization declined: " + authResult.get("responseCode"));
                idempotencyService.cacheResponse(command.idempotencyKey(),
                    new IdempotencyService.SagaResult(result.status(), result.message()));
                return result;
            }

        } catch (Exception e) {
            log.error("Saga failed with exception: {}", e.getMessage(), e);
            SagaResult result = new SagaResult("FAILED", null, "Error: " + e.getMessage());
            idempotencyService.cacheResponse(command.idempotencyKey(),
                new IdempotencyService.SagaResult(result.status(), result.message()));
            return result;
        }
    }

    private Map<String, Object> checkVelocity(WithdrawalSagaCommand command) {
        Map<String, Object> request = new HashMap<>();
        request.put("agentId", command.agentId());
        request.put("amount", command.amount());
        request.put("customerCardMasked", command.customerCardMasked());
        request.put("geofenceLat", command.geofenceLat());
        request.put("geofenceLng", command.geofenceLng());
        
        return rulesServicePort.checkVelocity(request);
    }

    private Map<String, Object> calculateFees(WithdrawalSagaCommand command) {
        Map<String, Object> request = new HashMap<>();
        request.put("agentId", command.agentId());
        request.put("amount", command.amount());
        request.put("transactionType", "WITHDRAWAL");
        
        return rulesServicePort.calculateFees(request);
    }

    private Map<String, Object> blockFloat(WithdrawalSagaCommand command, Map<String, Object> fees) {
        Map<String, Object> request = new HashMap<>();
        request.put("agentId", command.agentId());
        request.put("amount", command.amount());
        request.put("customerFee", fees.get("customerFee"));
        request.put("agentCommission", fees.get("agentCommission"));
        request.put("bankShare", fees.get("bankShare"));
        request.put("idempotencyKey", command.idempotencyKey());
        request.put("customerCardMasked", command.customerCardMasked());
        request.put("geofenceLat", command.geofenceLat());
        request.put("geofenceLng", command.geofenceLng());
        
        return ledgerServicePort.blockFloat(request);
    }

    private Map<String, Object> authorizeTransaction(WithdrawalSagaCommand command, UUID transactionId) {
        Map<String, Object> request = new HashMap<>();
        request.put("internalTransactionId", transactionId);
        request.put("pan", command.pan());
        request.put("amount", command.amount());
        
        return switchAdapterPort.authorizeTransaction(request);
    }

    private Map<String, Object> buildCompletedEvent(WithdrawalSagaCommand command, UUID transactionId, 
                                                      Map<String, Object> fees, Map<String, Object> authResult) {
        Map<String, Object> event = new HashMap<>();
        event.put("transactionId", transactionId);
        event.put("agentId", command.agentId());
        event.put("amount", command.amount());
        event.put("customerFee", fees.get("customerFee"));
        event.put("agentCommission", fees.get("agentCommission"));
        event.put("bankShare", fees.get("bankShare"));
        event.put("status", "COMPLETED");
        event.put("customerCardMasked", command.customerCardMasked());
        event.put("switchTxId", authResult.get("switchTxId"));
        event.put("referenceId", authResult.get("referenceId"));
        return event;
    }

    private Map<String, Object> buildFailedEvent(WithdrawalSagaCommand command, UUID transactionId, 
                                                  Map<String, Object> authResult) {
        Map<String, Object> event = new HashMap<>();
        event.put("transactionId", transactionId);
        event.put("agentId", command.agentId());
        event.put("amount", command.amount());
        event.put("status", "FAILED");
        event.put("customerCardMasked", command.customerCardMasked());
        event.put("reason", authResult.get("responseCode"));
        return event;
    }
}
