package com.agentbanking.biller.infrastructure.web;

import com.agentbanking.biller.domain.model.BillPayment;
import com.agentbanking.biller.domain.model.TopupTransaction;
import com.agentbanking.biller.domain.service.BillerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class BillerController {

    private final BillerService billerService;

    public BillerController(BillerService billerService) {
        this.billerService = billerService;
    }

    @PostMapping("/validate-ref")
    public ResponseEntity<Map<String, Object>> validateRef(@RequestBody Map<String, String> request) {
        String billerCode = request.get("billerCode");
        String ref1 = request.get("ref1");

        // Stub validation - would call biller API in production
        boolean valid = ref1 != null && !ref1.isBlank();

        return ResponseEntity.ok(Map.of(
            "valid", valid,
            "billerCode", billerCode,
            "ref1", ref1,
            "amount", valid ? "150.00" : "0",
            "customerName", valid ? "MOCK CUSTOMER" : ""
        ));
    }

    @PostMapping("/pay-bill")
    public ResponseEntity<Map<String, Object>> payBill(@RequestBody Map<String, Object> request) {
        try {
            String billerCode = (String) request.get("billerCode");
            String ref1 = (String) request.get("ref1");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));

            BillPayment payment = billerService.validateAndPay(billerCode, ref1, amount, internalTxId);

            return ResponseEntity.ok(Map.of(
                "status", "PAID",
                "paymentId", payment.getPaymentId().toString(),
                "receiptNo", payment.getReceiptNo(),
                "billerReference", payment.getBillerReference(),
                "amount", payment.getAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_BILLER_PAYMENT_FAILED", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/topup")
    public ResponseEntity<Map<String, Object>> topup(@RequestBody Map<String, Object> request) {
        try {
            String telco = (String) request.get("telco");
            String phoneNumber = (String) request.get("phoneNumber");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));

            TopupTransaction topup = billerService.processTopup(telco, phoneNumber, amount, internalTxId);

            return ResponseEntity.ok(Map.of(
                "status", "COMPLETED",
                "topupId", topup.getTopupId().toString(),
                "telcoReference", topup.getTelcoReference(),
                "amount", topup.getAmount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_TOPUP_FAILED", "message", e.getMessage())
            ));
        }
    }
}
