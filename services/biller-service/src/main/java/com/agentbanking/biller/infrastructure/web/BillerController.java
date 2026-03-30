package com.agentbanking.biller.infrastructure.web;

import com.agentbanking.biller.domain.port.in.ValidateBillUseCase;
import com.agentbanking.biller.domain.port.in.PayBillUseCase;
import com.agentbanking.biller.domain.port.in.ProcessTopupUseCase;
import com.agentbanking.biller.domain.port.in.JomPayUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class BillerController {

    private final ValidateBillUseCase validateBillUseCase;
    private final PayBillUseCase payBillUseCase;
    private final ProcessTopupUseCase processTopupUseCase;
    private final JomPayUseCase jomPayUseCase;

    public BillerController(ValidateBillUseCase validateBillUseCase,
                            PayBillUseCase payBillUseCase,
                            ProcessTopupUseCase processTopupUseCase,
                            JomPayUseCase jomPayUseCase) {
        this.validateBillUseCase = validateBillUseCase;
        this.payBillUseCase = payBillUseCase;
        this.processTopupUseCase = processTopupUseCase;
        this.jomPayUseCase = jomPayUseCase;
    }

    @PostMapping("/validate-ref")
    public ResponseEntity<Map<String, Object>> validateRef(@RequestBody Map<String, String> request) {
        String billerCode = request.get("billerCode");
        String ref1 = request.get("ref1");

        ValidateBillUseCase.ValidateBillResult result = validateBillUseCase.validateBill(billerCode, ref1);

        return ResponseEntity.ok(Map.of(
            "valid", result.valid(),
            "billerCode", result.billerCode(),
            "ref1", result.ref1(),
            "amount", result.amount(),
            "customerName", result.customerName()
        ));
    }

    @PostMapping("/pay-bill")
    public ResponseEntity<Map<String, Object>> payBill(@RequestBody Map<String, Object> request) {
        try {
            String billerCode = (String) request.get("billerCode");
            String ref1 = (String) request.get("ref1");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            UUID internalTxId = UUID.fromString((String) request.get("internalTransactionId"));

            PayBillUseCase.PayBillResult payment = payBillUseCase.payBill(billerCode, ref1, amount, internalTxId);

            return ResponseEntity.ok(Map.of(
                "status", payment.status(),
                "paymentId", payment.paymentId().toString(),
                "receiptNo", payment.receiptNo(),
                "billerReference", payment.billerReference(),
                "amount", payment.amount()
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
            
            UUID internalTxId;
            if (request.containsKey("idempotencyKey") && request.get("idempotencyKey") != null) {
                String idempotencyKey = (String) request.get("idempotencyKey");
                internalTxId = UUID.nameUUIDFromBytes(idempotencyKey.getBytes());
            } else if (request.containsKey("internalTransactionId") && request.get("internalTransactionId") != null) {
                internalTxId = UUID.fromString((String) request.get("internalTransactionId"));
            } else {
                internalTxId = UUID.randomUUID();
            }

            ProcessTopupUseCase.ProcessTopupResult topup = processTopupUseCase.processTopup(telco, phoneNumber, amount, internalTxId);

            return ResponseEntity.ok(Map.of(
                "status", topup.status(),
                "topupId", topup.topupId().toString(),
                "telcoReference", topup.telcoReference(),
                "amount", topup.amount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_TOPUP_FAILED", "message", e.getMessage())
            ));
        }
    }

    @PostMapping("/billpayment/jompay")
    public ResponseEntity<Map<String, Object>> jomPay(@RequestBody Map<String, Object> request) {
        try {
            String billerCode = (String) request.get("billerCode");
            String billerName = (String) request.get("billerName");
            String ref1 = (String) request.get("ref1");
            String ref2 = (String) request.get("ref2");
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String currency = (String) request.get("currency");
            
            UUID internalTxId;
            if (request.containsKey("idempotencyKey") && request.get("idempotencyKey") != null) {
                String idempotencyKey = (String) request.get("idempotencyKey");
                internalTxId = UUID.nameUUIDFromBytes(idempotencyKey.getBytes());
            } else if (request.containsKey("internalTransactionId") && request.get("internalTransactionId") != null) {
                internalTxId = UUID.fromString((String) request.get("internalTransactionId"));
            } else {
                internalTxId = UUID.randomUUID();
            }

            JomPayUseCase.JomPayCommand command = new JomPayUseCase.JomPayCommand(
                billerCode, billerName, ref1, ref2, amount, currency, internalTxId
            );

            JomPayUseCase.JomPayResult result = jomPayUseCase.processJomPay(command);

            return ResponseEntity.ok(Map.of(
                "status", result.status(),
                "paymentId", result.paymentId().toString(),
                "receiptNo", result.receiptNo(),
                "billerReference", result.billerReference(),
                "amount", result.amount()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "FAILED",
                "error", Map.of("code", "ERR_JOMPAY_FAILED", "message", e.getMessage())
            ));
        }
    }
}