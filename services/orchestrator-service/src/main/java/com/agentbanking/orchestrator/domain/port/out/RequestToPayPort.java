package com.agentbanking.orchestrator.domain.port.out;

import java.math.BigDecimal;

public interface RequestToPayPort {

    RTPResult sendRequestToPay(String proxy, BigDecimal amount, String idempotencyKey);

    RTPStatus checkRTPStatus(String rtpReference);

    record RTPResult(boolean success, String rtpReference, String errorCode) {}
    record RTPStatus(String status, String paynetReference, String errorCode) {}
}
