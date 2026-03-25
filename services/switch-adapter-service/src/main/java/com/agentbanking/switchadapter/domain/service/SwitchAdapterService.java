package com.agentbanking.switchadapter.domain.service;

import com.agentbanking.switchadapter.domain.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SwitchAdapterService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SwitchTransaction processCardAuth(UUID internalTransactionId, 
                                              String pan, 
                                              double amount) {
        // Simulate ISO 8583 authorization
        SwitchTransaction txn = new SwitchTransaction();
        txn.setSwitchTxId(UUID.randomUUID());
        txn.setInternalTransactionId(internalTransactionId);
        txn.setMtType(MessageType.MT0100);
        txn.setIsoResponseCode("00"); // Approved
        txn.setSwitchReference("PAYNET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        txn.setStatus(SwitchStatus.APPROVED);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setCompletedAt(LocalDateTime.now());
        
        em.persist(txn);
        return txn;
    }

    @Transactional
    public SwitchTransaction processReversal(UUID originalTransactionId, 
                                              String originalReference,
                                              double amount) {
        // Simulate ISO 8583 reversal (MTI 0400)
        SwitchTransaction txn = new SwitchTransaction();
        txn.setSwitchTxId(UUID.randomUUID());
        txn.setInternalTransactionId(originalTransactionId);
        txn.setMtType(MessageType.MT0400);
        txn.setIsoResponseCode("00"); // Acknowledged
        txn.setSwitchReference("REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        txn.setStatus(SwitchStatus.REVERSED);
        txn.setOriginalReference(originalReference);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setCompletedAt(LocalDateTime.now());
        
        em.persist(txn);
        return txn;
    }

    @Transactional
    public SwitchTransaction processDuitNowTransfer(UUID internalTransactionId,
                                                     String proxyType,
                                                     String proxyValue,
                                                     double amount) {
        // Simulate ISO 20022 DuitNow transfer
        SwitchTransaction txn = new SwitchTransaction();
        txn.setSwitchTxId(UUID.randomUUID());
        txn.setInternalTransactionId(internalTransactionId);
        txn.setMtType(MessageType.ISO20022);
        txn.setIsoResponseCode("ACSC"); // AcceptedSettlementCompleted
        txn.setSwitchReference("DN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        txn.setStatus(SwitchStatus.APPROVED);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setCompletedAt(LocalDateTime.now());
        
        em.persist(txn);
        return txn;
    }
}
