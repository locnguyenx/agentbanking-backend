package com.agentbanking.mock.controller;

import com.agentbanking.mock.config.MockConfig;
import com.agentbanking.mock.data.TestDataService;
import com.agentbanking.mock.model.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock/jpn")
public class JpnMockController {

    private final TestDataService dataService;
    private final MockConfig config;

    public JpnMockController(TestDataService dataService, MockConfig config) {
        this.dataService = dataService;
        this.config = config;
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody JpnVerifyRequest request) {
        var citizen = dataService.findCitizenByMykad(request.mykad());
        if (citizen == null) {
            return Map.of("status", "NOT_FOUND");
        }
        int age = Period.between(LocalDate.parse(citizen.dateOfBirth()), LocalDate.now()).getYears();
        return Map.of(
            "status", "FOUND",
            "fullName", citizen.fullName(),
            "dateOfBirth", citizen.dateOfBirth(),
            "age", age,
            "amlStatus", citizen.amlStatus()
        );
    }

    @PostMapping("/biometric")
    public Map<String, Object> biometric(@RequestBody Map<String, String> request) {
        return Map.of(
            "verificationId", request.get("verificationId"),
            "match", config.getJpn().getDefaultMatch()
        );
    }
}
