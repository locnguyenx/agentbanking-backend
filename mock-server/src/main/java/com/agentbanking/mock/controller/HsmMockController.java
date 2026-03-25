package com.agentbanking.mock.controller;

import com.agentbanking.mock.config.MockConfig;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/mock/hsm")
public class HsmMockController {

    private final MockConfig config;

    public HsmMockController(MockConfig config) {
        this.config = config;
    }

    @PostMapping("/verify-pin")
    public Map<String, Object> verifyPin(@RequestBody Map<String, String> request) {
        boolean valid = "VALID".equals(config.getHsm().getPinValidation());
        return Map.of("valid", valid);
    }

    @PostMapping("/generate-key")
    public Map<String, Object> generateKey(@RequestBody Map<String, String> request) {
        return Map.of(
            "keyId", "HSM-KEY-" + System.currentTimeMillis(),
            "status", "GENERATED"
        );
    }
}
