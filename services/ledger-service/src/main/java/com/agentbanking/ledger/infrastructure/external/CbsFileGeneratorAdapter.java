package com.agentbanking.ledger.infrastructure.external;

import com.agentbanking.ledger.domain.model.SettlementSummaryRecord;
import com.agentbanking.ledger.domain.port.out.CbsFileGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class CbsFileGeneratorAdapter implements CbsFileGenerator {

    private static final Logger log = LoggerFactory.getLogger(CbsFileGeneratorAdapter.class);
    private static final String CBS_OUTPUT_DIR = "/data/cbs/settlement";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public String generateCsv(List<SettlementSummaryRecord> settlements) {
        StringBuilder sb = new StringBuilder();
        sb.append("settlement_id,agent_id,settlement_date,total_withdrawals,total_deposits,");
        sb.append("total_bill_payments,total_retail_sales,total_commissions,net_amount,direction,currency\n");

        for (SettlementSummaryRecord s : settlements) {
            sb.append(s.settlementId()).append(",");
            sb.append(s.agentId()).append(",");
            sb.append(s.settlementDate()).append(",");
            sb.append(s.totalWithdrawals()).append(",");
            sb.append(s.totalDeposits()).append(",");
            sb.append(s.totalBillPayments()).append(",");
            sb.append(s.totalRetailSales()).append(",");
            sb.append(s.totalCommissions()).append(",");
            sb.append(s.netAmount()).append(",");
            sb.append(s.direction()).append(",");
            sb.append(s.currency()).append("\n");
        }

        String csv = sb.toString();

        if (!settlements.isEmpty()) {
            LocalDate date = settlements.get(0).settlementDate();
            String filename = "settlement_" + date.format(DATE_FMT) + ".csv";
            writeFile(csv, filename);
        }

        return csv;
    }

    private void writeFile(String content, String filename) {
        try {
            Path dir = Paths.get(CBS_OUTPUT_DIR);
            Files.createDirectories(dir);
            Path filePath = dir.resolve(filename);
            Files.writeString(filePath, content);
            log.info("CBS settlement file generated: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to write CBS settlement file: {}", filename, e);
        }
    }
}
