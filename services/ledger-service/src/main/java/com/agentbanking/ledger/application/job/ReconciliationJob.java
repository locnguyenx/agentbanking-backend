package com.agentbanking.ledger.application.job;

import com.agentbanking.ledger.domain.model.DiscrepancyCase;
import com.agentbanking.ledger.domain.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);
    private static final ZoneId MYT_ZONE = ZoneId.of("Asia/Kuala_Lumpur");

    private final ReconciliationService reconciliationService;

    public ReconciliationJob(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Kuala_Lumpur")
    public void runReconciliation() {
        LocalDate today = LocalDate.now(MYT_ZONE);
        log.info("Starting reconciliation for date: {}", today);

        try {
            // TODO: Extract internal ledger data from TransactionRepository
            List<Map<String, Object>> internalTransactions = List.of();

            // TODO: Extract PayNet PSR data via switch-adapter
            List<Map<String, Object>> networkTransactions = List.of();

            // Triple-match reconciliation
            List<DiscrepancyCase> discrepancies = reconciliationService.reconcile(
                    internalTransactions, networkTransactions);

            if (discrepancies.isEmpty()) {
                log.info("Reconciliation completed for date: {}. No discrepancies found.", today);
            } else {
                log.warn("Reconciliation completed for date: {}. Found {} discrepancies (Ghosts: {}, Orphans: {}, Mismatches: {})",
                        today,
                        discrepancies.size(),
                        discrepancies.stream().filter(d -> d.discrepancyType() == com.agentbanking.ledger.domain.model.DiscrepancyType.GHOST).count(),
                        discrepancies.stream().filter(d -> d.discrepancyType() == com.agentbanking.ledger.domain.model.DiscrepancyType.ORPHAN).count(),
                        discrepancies.stream().filter(d -> d.discrepancyType() == com.agentbanking.ledger.domain.model.DiscrepancyType.MISMATCH).count()
                );
            }
        } catch (Exception e) {
            log.error("Reconciliation failed for date: {}", today, e);
        }
    }
}
