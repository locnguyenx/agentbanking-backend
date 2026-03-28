package com.agentbanking.ledger.domain.service;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.domain.model.DiscrepancyStatus;
import com.agentbanking.ledger.domain.model.DiscrepancyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReconciliationService {

    public List<DiscrepancyCase> reconcile(List<Map<String, Object>> internalTransactions,
                                            List<Map<String, Object>> networkTransactions) {
        List<DiscrepancyCase> discrepancies = new ArrayList<>();

        // Build lookup maps
        Map<String, Map<String, Object>> internalByTxnId = buildLookup(internalTransactions);
        Map<String, Map<String, Object>> networkByTxnId = buildLookup(networkTransactions);

        // Check for Ghost transactions (internal success, network missing)
        for (Map.Entry<String, Map<String, Object>> entry : internalByTxnId.entrySet()) {
            String txnId = entry.getKey();
            Map<String, Object> internalTxn = entry.getValue();

            if (!networkByTxnId.containsKey(txnId)) {
                discrepancies.add(new DiscrepancyCase(
                    UUID.randomUUID(),
                    txnId,
                    DiscrepancyType.GHOST,
                    (BigDecimal) internalTxn.get("amount"),
                    null,
                    DiscrepancyStatus.PENDING_MAKER,
                    null, null, null, null, null, null,
                    Instant.now(), null
                ));
            }
        }

        // Check for Orphan transactions (network success, internal missing)
        for (Map.Entry<String, Map<String, Object>> entry : networkByTxnId.entrySet()) {
            String txnId = entry.getKey();
            Map<String, Object> networkTxn = entry.getValue();

            if (!internalByTxnId.containsKey(txnId)) {
                discrepancies.add(new DiscrepancyCase(
                    UUID.randomUUID(),
                    txnId,
                    DiscrepancyType.ORPHAN,
                    null,
                    (BigDecimal) networkTxn.get("amount"),
                    DiscrepancyStatus.PENDING_MAKER,
                    null, null, null, null, null, null,
                    Instant.now(), null
                ));
            }
        }

        // Check for Mismatch transactions (both present, amounts differ)
        for (Map.Entry<String, Map<String, Object>> entry : internalByTxnId.entrySet()) {
            String txnId = entry.getKey();
            Map<String, Object> internalTxn = entry.getValue();

            if (networkByTxnId.containsKey(txnId)) {
                Map<String, Object> networkTxn = networkByTxnId.get(txnId);
                BigDecimal internalAmount = (BigDecimal) internalTxn.get("amount");
                BigDecimal networkAmount = (BigDecimal) networkTxn.get("amount");

                if (internalAmount.compareTo(networkAmount) != 0) {
                    discrepancies.add(new DiscrepancyCase(
                        UUID.randomUUID(),
                        txnId,
                        DiscrepancyType.MISMATCH,
                        internalAmount,
                        networkAmount,
                        DiscrepancyStatus.PENDING_MAKER,
                        null, null, null, null, null, null,
                        Instant.now(), null
                    ));
                }
            }
        }

        return discrepancies;
    }

    private Map<String, Map<String, Object>> buildLookup(List<Map<String, Object>> transactions) {
        return transactions.stream()
            .collect(java.util.stream.Collectors.toMap(
                txn -> (String) txn.get("transactionId"),
                txn -> txn,
                (existing, replacement) -> existing
            ));
    }
}
