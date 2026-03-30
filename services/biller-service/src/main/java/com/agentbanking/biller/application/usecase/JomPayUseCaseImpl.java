package com.agentbanking.biller.application.usecase;

import com.agentbanking.biller.domain.model.BillPaymentRecord;
import com.agentbanking.biller.domain.model.BillerConfigRecord;
import com.agentbanking.biller.domain.model.PaymentStatus;
import com.agentbanking.biller.domain.port.in.JomPayUseCase;
import com.agentbanking.biller.domain.port.out.BillerConfigRepository;
import com.agentbanking.biller.domain.port.out.BillPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class JomPayUseCaseImpl implements JomPayUseCase {

    private final BillerConfigRepository billerConfigRepository;
    private final BillPaymentRepository billPaymentRepository;

    public JomPayUseCaseImpl(BillerConfigRepository billerConfigRepository,
                             BillPaymentRepository billPaymentRepository) {
        this.billerConfigRepository = billerConfigRepository;
        this.billPaymentRepository = billPaymentRepository;
    }

    @Override
    @Transactional
    public JomPayResult processJomPay(JomPayCommand command) {
        BillerConfigRecord biller = billerConfigRepository.findByBillerCodeAndActiveTrue(command.billerCode())
            .orElseThrow(() -> new IllegalArgumentException("Biller not found or inactive: " + command.billerCode()));

        BillPaymentRecord payment = new BillPaymentRecord(
            UUID.randomUUID(),
            biller.billerId(),
            command.internalTransactionId(),
            command.ref1(),
            command.ref2(),
            command.amount(),
            PaymentStatus.PAID,
            "JOMPAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "JP-REF-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        BillPaymentRecord savedPayment = billPaymentRepository.save(payment);

        return new JomPayResult(
            savedPayment.paymentId(),
            "PAID",
            savedPayment.receiptNo(),
            savedPayment.billerReference(),
            savedPayment.amount()
        );
    }
}
