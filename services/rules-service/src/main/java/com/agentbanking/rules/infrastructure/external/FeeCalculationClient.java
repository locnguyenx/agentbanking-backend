package com.agentbanking.rules.infrastructure.external;

import com.agentbanking.rules.domain.port.out.FeeCalculationGateway;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "rules-service", url = "${rules-service.url:http://rules-service:8081}")
public interface FeeCalculationClient extends FeeCalculationGateway {

    @PostMapping("/internal/fees/calculate")
    @Override
    FeeCalculationResult calculateFee(@RequestParam("amount") java.math.BigDecimal amount,
                                       @RequestParam("transactionType") String transactionType,
                                       @RequestParam("agentTier") String agentTier);
}
