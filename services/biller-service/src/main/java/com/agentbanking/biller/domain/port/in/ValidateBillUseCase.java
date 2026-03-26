package com.agentbanking.biller.domain.port.in;

public interface ValidateBillUseCase {
    ValidateBillResult validateBill(String billerCode, String ref1);

    record ValidateBillResult(
        boolean valid,
        String billerCode,
        String ref1,
        String amount,
        String customerName
    ) {}
}