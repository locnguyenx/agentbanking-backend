package com.agentbanking.switchadapter.infrastructure.web;

import com.agentbanking.common.exception.ErrorResponse;
import com.agentbanking.switchadapter.domain.port.in.AuthorizeTransactionUseCase;
import com.agentbanking.switchadapter.domain.port.in.BalanceInquiryUseCase;
import com.agentbanking.switchadapter.domain.port.in.ProcessReversalUseCase;
import com.agentbanking.switchadapter.domain.port.in.DuitNowTransferUseCase;
import com.agentbanking.switchadapter.domain.port.in.TransactionQuoteUseCase;
import com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase;
import com.agentbanking.switchadapter.infrastructure.web.dto.CardAuthRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.BalanceInquiryRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.DuitNowRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.ReversalRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.TransactionQuoteRequest;
import com.agentbanking.switchadapter.infrastructure.web.dto.TransactionQuoteResponse;
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
    private final TransactionQuoteUseCase transactionQuoteUseCase;
    private final ProxyEnquiryUseCase proxyEnquiryUseCase;

    public SwitchController(AuthorizeTransactionUseCase authorizeTransactionUseCase,
                            ProcessReversalUseCase processReversalUseCase,
                            DuitNowTransferUseCase duitNowTransferUseCase,
                            BalanceInquiryUseCase balanceInquiryUseCase,
                            TransactionQuoteUseCase transactionQuoteUseCase,
                            ProxyEnquiryUseCase proxyEnquiryUseCase) {
        this.authorizeTransactionUseCase = authorizeTransactionUseCase;
        this.processReversalUseCase = processReversalUseCase;
        this.duitNowTransferUseCase = duitNowTransferUseCase;
        this.balanceInquiryUseCase = balanceInquiryUseCase;
        this.transactionQuoteUseCase = transactionQuoteUseCase;
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
                request.getEffectiveTransactionId(),
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

    @PostMapping("/transactions/quote")
    public ResponseEntity<?> getQuote(@Valid @RequestBody TransactionQuoteRequest request,
                                       @RequestHeader(value = "X-Agent-Id", required = false) String agentId,
                                       @RequestHeader(value = "X-Agent-Tier", required = false) String agentTier) {
        try {
            TransactionQuoteUseCase.QuoteResult result = transactionQuoteUseCase.calculateQuote(
                agentId != null ? agentId : "unknown",
                agentTier != null ? agentTier : "STANDARD",
                request.amount(),
                request.serviceCode(),
                request.fundingSource(),
                request.billerRouting()
            );

            return ResponseEntity.ok(TransactionQuoteResponse.from(result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_BIZ_QUOTE_CALCULATION_FAILED",
                e.getMessage(),
                "RETRY"
            ));
        }
    }

    @GetMapping("/transfer/proxy/enquiry")
    public ResponseEntity<?> proxyEnquiry(@RequestParam String proxyId,
                                           @RequestParam String proxyType) {
        try {
            ProxyEnquiryUseCase.ProxyEnquiryResult result = proxyEnquiryUseCase.enquiryProxy(proxyId, proxyType);
            return ResponseEntity.ok(Map.of(
                "name", result.name(),
                "proxyType", result.proxyType()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(ErrorResponse.of(
                "ERR_BIZ_PROXY_NOT_FOUND",
                e.getMessage(),
                "DECLINE"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ErrorResponse.of(
                "ERR_EXT_PROXY_ENQUIRY_FAILED",
                e.getMessage(),
                "RETRY"
            ));
        }
    }
}