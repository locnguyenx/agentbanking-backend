package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PINInventoryPort {

    PINInventoryResult validateInventory(String provider, BigDecimal faceValue);

    PINGenerationResult generatePIN(String provider, BigDecimal faceValue, String idempotencyKey);

    record PINInventoryResult(boolean available, int stockCount, String errorCode) {}
    record PINGenerationResult(boolean success, String pinCode, String serialNumber, LocalDate expiryDate, String errorCode) {}
}
