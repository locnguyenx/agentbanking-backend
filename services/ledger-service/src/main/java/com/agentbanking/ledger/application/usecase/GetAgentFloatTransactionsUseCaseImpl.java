package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.AgentFloatRecord;
import com.agentbanking.ledger.domain.model.TransactionRecord;
import com.agentbanking.ledger.domain.model.TransactionType;
import com.agentbanking.ledger.domain.port.in.GetAgentFloatTransactionsUseCase;
import com.agentbanking.ledger.domain.port.out.AgentFloatRepository;
import com.agentbanking.ledger.domain.port.out.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GetAgentFloatTransactionsUseCaseImpl implements GetAgentFloatTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public GetAgentFloatTransactionsUseCaseImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public TransactionSummary getSummary(UUID agentId, YearMonth period) {
        LocalDateTime startOfMonth = period.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = period.atEndOfMonth().atTime(23, 59, 59);

        List<TransactionRecord> transactions = transactionRepository.findByAgentId(agentId).stream()
                .filter(t -> t.createdAt() != null 
                        && !t.createdAt().isBefore(startOfMonth) 
                        && !t.createdAt().isAfter(endOfMonth))
                .collect(Collectors.toList());

        int totalCount = transactions.size();
        BigDecimal totalVolume = transactions.stream()
                .map(TransactionRecord::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<TransactionRecord>> byType = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.transactionType() != null 
                        ? t.transactionType().name() 
                        : "UNKNOWN"));

        List<TypeBreakdown> typeBreakdowns = byType.entrySet().stream()
                .map(entry -> new TypeBreakdown(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream()
                                .map(TransactionRecord::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                ))
                .sorted(Comparator.comparing(TypeBreakdown::volume).reversed())
                .toList();

        return new TransactionSummary(agentId, period, totalCount, totalVolume, typeBreakdowns);
    }
}