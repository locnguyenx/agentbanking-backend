package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.common.exception.ErrorResponse;
import com.agentbanking.switchadapter.domain.port.in.AuthorizeTransactionUseCase;
import com.agentbanking.switchadapter.domain.port.in.ProcessReversalUseCase;
import com.agentbanking.switchadapter.domain.port.in.DuitNowTransferUseCase;
import com.agentbanking.switchadapter.infrastructure.web.dto.CardAuthRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.DuitNowRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.ReversalRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal")
public class SwitchController {

    private final AuthorizeTransactionUseCase authorizeTransactionUseCase;
    private final ProcessReversalUseCase processReversalUseCase;
    private final DuitNowTransferUseCase duitNowTransferUseCase;

    public SwitchController(AuthorizeTransactionUseCase authorizeTransactionUseCase,
                            ProcessReversalUseCase processReversalUseCase,
                            DuitNowTransferUseCase duitNowTransferUseCase) {
        this.authorizeTransactionUseCase = authorizeTransactionUseCase;
        this.processReversalUseCase = processReversalUseCase;
        this.duitNowTransferUseCase = duitNowTransferUseCase;
    }

    @PostMapping("/auth")
    public ResponseEntity<?> cardAuth(@Valid @RequestBody CardAuthRequest request) {
        try {
            AuthorizeTransactionUseCase.AuthorizeTransactionResult txn = authorizeTransactionUseCase.authorizeTransaction(
                request.internalTransactionId(),
                request.pan(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", txn.status(),
                "responseCode", txn.responseCode(),
                "referenceId", txn.referenceId(),
                "switchTxId", txn.switchTxId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_SWITCH_AUTH_FAILED",
                e.getMessage(),
                "DECLINE"
            ));
        }
    }

    @PostMapping("/reversal")
    public ResponseEntity<?> reversal(@Valid @RequestBody ReversalRequest request) {
        try {
            ProcessReversalUseCase.ProcessReversalResult txn = processReversalUseCase.processReversal(
                request.originalTransactionId(),
                request.originalReference(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", txn.status(),
                "responseCode", txn.responseCode(),
                "referenceId", txn.referenceId(),
                "switchTxId", txn.switchTxId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_SWITCH_REVERSAL_FAILED",
                e.getMessage(),
                "RETRY"
            ));
        }
    }

    @PostMapping("/duitnow")
    public ResponseEntity<?> duitNowTransfer(@Valid @RequestBody DuitNowRequest request) {
        try {
            DuitNowTransferUseCase.DuitNowTransferResult txn = duitNowTransferUseCase.transferDuitNow(
                request.internalTransactionId(),
                request.proxyType(),
                request.proxyValue(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", txn.status(),
                "responseCode", txn.responseCode(),
                "referenceId", txn.referenceId(),
                "switchTxId", txn.switchTxId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_DUITNOW_FAILED",
                e.getMessage(),
                "RETRY"
            ));
        }
    }
}