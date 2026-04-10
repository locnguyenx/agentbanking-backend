package com.agentbanking.orchestrator.infrastructure.persistence.repository;

import com.agentbanking.orchestrator.domain.model.TransactionType;
import com.agentbanking.orchestrator.domain.port.out.TransactionRecordRepository;
import com.agentbanking.orchestrator.infrastructure.persistence.entity.TransactionRecordEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class TransactionRecordRepositoryImpl implements TransactionRecordRepository {

    private final TransactionRecordJpaRepository jpaRepository;

    public TransactionRecordRepositoryImpl(TransactionRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public void create(UUID id, String workflowId, TransactionType type, UUID agentId,
                       BigDecimal amount, String status) {
        TransactionRecordEntity entity = new TransactionRecordEntity();
        entity.setId(id);
        entity.setWorkflowId(workflowId);
        entity.setTransactionType(type.name());
        entity.setAgentId(agentId);
        entity.setAmount(amount);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        jpaRepository.save(entity);
    }

    @Override
    @Transactional
    public void updateStatus(String workflowId, String status, String errorCode,
                             String errorMessage, String externalReference,
                             BigDecimal customerFee, String referenceNumber) {
        jpaRepository.findByWorkflowId(workflowId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(errorMessage);
            entity.setExternalReference(externalReference);
            entity.setCustomerFee(customerFee);
            entity.setReferenceNumber(referenceNumber);
            entity.setUpdatedAt(Instant.now());
            if ("COMPLETED".equals(status)) {
                entity.setCompletedAt(Instant.now());
            }
            jpaRepository.save(entity);
        });
    }

    @Override
    public Optional<TransactionRecordDTO> findByWorkflowId(String workflowId) {
        return jpaRepository.findByWorkflowId(workflowId).map(this::toDTO);
    }

    @Override
    public List<TransactionRecordDTO> findStuckTransactions() {
        return jpaRepository.findStuckTransactions()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionRecordDTO> findAllWithFilters(
            Instant fromDate,
            Instant toDate,
            UUID agentId,
            String agentCode,
            String transactionType,
            String status,
            int page,
            int size
    ) {
        Specification<TransactionRecordEntity> spec = Specification.where(null);

        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
        if (agentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("agentId"), agentId));
        }
        if (transactionType != null && !transactionType.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("transactionType"), transactionType));
        }
        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return jpaRepository.findAll(spec, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .getContent()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private TransactionRecordDTO toDTO(TransactionRecordEntity entity) {
        return new TransactionRecordDTO(
                entity.getId(),
                entity.getWorkflowId(),
                TransactionType.valueOf(entity.getTransactionType()),
                entity.getAgentId(),
                entity.getAmount(),
                entity.getCustomerFee(),
                entity.getStatus(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getExternalReference(),
                entity.getReferenceNumber(),
                entity.getPendingReason(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
