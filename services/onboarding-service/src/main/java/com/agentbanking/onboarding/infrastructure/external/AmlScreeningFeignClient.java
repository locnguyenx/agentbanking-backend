package com.agentbanking.onboarding.infrastructure.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "aml-screening", url = "${aml-screening.url}")
public interface AmlScreeningFeignClient {

    @GetMapping("/screen")
    Map<String, Object> screen(
            @RequestParam("mykadNumber") String mykadNumber,
            @RequestParam("fullName") String fullName
    );
}
