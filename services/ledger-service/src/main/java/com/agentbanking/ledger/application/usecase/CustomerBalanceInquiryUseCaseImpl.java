package com.agentbanking.ledger.application.usecase;

import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.ledger.domain.port.in.CustomerBalanceInquiryUseCase;
import com.agentbanking.ledger.infrastructure.external.SwitchAdapterBalanceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerBalanceInquiryUseCaseImpl implements CustomerBalanceInquiryUseCase {

    private static final Logger log = LoggerFactory.getLogger(CustomerBalanceInquiryUseCaseImpl.class);

    private final SwitchAdapterBalanceClient switchAdapterBalanceClient;

    public CustomerBalanceInquiryUseCaseImpl(SwitchAdapterBalanceClient switchAdapterBalanceClient) {
        this.switchAdapterBalanceClient = switchAdapterBalanceClient;
    }

    @Override
    public CustomerBalanceResponse inquire(CustomerInquiryCommand command) {
        log.info("Initiating balance inquiry for customer");

        SwitchAdapterBalanceClient.SwitchBalanceRequest request =
            new SwitchAdapterBalanceClient.SwitchBalanceRequest(
                command.encryptedCardData(),
                command.pinBlock()
            );

        SwitchAdapterBalanceClient.SwitchBalanceResponse response =
            switchAdapterBalanceClient.getBalance(request);

        if (!response.isSuccessful()) {
            log.error("Balance inquiry failed: status={}, responseCode={}",
                response.status(), response.responseCode());
            throw new LedgerException(ErrorCodes.ERR_INVALID_CARD, "DECLINE",
                "Card validation failed");
        }

        log.info("Balance inquiry successful: balance={} {}", response.balance(), response.currency());

        return new CustomerBalanceResponse(
            response.balance(),
            response.currency(),
            response.accountMasked()
        );
    }
}
