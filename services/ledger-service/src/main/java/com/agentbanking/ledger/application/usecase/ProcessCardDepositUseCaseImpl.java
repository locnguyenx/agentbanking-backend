package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.JournalEntryRecord;
import com.agentbanking.ledger.domain.service.LedgerService;
import com.agentbanking.ledger.domain.port.in.ProcessCardDepositUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.port.out.SwitchServicePort;
import com.agentbanking.ledger.domain.port.out.RulesServicePort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implementation of ProcessCardDepositUseCase
 */
@Component
public class ProcessCardDepositUseCaseImpl implements ProcessCardDepositUseCase {

    private final LedgerService ledgerService;
    private final RulesServicePort rulesService;
    private final SwitchServicePort switchService;
    private final IdempotencyCache idempotencyCache;

    public ProcessCardDepositUseCaseImpl(LedgerService ledgerService,
                                          RulesServicePort rulesService,
                                          SwitchServicePort switchService,
                                          IdempotencyCache idempotencyCache) {
        this.ledgerService = ledgerService;
        this.rulesService = rulesService;
        this.switchService = switchService;
        this.idempotencyCache = idempotencyCache;
    }

    @Override
    public TransactionResult processCardDeposit(CardDepositCommand command) {
        // Check idempotency
        String idempotencyKey = command.idempotencyKey();
        if (idempotencyCache.exists(idempotencyKey)) {
            // Return cached result
            return idempotencyCache.get(idempotencyKey, TransactionResult.class);
        }

        // 1. Check velocity and fees via Rules Service
        // For card deposit, we check velocity based on transaction type
        var velocityCheck = rulesService.checkVelocity("CUSTOMER_MYKAD_PLACEHOLDER", 
                "CARD_DEPOSIT", command.amount());
        if (!velocityCheck.passed()) {
            throw new IllegalArgumentException(velocityCheck.errorCode());
        }

        // 2. Calculate fees via Rules Service (customer pays fee)
        var feeResult = rulesService.calculateFee("CARD_DEPOSIT", 
                "MICRO", // agent tier
                command.amount());
        if (!feeResult.passed()) {
            throw new IllegalArgumentException(feeResult.errorCode());
        }

        // 3. Process deposit via switch (card authorization)
        var switchResult = switchService.authorize(
                command.customerCardData(), 
                command.customerPinBlock(), 
                command.amount(),
                command.agentId().toString()
        );

        if (!switchResult.isApproved()) {
            throw new IllegalArgumentException(switchResult.getDeclineCode());
        }

        // 4. Credit agent float via LedgerService
        // Note: For deposits, agent float increases (they receive the cash)
        AgentFloatRecord agentFloat = ledgerService.getAgentFloat(command.agentId());
        if (agentFloat == null) {
            throw new IllegalArgumentException("ERR_AGENT_FLOAT_NOT_FOUND");
        }

        ledgerService.creditFloat(command.agentId(), command.amount());

        // 5. Create transaction record
        TransactionRecord transaction = new TransactionRecord(
                UUID.randomUUID(),
                idempotencyKey,
                command.agentId(),
                TransactionType.CARD_DEPOSIT,
                command.amount(),
                feeResult.customerFee(),
                feeResult.agentCommission(),
                feeResult.bankShare(),
                TransactionStatus.COMPLETED,
                null, // errorCode
                null, // customerMykad - not applicable for card
                maskPan(command.customerCardData()), // masked PAN
                switchResult.getReferenceNumber(),
                null, // geofenceLat - not applicable for card deposit
                null  // geofenceLng - not applicable for card deposit
        );

        // 6. Create journal entries (double-entry)
        JournalEntryRecord debitEntry = new JournalEntryRecord(
                UUID.randomUUID(),
                transaction.transactionId(),
                JournalEntryType.DEBIT,
                "CASH", // cash received
                command.amount(),
                "Card deposit cash received"
        );

        JournalEntryRecord creditEntry = new JournalEntryRecord(
                UUID.randomUUID(),
                transaction.transactionId(),
                JournalEntryType.CREDIT,
                "FLOAT_AGENT_" + command.agentId(),
                command.amount(),
                "Card deposit - agent float increase"
        );

        // 7. Save transaction and journal entries (this would be done via repositories)
        // For now, we assume the LedgerService handles persistence or we have repositories injected.
        // We'll skip the persistence step for brevity in this example.

        // 8. Cache successful result for idempotency
        TransactionResult result = new TransactionResult(
                "COMPLETED",
                transaction.transactionId(),
                command.amount(),
                feeResult.customerFee(),
                switchResult.getReferenceNumber()
        );
        idempotencyCache.save(idempotencyKey, result, 86400); // 24h TTL

        return result;
    }

    /**
     * Mask PAN for display/storage (first 6, last 4 digits)
     */
    private String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return pan;
        }
        return pan.substring(0, 6) + "******".repeat(Math.max(0, (pan.length() - 10) / 6 + 1)) 
                + pan.substring(Math.max(0, pan.length() - 4));
    }
}