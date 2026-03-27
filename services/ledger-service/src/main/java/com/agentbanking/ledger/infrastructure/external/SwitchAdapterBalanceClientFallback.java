package com.agentbanking.ledger.infrastructure.external;

import com.agentbanking.common.exception.LedgerException;
import com.agentbanking.common.security.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;

public class SwitchAdapterBalanceClientFallback implements FallbackFactory<SwitchAdapterBalanceClient> {

    private static final Logger log = LoggerFactory.getLogger(SwitchAdapterBalanceClientFallback.class);

    @Override
    public SwitchAdapterBalanceClient create(Throwable cause) {
        log.error("Switch Adapter unavailable, fallback triggered: {}", cause.getMessage());
        return new SwitchAdapterBalanceClient() {
            @Override
            public SwitchBalanceResponse getBalance(SwitchBalanceRequest request) {
                throw new LedgerException(
                    ErrorCodes.ERR_SWITCH_UNAVAILABLE,
                    "RETRY",
                    "Switch Adapter service unavailable: " + cause.getMessage()
                );
            }
        };
    }
}
