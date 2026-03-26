package com.agentbanking.biller.application.usecase;

import com.agentbanking.biller.domain.model.BillPaymentRecord;
import com.agentbanking.biller.domain.model.BillerConfigRecord;
import com.agentbanking.biller.domain.model.PaymentStatus;
import com.agentbanking.biller.domain.port.in.PayBillUseCase;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PayBillUseCaseImpl implements PayBillUseCase {

    private final BillerConfigRepository billerConfigRepository;
    private final BillPaymentRepository billPaymentRepository;

    public PayBillUseCaseImpl(BillerConfigRepository billerConfigRepository,
                              BillPaymentRepository billPaymentRepository) {
        this.billerConfigRepository = billerConfigRepository;
        this.billPaymentRepository = billPaymentRepository;
    }

    @Override
    @Transactional
    public PayBillResult payBill(String billerCode, String ref1, BigDecimal amount, UUID internalTransactionId) {
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

        BillPaymentRecord savedPayment = billPaymentRepository.save(payment);

        return new PayBillResult(
            savedPayment.paymentId(),
            "PAID",
            savedPayment.receiptNo(),
            savedPayment.billerReference(),
            savedPayment.amount()
        );
    }
}