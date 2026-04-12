package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatBlockResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCommitResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReleaseResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatCreditResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReverseInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.FloatReverseResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationInput;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.AccountValidationResult;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.TransactionDetailsResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "ledger-service", url = "${ledger-service.url}", path = "/internal", fallbackFactory = LedgerServiceClientFallbackFactory.class)
public interface LedgerServiceClient {

    @PostMapping("/debit")
    FloatBlockResult blockFloat(@RequestBody FloatBlockInput input);

    @PostMapping("/credit")
    FloatCommitResult commitFloat(@RequestBody FloatCommitInput input);

    @PostMapping("/release")
    FloatReleaseResult releaseFloat(@RequestBody FloatReleaseInput input);

    @PostMapping("/credit-agent")
    FloatCreditResult creditAgentFloat(@RequestBody FloatCreditInput input);

    @PostMapping("/reverse")
    FloatReverseResult reverseCreditFloat(@RequestBody FloatReverseInput input);

    @PostMapping("/validate-account")
    AccountValidationResult validateAccount(@RequestBody AccountValidationInput input);

    @GetMapping("/transactions/{transactionId}")
    TransactionDetailsResult getTransaction(@PathVariable("transactionId") UUID transactionId);
}
