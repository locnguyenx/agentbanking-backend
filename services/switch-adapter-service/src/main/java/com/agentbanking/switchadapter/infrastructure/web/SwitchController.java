package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.common.exception.ErrorResponse;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
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

    private final SwitchAdapterService switchAdapterService;

    public SwitchController(SwitchAdapterService switchAdapterService) {
        this.switchAdapterService = switchAdapterService;
    }

    @PostMapping("/auth")
    public ResponseEntity<?> cardAuth(@Valid @RequestBody CardAuthRequest request) {
        try {
            SwitchTransactionRecord txn = switchAdapterService.processCardAuth(
                request.internalTransactionId(),
                request.pan(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", "APPROVED",
                "responseCode", txn.isoResponseCode(),
                "referenceId", txn.switchReference(),
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
            SwitchTransactionRecord txn = switchAdapterService.processReversal(
                request.originalTransactionId(),
                request.originalReference(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", "REVERSED",
                "responseCode", txn.isoResponseCode(),
                "referenceId", txn.switchReference(),
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
            SwitchTransactionRecord txn = switchAdapterService.processDuitNowTransfer(
                request.internalTransactionId(),
                request.proxyType(),
                request.proxyValue(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", "SETTLED",
                "responseCode", txn.isoResponseCode(),
                "referenceId", txn.switchReference(),
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
