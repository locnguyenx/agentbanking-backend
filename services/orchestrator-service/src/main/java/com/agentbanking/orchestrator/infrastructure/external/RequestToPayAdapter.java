package com.agentbanking.orchestrator.infrastructure.external;

import com.agentbanking.orchestrator.domain.port.out.RequestToPayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class RequestToPayAdapter implements RequestToPayPort {

    private static final Logger log = LoggerFactory.getLogger(RequestToPayAdapter.class);

    private final RequestToPayClient client;

    public RequestToPayAdapter(RequestToPayClient client) {
        this.client = client;
    }

    @Override
    public RTPResult sendRequestToPay(String proxy, BigDecimal amount, String idempotencyKey) {
        log.info("Sending RTP to proxy {}: {}", proxy, amount);
        var response = client.sendRTP(new RequestToPayClient.RTPSendRequest(proxy, amount, idempotencyKey));
        return new RTPResult(response.success(), response.rtpReference(), response.errorCode());
    }

    @Override
    public RTPStatus checkRTPStatus(String rtpReference) {
        log.info("Checking RTP status: {}", rtpReference);
        var response = client.checkStatus(rtpReference);
        return new RTPStatus(response.status(), response.paynetReference(), response.errorCode());
    }
}
