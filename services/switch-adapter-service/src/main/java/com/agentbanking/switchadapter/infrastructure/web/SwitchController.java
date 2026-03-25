package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.switchadapter.domain.model.SwitchTransaction;
import com.agentbanking.switchadapter.domain.service.SwitchAdapterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class SwitchController {

    private final SwitchAdapterService switchAdapterService;

    public SwitchController(SwitchAdapterService switchAdapterService) {
        this.switchAdapterService = switchAdapterService;
    }

    @PostMapping("/auth")
    public ResponseEntity<Map<String, Object>> cardAuth(@RequestBody Map<String, Object> request) {
        try {
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));
            String pan = (String) request.get("pan");
            double amount = ((Number) request.get("amount")).doubleValue();

            SwitchTransaction txn = switchAdapterService.processCardAuth(internalTxId, pan, amount);

            return ResponseEntity.ok(Map.of(
                "status", "APPROVED",
                "responseCode", txn.getIsoResponseCode(),
                "referenceId", txn.getSwitchReference(),
                "switchTxId", txn.getSwitchTxId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_SWITCH_AUTH_FAILED", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/reversal")
    public ResponseEntity<Map<String, Object>> reversal(@RequestBody Map<String, Object> request) {
        try {
            UUID originalTxId = UUID.fromString((String) request.get("originalTransactionId"));
            String originalRef = (String) request.get("originalReference");
            double amount = ((Number) request.get("amount")).doubleValue();

            SwitchTransaction txn = switchAdapterService.processReversal(originalTxId, originalRef, amount);

            return ResponseEntity.ok(Map.of(
                "status", "REVERSED",
                "responseCode", txn.getIsoResponseCode(),
                "referenceId", txn.getSwitchReference(),
                "switchTxId", txn.getSwitchTxId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_SWITCH_REVERSAL_FAILED", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/duitnow")
    public ResponseEntity<Map<String, Object>> duitNowTransfer(@RequestBody Map<String, Object> request) {
        try {
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));
            String proxyType = (String) request.get("proxyType");
            String proxyValue = (String) request.get("proxyValue");
            double amount = ((Number) request.get("amount")).doubleValue();

            SwitchTransaction txn = switchAdapterService.processDuitNowTransfer(internalTxId, proxyType, proxyValue, amount);

            return ResponseEntity.ok(Map.of(
                "status", "SETTLED",
                "responseCode", txn.getIsoResponseCode(),
                "referenceId", txn.getSwitchReference(),
                "switchTxId", txn.getSwitchTxId().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_DUITNOW_FAILED", "message", e.getMessage())
            ));
        }
    }
}
