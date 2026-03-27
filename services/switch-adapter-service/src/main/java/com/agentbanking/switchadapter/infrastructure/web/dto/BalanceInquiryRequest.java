package com.agentbanking.switchadapter.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record BalanceInquiryRequest(
    @NotBlank(message = "Encrypted card data is required")
    String encryptedCardData,
    
    @NotBlank(message = "PIN block is required")
    String pinBlock
) {}
