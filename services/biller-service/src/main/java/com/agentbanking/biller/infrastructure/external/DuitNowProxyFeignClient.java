package com.agentbanking.biller.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Feign client to call the mock server for DuitNow proxy resolution.
 */
@FeignClient(name = "paynet-proxy-client", url = "${app.external.paynet.url:http://mock-server:8090/mock/paynet}")
public interface DuitNowProxyFeignClient {

    @GetMapping("/proxy/resolve")
    Map<String, String> resolveProxy(@RequestParam("proxyId") String proxyId, @RequestParam("proxyType") String proxyType);
}
