package com.agentbanking.biller.application.usecase;

import com.agentbanking.biller.domain.port.in.ValidateBillUseCase;
import org.springframework.stereotype.Service;

@Service
public class ValidateBillUseCaseImpl implements ValidateBillUseCase {

    @Override
    public ValidateBillResult validateBill(String billerCode, String ref1) {
        boolean valid = ref1 != null && !ref1.isBlank();

        return new ValidateBillResult(
            valid,
            billerCode,
            ref1,
            valid ? "150.00" : "0",
            valid ? "MOCK CUSTOMER" : ""
        );
    }
}