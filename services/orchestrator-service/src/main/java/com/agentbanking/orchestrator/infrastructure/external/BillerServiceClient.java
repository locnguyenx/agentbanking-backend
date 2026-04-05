package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.BillerServicePort;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillValidationResult;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillPaymentResult;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationInput;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillNotificationResult;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.BillReversalInput;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "biller-service", url = "${biller-service.url}")
public interface BillerServiceClient {

    @PostMapping("/internal/validate-bill")
    BillValidationResult validateBill(@RequestBody BillValidationInput input);

    @PostMapping("/internal/pay-bill")
    BillPaymentResult payBill(@RequestBody BillPaymentInput input);

    @PostMapping("/internal/notify-biller")
    BillNotificationResult notifyBiller(@RequestBody BillNotificationInput input);

    @PostMapping("/internal/notify-biller-reversal")
    BillNotificationResult notifyBillerReversal(@RequestBody BillReversalInput input);
}
