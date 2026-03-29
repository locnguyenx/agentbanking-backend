package com.agentbanking.ledger.infrastructure.web;

import com.agentbanking.ledger.domain.port.in.ProcessCashBackUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessPinPurchaseUseCase;
import com.agentbanking.ledger.domain.port.in.ProcessRetailSaleUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for merchant transaction endpoints
 */
@RestController
@RequestMapping("/internal/merchant")
public class MerchantController {

    private final ProcessRetailSaleUseCase processRetailSaleUseCase;
    private final ProcessCashBackUseCase processCashBackUseCase;
    private final ProcessPinPurchaseUseCase processPinPurchaseUseCase;

    public MerchantController(ProcessRetailSaleUseCase processRetailSaleUseCase,
                              ProcessCashBackUseCase processCashBackUseCase,
                              ProcessPinPurchaseUseCase processPinPurchaseUseCase) {
        this.processRetailSaleUseCase = processRetailSaleUseCase;
        this.processCashBackUseCase = processCashBackUseCase;
        this.processPinPurchaseUseCase = processPinPurchaseUseCase;
    }

    /**
     * Process retail sale (card/QR or PIN purchase)
     * POST /internal/merchant/retail-sale
     */
    @PostMapping("/retail-sale")
    public ResponseEntity<ProcessRetailSaleUseCase.RetailSaleResponse> processRetailSale(
            @RequestBody ProcessRetailSaleUseCase.RetailSaleCommand command) {
        try {
            var response = processRetailSaleUseCase.processRetailSale(command);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Process cash-back transaction
     * POST /internal/merchant/cash-back
     */
    @PostMapping("/cash-back")
    public ResponseEntity<ProcessCashBackUseCase.CashBackResponse> processCashBack(
            @RequestBody ProcessCashBackUseCase.CashBackCommand command) {
        try {
            var response = processCashBackUseCase.processCashBack(command);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Process PIN voucher purchase
     * POST /internal/merchant/pin-purchase
     */
    @PostMapping("/pin-purchase")
    public ResponseEntity<ProcessPinPurchaseUseCase.PinPurchaseResponse> processPinPurchase(
            @RequestBody ProcessPinPurchaseUseCase.PinPurchaseCommand command) {
        try {
            var response = processPinPurchaseUseCase.processPinPurchase(command);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}
