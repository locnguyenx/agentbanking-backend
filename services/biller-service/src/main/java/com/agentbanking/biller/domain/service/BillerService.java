package com.agentbanking.biller.domain.service;

import com.agentbanking.biller.domain.model.*;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import com.agentbanking.biller.domain.port.out.EWalletTransactionRepository;
import com.agentbanking.biller.domain.port.out.EsspTransactionRepository;
import com.agentbanking.biller.domain.port.out.TopupTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class BillerService {

    private final BillerConfigRepository billerConfigRepository;
    private final BillPaymentRepository billPaymentRepository;
    private final EWalletTransactionRepository ewalletTransactionRepository;
    private final EsspTransactionRepository esspTransactionRepository;
    private final TopupTransactionRepository topupTransactionRepository;

    public BillerService(BillerConfigRepository billerConfigRepository,
                          BillPaymentRepository billPaymentRepository,
                          EWalletTransactionRepository ewalletTransactionRepository,
                          EsspTransactionRepository esspTransactionRepository,
                          TopupTransactionRepository topupTransactionRepository) {
        this.billerConfigRepository = billerConfigRepository;
        this.billPaymentRepository = billPaymentRepository;
        this.ewalletTransactionRepository = ewalletTransactionRepository;
        this.esspTransactionRepository = esspTransactionRepository;
        this.topupTransactionRepository = topupTransactionRepository;
    }

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

    public EWalletTransactionRecord processEWalletTransaction(String walletProvider, String walletId,
                                                             BigDecimal amount, UUID internalTransactionId,
                                                             boolean isWithdrawal) {
        BillerConfigRecord biller = billerConfigRepository.findByBillerCodeAndActiveTrue(walletProvider.toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Wallet provider not found or inactive: " + walletProvider));

        EWalletTransactionRecord transaction = new EWalletTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            walletProvider,
            walletId,
            amount,
            PaymentStatus.PAID,
            walletProvider + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            internalTransactionId.toString(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        return ewalletTransactionRepository.save(transaction);
    }

    public EsspTransactionRecord processEsspPurchase(BigDecimal amount, UUID internalTransactionId) {
        BillerConfigRecord biller = billerConfigRepository.findByBillerCodeAndActiveTrue("ESSP")
            .orElseThrow(() -> new IllegalArgumentException("ESSP biller not found or inactive"));

        EsspTransactionRecord transaction = new EsspTransactionRecord(
            UUID.randomUUID(),
            internalTransactionId,
            amount,
            PaymentStatus.PAID,
            "ESSP-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(),
            internalTransactionId.toString(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        return esspTransactionRepository.save(transaction);
    }
}