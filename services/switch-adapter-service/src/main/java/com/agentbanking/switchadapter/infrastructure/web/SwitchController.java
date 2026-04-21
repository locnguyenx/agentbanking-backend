package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.common.exception.ErrorResponse;
import com.agentbanking.switchadapter.domain.port.in.AuthorizeTransactionUseCase;
import com.agentbanking.switchadapter.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.switchadapter.domain.port.in.ProcessReversalUseCase;
import com.agentbanking.switchadapter.domain.port.in.DuitNowTransferUseCase;
import com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase;
import com.agentbanking.switchadapter.infrastructure.web.dto.CardAuthRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.BalanceInquiryRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.DuitNowRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.ReversalRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class SwitchController {

    private final AuthorizeTransactionUseCase authorizeTransactionUseCase;
    private final ProcessReversalUseCase processReversalUseCase;
    private final DuitNowTransferUseCase duitNowTransferUseCase;
    private final BalanceInquiryUseCase balanceInquiryUseCase;
    private final ProxyEnquiryUseCase proxyEnquiryUseCase;

    public SwitchController(AuthorizeTransactionUseCase authorizeTransactionUseCase,
                            ProcessReversalUseCase processReversalUseCase,
                            DuitNowTransferUseCase duitNowTransferUseCase,
                            BalanceInquiryUseCase balanceInquiryUseCase,
                            ProxyEnquiryUseCase proxyEnquiryUseCase) {
        this.authorizeTransactionUseCase = authorizeTransactionUseCase;
        this.processReversalUseCase = processReversalUseCase;
        this.duitNowTransferUseCase = duitNowTransferUseCase;
        this.balanceInquiryUseCase = balanceInquiryUseCase;
        this.proxyEnquiryUseCase = proxyEnquiryUseCase;
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
                "reference", txn.reference(),
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
                request.getEffectiveTransactionId(),
                request.proxyType(),
                request.proxyValue(),
                request.amount()
            );

            return ResponseEntity.ok(Map.of(
                "status", txn.status(),
                "responseCode", txn.responseCode(),
                "reference", txn.reference(),
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

    @PostMapping("/proxy-enquiry")
    public ResponseEntity<?> proxyEnquiry(@Valid @RequestBody ProxyEnquiryInput request) {
        try {
            ProxyEnquiryUseCase.ProxyEnquiryResult result = proxyEnquiryUseCase.enquiryProxy(
                request.proxyValue(),
                request.proxyType()
            );

            return ResponseEntity.ok(Map.of(
                "valid", result.valid(),
                "recipientName", result.recipientName(),
                "bankCode", result.bankCode()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_PROXY_ENQUIRY_FAILED",
                e.getMessage(),
                "DECLINE"
            ));
        }
    }

    record ProxyEnquiryInput(String proxyType, String proxyValue) {}

    @PostMapping("/balance-inquiry")
    public ResponseEntity<?> balanceInquiry(@Valid @RequestBody BalanceInquiryRequest request) {
        try {
            BalanceInquiryUseCase.BalanceInquiryResult txn = balanceInquiryUseCase.inquiryBalance(
                UUID.randomUUID(),
                request.encryptedCardData(),
                request.pinBlock()
            );

            return ResponseEntity.ok(Map.of(
                "status", txn.status(),
                "responseCode", txn.responseCode(),
                "balance", txn.balance(),
                "currency", txn.currency(),
                "accountMasked", txn.accountMasked()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_SWITCH_AUTH_FAILED",
                e.getMessage(),
                "DECLINE"
            ));
        }
    }
}
