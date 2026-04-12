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
                             BigDecimal customerFee, String referenceNumber, 
                             String pendingReason, String errorDetails) {
        jpaRepository.findByWorkflowId(workflowId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(errorMessage);
            entity.setExternalReference(externalReference);
            entity.setCustomerFee(customerFee);
            entity.setReferenceNumber(referenceNumber);
            entity.setPendingReason(truncateTo100(pendingReason));
            entity.setErrorDetails(truncateTo4000(errorDetails));
            entity.setUpdatedAt(Instant.now());
            if ("COMPLETED".equals(status)) {
                entity.setCompletedAt(Instant.now());
            }
            jpaRepository.save(entity);
        });
    }

    private String truncateTo100(String value) {
        if (value == null) return null;
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private String truncateTo4000(String value) {
        if (value == null) return null;
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }

    @Override
    public Optional<TransactionRecordDTO> findByWorkflowId(String workflowId) {
        // First try exact match
        var exactMatch = jpaRepository.findByWorkflowId(workflowId);
        if (exactMatch.isPresent()) {
            return exactMatch.map(this::toDTO);
        }
        // Then try partial match (for prefixed workflow IDs like "e2e-stp-duitnow-uuid")
        String uuidStr = extractUuidFromWorkflowId(workflowId);
        return jpaRepository.findByWorkflowIdContaining(uuidStr).map(this::toDTO);
    }
    
    private String extractUuidFromWorkflowId(String workflowId) {
        if (workflowId == null || workflowId.isEmpty()) {
            return workflowId;
        }
        if (workflowId.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")) {
            return workflowId;
        }
        String[] parts = workflowId.split("-");
        if (parts.length > 5) {
            StringBuilder sb = new StringBuilder();
            for (int i = parts.length - 5; i < parts.length; i++) {
                if (i > parts.length - 5) sb.append("-");
                sb.append(parts[i]);
            }
            return sb.toString();
        }
        return workflowId;
    }

    @Override
    public Optional<TransactionRecordDTO> findByWorkflowIdContaining(String uuid) {
        return jpaRepository.findByWorkflowIdContaining(uuid).map(this::toDTO);
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
                entity.getErrorDetails(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
