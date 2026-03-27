package com.agentbanking.ledger.application.job;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;
import com.agentbanking.ledger.domain.service.SettlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class EodSettlementJob {

    private static final Logger log = LoggerFactory.getLogger(EodSettlementJob.class);
    private static final ZoneId MYT_ZONE = ZoneId.of("Asia/Kuala_Lumpur");

    private final SettlementService settlementService;

    public EodSettlementJob(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @Scheduled(cron = "59 59 23 * * *", zone = "Asia/Kuala_Lumpur")
    public void runSettlement() {
        LocalDate today = LocalDate.now(MYT_ZONE);
        log.info("Starting EOD settlement for date: {}", today);

        try {
            List<SettlementSummaryRecord> settlements = settlementService.runEodSettlement(today);
            log.info("EOD settlement completed. {} agent settlements generated for date: {}",
                    settlements.size(), today);
        } catch (Exception e) {
            log.error("EOD settlement failed for date: {}", today, e);
        }
    }
}
