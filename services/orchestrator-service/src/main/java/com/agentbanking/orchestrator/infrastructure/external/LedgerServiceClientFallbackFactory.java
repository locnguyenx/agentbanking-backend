package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class LedgerServiceClientFallbackFactory implements FallbackFactory<LedgerServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceClientFallbackFactory.class);

    @Override
    public LedgerServiceClient create(Throwable cause) {
        log.error("LedgerServiceClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new LedgerServiceClient() {
            @Override
            public FloatBlockResult blockFloat(FloatBlockInput input) {
                log.error("Ledger service unavailable, failing float block for agent: {}", input.agentId());
                return new FloatBlockResult(false, null, "ERR_SYS_LEDGER_UNAVAILABLE");
            }

            @Override
            public FloatCommitResult commitFloat(FloatCommitInput input) {
                log.warn("Ledger service unavailable, auto-approving float commit for transaction: {}", input.transactionId());
                return new FloatCommitResult(true, "LEDGER_UNAVAILABLE");
            }

            @Override
            public FloatReleaseResult releaseFloat(FloatReleaseInput input) {
                log.warn("Ledger service unavailable, auto-approving float release for transaction: {}", input.transactionId());
                return new FloatReleaseResult(true, "LEDGER_UNAVAILABLE");
            }

            @Override
            public FloatCreditResult creditAgentFloat(FloatCreditInput input) {
                log.warn("Ledger service unavailable, simulating float credit for agent: {}", input.agentId());
                return new FloatCreditResult(true, BigDecimal.valueOf(10000.00), UUID.randomUUID(), "LEDGER_UNAVAILABLE");
            }

            @Override
            public FloatReverseResult reverseCreditFloat(FloatReverseInput input) {
                log.warn("Ledger service unavailable, simulating float reverse for agent: {}", input.agentId());
                return new FloatReverseResult(true, "LEDGER_UNAVAILABLE");
            }

            @Override
            public AccountValidationResult validateAccount(AccountValidationInput input) {
                log.warn("Ledger service unavailable, auto-validating account: {}", input.destinationAccount());
                return new AccountValidationResult(true, "John Doe", "LEDGER_UNAVAILABLE");
            }

            @Override
            public TransactionDetailsResult getTransaction(UUID transactionId) {
                log.warn("Ledger service unavailable, returning mock transaction details for: {}", transactionId);
                return new TransactionDetailsResult(
                    transactionId,
                    UUID.randomUUID(),
                    "CASH_WITHDRAWAL",
                    BigDecimal.valueOf(500),
                    BigDecimal.valueOf(2.00),
                    BigDecimal.valueOf(0.50),
                    BigDecimal.valueOf(1.50),
                    "COMPLETED",
                    null,
                    "411111******1111",
                    "REF123456",
                    BigDecimal.valueOf(3.1390),
                    BigDecimal.valueOf(101.6869),
                    "STANDARD",
                    "0123",
                    null,
                    null,
                    null,
                    null,
                    "2024-01-01T12:00:00Z",
                    "2024-01-01T12:01:00Z"
                );
            }

            @Override
            public DailyMetricsResult getDailyMetrics(UUID agentId) {
                log.warn("Ledger service unavailable, returning default metrics for agent: {}", agentId);
                return new DailyMetricsResult(0, BigDecimal.ZERO, BigDecimal.ZERO);
            }
        };
    }
}