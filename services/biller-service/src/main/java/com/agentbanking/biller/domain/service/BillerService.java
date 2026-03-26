package com.agentbanking.biller.domain.service;

import com.agentbanking.biller.domain.model.*;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BillerService {

    private final BillerConfigRepository billerConfigRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final TopupTransactionRepository topupTransactionRepository;

    public BillerService(BillerConfigRepository billerConfigRepository, 
                         BillPaymentRepository billPaymentRepository,
                         TopupTransactionRepository topupTransactionRepository) {
        this.billerConfigRepository = billerConfigRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.topupTransactionRepository = topupTransactionRepository;
    }

    @Transactional
    public BillPaymentRecord validateAndPay(String billerCode, String ref1, 
                                       BigDecimal amount, UUID internalTransactionId) {
        BillerConfigRecord biller = billerConfigRepository.findByBillerCodeAndActiveTrue(billerCode)
            .orElseThrow(() -> new IllegalArgumentException("Biller not found or inactive: " + billerCode));

        BillPaymentRecord payment = new BillPaymentRecord(
            UUID.randomUUID(),
            biller.billerId(),
            internalTransactionId,
            ref1,
            null,
            amount,
            PaymentStatus.PAID,
            billerCode + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "BILLER-REF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        return billPaymentRepository.save(payment);
    }

    @Transactional
    public TopupTransaction processTopup(String telco, String phoneNumber,
                                  BigDecimal amount, UUID internalTransactionId) {
        TopupTransactionRecord record = new TopupTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            telco,
            phoneNumber,
            amount,
            PaymentStatus.PAID,
            telco + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        TopupTransactionRecord saved = topupTransactionRepository.save(record);
        
        TopupTransaction topup = new TopupTransaction();
        topup.setTopupId(saved.topupId());
        topup.setInternalTransactionId(saved.internalTransactionId());
        topup.setTelco(saved.telco());
        topup.setPhoneNumber(saved.phoneNumber());
        topup.setAmount(saved.amount());
        topup.setStatus(saved.status());
        topup.setTelcoReference(saved.telcoReference());
        topup.setCreatedAt(saved.createdAt());
        topup.setCompletedAt(saved.completedAt());
        
        return topup;
    }
}