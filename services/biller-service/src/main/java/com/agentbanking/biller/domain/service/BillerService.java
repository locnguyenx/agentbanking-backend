package com.agentbanking.biller.domain.service;

import com.agentbanking.biller.domain.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BillerService {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public BillPayment validateAndPay(String billerCode, String ref1, 
                                       BigDecimal amount, UUID internalTransactionId) {
        // Find biller config
        var biller = em.createQuery("SELECT b FROM BillerConfig b WHERE b.billerCode = :code AND b.active = true", 
                                     BillerConfig.class)
                       .setParameter("code", billerCode)
                       .getResultList()
                       .stream()
                       .findFirst()
                       .orElseThrow(() -> new IllegalArgumentException("Biller not found or inactive: " + billerCode));

        // Create payment record
        BillPayment payment = new BillPayment();
        payment.setPaymentId(UUID.randomUUID());
        payment.setBillerId(biller.getBillerId());
        payment.setInternalTransactionId(internalTransactionId);
        payment.setRef1(ref1);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PAID);
        payment.setReceiptNo(billerCode + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setBillerReference("BILLER-REF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCompletedAt(LocalDateTime.now());
        
        em.persist(payment);
        return payment;
    }

    @Transactional
    public TopupTransaction processTopup(String telco, String phoneNumber,
                                          BigDecimal amount, UUID internalTransactionId) {
        TopupTransaction topup = new TopupTransaction();
        topup.setTopupId(UUID.randomUUID());
        topup.setInternalTransactionId(internalTransactionId);
        topup.setTelco(telco);
        topup.setPhoneNumber(phoneNumber);
        topup.setAmount(amount);
        topup.setStatus(PaymentStatus.PAID);
        topup.setTelcoReference(telco + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        topup.setCreatedAt(LocalDateTime.now());
        topup.setCompletedAt(LocalDateTime.now());
        
        em.persist(topup);
        return topup;
    }
}
