package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LedgerServiceAdapter implements LedgerServicePort {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceAdapter.class);

    private final LedgerServiceClient ledgerServiceClient;

    public LedgerServiceAdapter(LedgerServiceClient ledgerServiceClient) {
        this.ledgerServiceClient = ledgerServiceClient;
    }

    @Override
    public FloatBlockResult blockFloat(FloatBlockInput input) {
        log.info("Blocking float for agent: {}", input.agentId());
        return ledgerServiceClient.blockFloat(input);
    }

    @Override
    public FloatCommitResult commitFloat(FloatCommitInput input) {
        log.info("Committing float for transaction: {}", input.transactionId());
        return ledgerServiceClient.commitFloat(input);
    }

    @Override
    public FloatReleaseResult releaseFloat(FloatReleaseInput input) {
        log.info("Releasing float for transaction: {}", input.transactionId());
        return ledgerServiceClient.releaseFloat(input);
    }

    @Override
    public FloatCreditResult creditAgentFloat(FloatCreditInput input) {
        log.info("Crediting float for agent: {}", input.agentId());
        return ledgerServiceClient.creditAgentFloat(input);
    }

    @Override
    public FloatReverseResult reverseCreditFloat(FloatReverseInput input) {
        log.info("Reversing credit for agent: {}", input.agentId());
        return ledgerServiceClient.reverseCreditFloat(input);
    }

    @Override
    public AccountValidationResult validateAccount(AccountValidationInput input) {
        log.info("Validating account: {}", input.destinationAccount());
        return ledgerServiceClient.validateAccount(input);
    }

    @Override
    public TransactionDetailsResult getTransactionDetails(UUID transactionId) {
        log.info("Fetching transaction details from ledger for: {}", transactionId);
        return ledgerServiceClient.getTransaction(transactionId);
    }

    @Override
    public DailyMetricsResult getDailyMetrics(UUID agentId) {
        log.info("Fetching daily metrics for agent: {}", agentId);
        return ledgerServiceClient.getDailyMetrics(agentId);
    }
}
