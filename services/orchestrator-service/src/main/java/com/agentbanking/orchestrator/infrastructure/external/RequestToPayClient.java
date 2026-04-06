package com.agentbanking.orchestrator.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "switch-adapter-rtp", url = "${switch-adapter-service.url}")
public interface RequestToPayClient {

    @PostMapping("/internal/rtp/send")
    RTPSendResponse sendRTP(@RequestBody RTPSendRequest request);

    @GetMapping("/internal/rtp/status/{rtpReference}")
    RTPStatusResponse checkStatus(@PathVariable String rtpReference);

    record RTPSendRequest(String proxy, BigDecimal amount, String idempotencyKey) {}
    record RTPSendResponse(boolean success, String rtpReference, String errorCode) {}
    record RTPStatusResponse(String status, String paynetReference, String errorCode) {}
}
