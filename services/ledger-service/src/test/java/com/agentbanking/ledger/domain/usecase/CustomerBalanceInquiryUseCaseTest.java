package com.agentbanking.ledger.domain.usecase;

import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.ledger.application.usecase.CustomerBalanceInquiryUseCaseImpl;
import com.agentbanking.ledger.domain.port.in.CustomerBalanceInquiryUseCase;
import com.agentbanking.ledger.infrastructure.external.SwitchAdapterBalanceClient;
import com.agentbanking.ledger.infrastructure.external.SwitchAdapterBalanceClientFallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerBalanceInquiryUseCaseTest {

    @Mock
    private SwitchAdapterBalanceClient switchAdapterBalanceClient;

    private CustomerBalanceInquiryUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CustomerBalanceInquiryUseCaseImpl(switchAdapterBalanceClient);
    }

    @Test
    void shouldReturnBalanceFromSwitchAdapter() {
        when(switchAdapterBalanceClient.getBalance(any()))
            .thenReturn(new SwitchAdapterBalanceClient.SwitchBalanceResponse(
                "SUCCESS",
                "00",
                new BigDecimal("5000.00"),
                "MYR",
                "411111******1111"
            ));

        CustomerBalanceInquiryUseCase.CustomerBalanceResponse response = useCase.inquire(
            new CustomerBalanceInquiryUseCase.CustomerInquiryCommand(
                "encryptedCardData",
                "pinBlock"
            )
        );

        assertNotNull(response);
        assertEquals(new BigDecimal("5000.00"), response.balance());
        assertEquals("MYR", response.currency());
        assertEquals("411111******1111", response.accountMasked());
    }

    @Test
    void shouldRejectInquiryWithInvalidCard() {
        when(switchAdapterBalanceClient.getBalance(any()))
            .thenThrow(new LedgerException(
                ErrorCodes.ERR_INVALID_CARD, "DECLINE"
            ));

        assertThrows(LedgerException.class, () ->
            useCase.inquire(
                new CustomerBalanceInquiryUseCase.CustomerInquiryCommand(
                    "invalidCardData",
                    "pinBlock"
                )
            )
        );
    }

    @Test
    void shouldReturnGracefulErrorWhenSwitchAdapterUnavailable() {
        SwitchAdapterBalanceClientFallback fallback = new SwitchAdapterBalanceClientFallback();
        SwitchAdapterBalanceClient fallbackClient = fallback.create(new RuntimeException("Connection refused"));

        LedgerException exception = assertThrows(LedgerException.class, () ->
            fallbackClient.getBalance(new SwitchAdapterBalanceClient.SwitchBalanceRequest("data", "pin"))
        );

        assertEquals(ErrorCodes.ERR_SWITCH_UNAVAILABLE, exception.getErrorCode());
        assertEquals("RETRY", exception.getActionCode());
    }
}
