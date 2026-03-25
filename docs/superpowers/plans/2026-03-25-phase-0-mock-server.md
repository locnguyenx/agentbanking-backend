# Phase 0: Mock Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a standalone Spring Boot mock server simulating all Tier 5 downstream systems (PayNet, JPN, HSM, Billers) so frontend and backend teams can develop and test without sandbox keys.

**Architecture:** Single Spring Boot app with configurable responses per simulated system. Exposes REST endpoints that mimic real downstream APIs. Supports latency simulation, configurable approve/decline patterns, and test data seeding.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring MVC, Jackson, YAML config

**Specs:** `docs/superpowers/specs/agent-banking-platform/` — BRD (§4 entities), BDD (§2-§7 edge cases), Design (§10 mock server)

---

## File Structure

```
mock-server/
├── build.gradle
├── src/main/java/com/agentbanking/mock/
│   ├── MockServerApplication.java
│   ├── config/
│   │   └── MockConfig.java                        # YAML config binding
│   ├── controller/
│   │   ├── PaynetMockController.java              # ISO 8583 + ISO 20022
│   │   ├── JpnMockController.java                 # MyKad + biometric
│   │   ├── HsmMockController.java                 # PIN verification
│   │   └── BillerMockController.java              # JomPAY, ASTRO, TM, EPF
│   ├── model/
│   │   ├── PaynetAuthRequest.java                 # Request DTOs
│   │   ├── PaynetAuthResponse.java                # Response DTOs
│   │   ├── JpnVerifyRequest.java
│   │   ├── JpnVerifyResponse.java
│   │   ├── HsmPinRequest.java
│   │   ├── HsmPinResponse.java
│   │   ├── BillerValidateRequest.java
│   │   ├── BillerValidateResponse.java
│   │   └── BillerPayRequest.java
│   └── data/
│       └── TestDataService.java                   # Loads fixtures from JSON
├── src/main/resources/
│   ├── application.yaml                           # Default config
│   ├── application-mock.yaml                      # Mock-specific overrides
│   └── mock-data/
│       ├── citizens.json                          # 100 sample MyKad records
│       ├── agents.json                            # 20 sample agents
│       └── billers.json                           # Biller reference data
└── src/test/java/com/agentbanking/mock/
    ├── controller/
    │   ├── PaynetMockControllerTest.java
    │   ├── JpnMockControllerTest.java
    │   ├── HsmMockControllerTest.java
    │   └── BillerMockControllerTest.java
    └── data/
        └── TestDataServiceTest.java
```

---

## Tasks

### Task 1: Project Setup [DONE]

**BDD Scenarios:** Foundation for all downstream simulation
**BRD Requirements:** C-1 (Java 21), C-2 (Spring Boot 3.x), Design §10

**Files:**
- Create: `mock-server/build.gradle`
- Create: `mock-server/src/main/java/com/agentbanking/mock/MockServerApplication.java`
- Create: `mock-server/src/main/resources/application.yaml`

- [ ] **Step 1: Write build.gradle**

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.5'
    id 'io.spring.dependency-management' version '1.1.5'
}

group = 'com.agentbanking'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '21'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.fasterxml.jackson.core:jackson-databind'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Write MockServerApplication.java**

```java
package com.agentbanking.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MockServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockServerApplication.class, args);
    }
}
```

- [ ] **Step 3: Write application.yaml**

```yaml
server:
  port: 8090

spring:
  application:
    name: mock-server

mock:
  paynet:
    default-response: APPROVE
    decline-codes: ["51", "54", "55"]
    latency-ms: 200
  jpn:
    default-match: MATCH
    aml-default: CLEAN
  hsm:
    pin-validation: VALID
  billers:
    default-validation: VALID
    latency-ms: 500
```

- [ ] **Step 4: Verify project compiles**

Run: `cd mock-server && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Verify app starts**

Run: `cd mock-server && ./gradlew bootRun` (background)
Expected: App starts on port 8090 with no errors

- [ ] **Step 6: Commit**

```bash
git add mock-server/
git commit -m "feat(mock-server): scaffold Spring Boot project with config"
```

---

### Task 2: Mock Config Binding [DONE]

**BDD Scenarios:** Foundation for configurable responses
**BRD Requirements:** Design §10 (configuration)

**Files:**
- Create: `mock-server/src/main/java/com/agentbanking/mock/config/MockConfig.java`
- Test: `mock-server/src/test/java/com/agentbanking/mock/config/MockConfigTest.java`

- [ ] **Step 1: Write failing test for config binding**

```java
package com.agentbanking.mock.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MockConfigTest {

    @Autowired
    private MockConfig config;

    @Test
    void shouldLoadPaynetConfig() {
        assertEquals("APPROVE", config.getPaynet().getDefaultResponse());
        assertEquals(200, config.getPaynet().getLatencyMs());
    }

    @Test
    void shouldLoadJpnConfig() {
        assertEquals("MATCH", config.getJpn().getDefaultMatch());
        assertEquals("CLEAN", config.getJpn().getAmlDefault());
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `cd mock-server && ./gradlew test --tests MockConfigTest`
Expected: FAIL — MockConfig class not found

- [ ] **Step 3: Write MockConfig.java**

```java
package com.agentbanking.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "mock")
public class MockConfig {
    private PaynetConfig paynet = new PaynetConfig();
    private JpnConfig jpn = new JpnConfig();
    private HsmConfig hsm = new HsmConfig();
    private BillerConfig billers = new BillerConfig();

    // Getters and setters
    public PaynetConfig getPaynet() { return paynet; }
    public void setPaynet(PaynetConfig paynet) { this.paynet = paynet; }
    public JpnConfig getJpn() { return jpn; }
    public void setJpn(JpnConfig jpn) { this.jpn = jpn; }
    public HsmConfig getHsm() { return hsm; }
    public void setHsm(HsmConfig hsm) { this.hsm = hsm; }
    public BillerConfig getBillers() { return billers; }
    public void setBillers(BillerConfig billers) { this.billers = billers; }

    public static class PaynetConfig {
        private String defaultResponse = "APPROVE";
        private List<String> declineCodes = List.of();
        private int latencyMs = 200;
        // Getters/setters
        public String getDefaultResponse() { return defaultResponse; }
        public void setDefaultResponse(String defaultResponse) { this.defaultResponse = defaultResponse; }
        public List<String> getDeclineCodes() { return declineCodes; }
        public void setDeclineCodes(List<String> declineCodes) { this.declineCodes = declineCodes; }
        public int getLatencyMs() { return latencyMs; }
        public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    }

    public static class JpnConfig {
        private String defaultMatch = "MATCH";
        private String amlDefault = "CLEAN";
        // Getters/setters
        public String getDefaultMatch() { return defaultMatch; }
        public void setDefaultMatch(String defaultMatch) { this.defaultMatch = defaultMatch; }
        public String getAmlDefault() { return amlDefault; }
        public void setAmlDefault(String amlDefault) { this.amlDefault = amlDefault; }
    }

    public static class HsmConfig {
        private String pinValidation = "VALID";
        // Getters/setters
        public String getPinValidation() { return pinValidation; }
        public void setPinValidation(String pinValidation) { this.pinValidation = pinValidation; }
    }

    public static class BillerConfig {
        private String defaultValidation = "VALID";
        private int latencyMs = 500;
        // Getters/setters
        public String getDefaultValidation() { return defaultValidation; }
        public void setDefaultValidation(String defaultValidation) { this.defaultValidation = defaultValidation; }
        public int getLatencyMs() { return latencyMs; }
        public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    }
}
```

- [ ] **Step 4: Run test — verify PASS**

Run: `cd mock-server && ./gradlew test --tests MockConfigTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add mock-server/src/main/java/.../config/ mock-server/src/test/java/.../config/
git commit -m "feat(mock-server): add config binding for mock responses"
```

---

### Task 3: Test Data Service [DONE]

**BDD Scenarios:** Supports all scenarios requiring mock citizen/agent/biller data
**BRD Requirements:** Design §10 (test data seeding)

**Files:**
- Create: `mock-server/src/main/resources/mock-data/citizens.json`
- Create: `mock-server/src/main/resources/mock-data/agents.json`
- Create: `mock-server/src/main/resources/mock-data/billers.json`
- Create: `mock-server/src/main/java/com/agentbanking/mock/data/TestDataService.java`
- Test: `mock-server/src/test/java/com/agentbanking/mock/data/TestDataServiceTest.java`

- [ ] **Step 1: Create citizens.json**

```json
[
  {"mykad": "123456789012", "fullName": "AHMAD BIN ABU", "dateOfBirth": "1990-05-15", "amlStatus": "CLEAN"},
  {"mykad": "990101011234", "fullName": "SITI BINTI ALI", "dateOfBirth": "1999-01-01", "amlStatus": "CLEAN"},
  {"mykad": "880101015555", "fullName": "TAN WEI MING", "dateOfBirth": "1988-01-01", "amlStatus": "FLAGGED"},
  {"mykad": "050101019999", "fullName": "YOUNG CUSTOMER", "dateOfBirth": "2005-01-01", "amlStatus": "CLEAN"}
]
```

- [ ] **Step 2: Create agents.json**

```json
[
  {"agentCode": "AGT-001", "tier": "MICRO", "status": "ACTIVE", "gpsLat": 3.1390, "gpsLng": 101.6869},
  {"agentCode": "AGT-002", "tier": "STANDARD", "status": "ACTIVE", "gpsLat": 3.1400, "gpsLng": 101.6870},
  {"agentCode": "AGT-003", "tier": "PREMIER", "status": "ACTIVE", "gpsLat": 3.1395, "gpsLng": 101.6865},
  {"agentCode": "AGT-004", "tier": "STANDARD", "status": "SUSPENDED", "gpsLat": 3.1390, "gpsLng": 101.6869}
]
```

- [ ] **Step 3: Create billers.json**

```json
[
  {"billerCode": "JOMPAY", "name": "JomPAY", "validRefs": ["INV-12345", "INV-67890"]},
  {"billerCode": "ASTRO", "name": "ASTRO RPN", "validRefs": ["AST-001", "AST-002"]},
  {"billerCode": "TM", "name": "TM RPN", "validRefs": ["TM-001", "TM-002"]},
  {"billerCode": "EPF", "name": "EPF i-SARAAN", "validRefs": ["EPF-001", "EPF-002"]}
]
```

- [ ] **Step 4: Write failing test for TestDataService**

```java
package com.agentbanking.mock.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TestDataServiceTest {

    @Autowired
    private TestDataService service;

    @Test
    void shouldLoadCitizens() {
        assertNotNull(service.findCitizenByMykad("123456789012"));
        assertEquals("AHMAD BIN ABU", service.findCitizenByMykad("123456789012").fullName());
    }

    @Test
    void shouldReturnNullForUnknownMykad() {
        assertNull(service.findCitizenByMykad("000000000000"));
    }

    @Test
    void shouldLoadAgents() {
        assertNotNull(service.findAgentByCode("AGT-001"));
        assertEquals("MICRO", service.findAgentByCode("AGT-001").tier());
    }

    @Test
    void shouldCheckBillerRef() {
        assertTrue(service.isValidBillerRef("JOMPAY", "INV-12345"));
        assertFalse(service.isValidBillerRef("JOMPAY", "INVALID-REF"));
    }
}
```

- [ ] **Step 5: Run test — verify FAIL**

Run: `cd mock-server && ./gradlew test --tests TestDataServiceTest`
Expected: FAIL — TestDataService not found

- [ ] **Step 6: Write TestDataService.java**

```java
package com.agentbanking.mock.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
public class TestDataService {

    private List<Citizen> citizens = new ArrayList<>();
    private List<Agent> agents = new ArrayList<>();
    private List<Biller> billers = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void load() throws IOException {
        citizens = mapper.readValue(new ClassPathResource("mock-data/citizens.json").getInputStream(),
            new TypeReference<>() {});
        agents = mapper.readValue(new ClassPathResource("mock-data/agents.json").getInputStream(),
            new TypeReference<>() {});
        billers = mapper.readValue(new ClassPathResource("mock-data/billers.json").getInputStream(),
            new TypeReference<>() {});
    }

    public Citizen findCitizenByMykad(String mykad) {
        return citizens.stream().filter(c -> c.mykad().equals(mykad)).findFirst().orElse(null);
    }

    public Agent findAgentByCode(String code) {
        return agents.stream().filter(a -> a.agentCode().equals(code)).findFirst().orElse(null);
    }

    public boolean isValidBillerRef(String billerCode, String ref) {
        return billers.stream()
            .filter(b -> b.billerCode().equals(billerCode))
            .findFirst()
            .map(b -> b.validRefs().contains(ref))
            .orElse(false);
    }

    public record Citizen(String mykad, String fullName, String dateOfBirth, String amlStatus) {}
    public record Agent(String agentCode, String tier, String status, double gpsLat, double gpsLng) {}
    public record Biller(String billerCode, String name, List<String> validRefs) {}
}
```

- [ ] **Step 7: Run test — verify PASS**

Run: `cd mock-server && ./gradlew test --tests TestDataServiceTest`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add mock-server/src/main/resources/mock-data/ mock-server/src/main/java/.../data/ mock-server/src/test/java/.../data/
git commit -m "feat(mock-server): add test data service with citizen/agent/biller fixtures"
```

---

### Task 4: JPN Mock Controller [DONE]

### Task 5: HSM Mock Controller [DONE]

### Task 6: PayNet Mock Controller [DONE]

### Task 7: Biller Mock Controller [DONE]

### Task 8: Latency Simulation [DONE]

### Task 9: Integration Verification [IN_PROGRESS]

**BDD Scenarios:** BDD-O01 (MyKad verification), BDD-O01-EC-01 (not found), BDD-O01-EC-03 (under 18), BDD-O02 (biometric match), BDD-O02-EC-01 (no match)
**BRD Requirements:** US-O01, US-O02, FR-6.1, FR-6.2, FR-6.3, FR-6.4

**Files:**
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/JpnVerifyRequest.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/JpnVerifyResponse.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/controller/JpnMockController.java`
- Test: `mock-server/src/test/java/com/agentbanking/mock/controller/JpnMockControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.agentbanking.mock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JpnMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnCitizenDataForValidMykad() throws Exception {
        mockMvc.perform(post("/mock/jpn/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykad\":\"123456789012\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("AHMAD BIN ABU"))
            .andExpect(jsonPath("$.amlStatus").value("CLEAN"))
            .andExpect(jsonPath("$.age").isNumber());
    }

    @Test
    void shouldReturnNotFoundForUnknownMykad() throws Exception {
        mockMvc.perform(post("/mock/jpn/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mykad\":\"000000000000\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnMatchForBiometric() throws Exception {
        mockMvc.perform(post("/mock/jpn/biometric")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"verificationId\":\"KYC-001\",\"biometricData\":\"blob\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.match").value("MATCH"));
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `cd mock-server && ./gradlew test --tests JpnMockControllerTest`
Expected: FAIL — controller not found

- [ ] **Step 3: Write model DTOs**

```java
// JpnVerifyRequest.java
package com.agentbanking.mock.model;
public record JpnVerifyRequest(String mykad) {}

// JpnVerifyResponse.java
package com.agentbanking.mock.model;
public record JpnVerifyResponse(String status, String fullName, String dateOfBirth, int age, String amlStatus) {}
```

- [ ] **Step 4: Write JpnMockController.java**

```java
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
```

- [ ] **Step 5: Run test — verify PASS**

Run: `cd mock-server && ./gradlew test --tests JpnMockControllerTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add mock-server/src/main/java/.../model/ mock-server/src/main/java/.../controller/JpnMockController.java mock-server/src/test/java/.../controller/JpnMockControllerTest.java
git commit -m "feat(mock-server): add JPN mock controller for MyKad and biometric"
```

---

### Task 5: HSM Mock Controller

**BDD Scenarios:** BDD-W01-EC-01 (invalid PIN), BDD-W01 (successful PIN verification)
**BRD Requirements:** FR-3.1, NFR-3.2 (PIN encryption)

**Files:**
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/HsmPinRequest.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/HsmPinResponse.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/controller/HsmMockController.java`
- Test: `mock-server/src/test/java/com/agentbanking/mock/controller/HsmMockControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.agentbanking.mock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HsmMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnValidPinVerification() throws Exception {
        mockMvc.perform(post("/mock/hsm/verify-pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pinBlock\":\"encrypted-pin-blob\",\"pan\":\"4111111111111111\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `cd mock-server && ./gradlew test --tests HsmMockControllerTest`
Expected: FAIL

- [ ] **Step 3: Write HsmMockController.java**

```java
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
```

- [ ] **Step 4: Run test — verify PASS**

Run: `cd mock-server && ./gradlew test --tests HsmMockControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add mock-server/src/main/java/.../model/Hsm*.java mock-server/src/main/java/.../controller/HsmMockController.java mock-server/src/test/java/.../controller/HsmMockControllerTest.java
git commit -m "feat(mock-server): add HSM mock controller for PIN verification"
```

---

### Task 6: PayNet Mock Controller

**BDD Scenarios:** BDD-W01 (card auth), BDD-W01-EC-01 (decline), BDD-W01-EC-02 (reversal)
**BRD Requirements:** FR-3.1, FR-3.4, FR-9.1, FR-9.2

**Files:**
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/PaynetAuthRequest.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/PaynetAuthResponse.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/controller/PaynetMockController.java`
- Test: `mock-server/src/test/java/com/agentbanking/mock/controller/PaynetMockControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.agentbanking.mock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PaynetMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApproveCardAuthByDefault() throws Exception {
        mockMvc.perform(post("/mock/paynet/iso8583/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pan\":\"4111111111111111\",\"amount\":500.00,\"merchantCode\":\"AGT-001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.responseCode").value("00"))
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.referenceId").isNotEmpty());
    }

    @Test
    void shouldAcknowledgeReversal() throws Exception {
        mockMvc.perform(post("/mock/paynet/iso8583/reversal")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"originalReferenceId\":\"REF-123\",\"amount\":500.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void shouldApproveDuitNowTransfer() throws Exception {
        mockMvc.perform(post("/mock/paynet/iso20022/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proxyType\":\"MOBILE\",\"proxyValue\":\"0123456789\",\"amount\":1000.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SETTLED"));
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `cd mock-server && ./gradlew test --tests PaynetMockControllerTest`
Expected: FAIL

- [ ] **Step 3: Write PaynetMockController.java**

```java
package com.agentbanking.mock.controller;

import com.agentbanking.mock.config.MockConfig;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock/paynet")
public class PaynetMockController {

    private final MockConfig config;

    public PaynetMockController(MockConfig config) {
        this.config = config;
    }

    @PostMapping("/iso8583/auth")
    public Map<String, Object> cardAuth(@RequestBody Map<String, Object> request) {
        String defaultResponse = config.getPaynet().getDefaultResponse();
        if ("APPROVE".equals(defaultResponse)) {
            return Map.of(
                "responseCode", "00",
                "status", "APPROVED",
                "referenceId", "PAYNET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
            );
        } else {
            return Map.of(
                "responseCode", config.getPaynet().getDeclineCodes().get(0),
                "status", "DECLINED",
                "reason", "Insufficient funds"
            );
        }
    }

    @PostMapping("/iso8583/reversal")
    public Map<String, Object> reversal(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "ACKNOWLEDGED",
            "referenceId", "REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    @PostMapping("/iso20022/transfer")
    public Map<String, Object> duitNowTransfer(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "SETTLED",
            "transactionId", "DN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "settlementTime", System.currentTimeMillis()
        );
    }
}
```

- [ ] **Step 4: Run test — verify PASS**

Run: `cd mock-server && ./gradlew test --tests PaynetMockControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add mock-server/src/main/java/.../model/Paynet*.java mock-server/src/main/java/.../controller/PaynetMockController.java mock-server/src/test/java/.../controller/PaynetMockControllerTest.java
git commit -m "feat(mock-server): add PayNet mock controller for card auth, reversal, DuitNow"
```

---

### Task 7: Biller Mock Controller

**BDD Scenarios:** BDD-B01 (JomPAY), BDD-B01-EC-01 (invalid ref), BDD-T01 (CELCOM top-up)
**BRD Requirements:** FR-7.1, FR-7.5, FR-8.1, FR-8.3

**Files:**
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/BillerValidateRequest.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/model/BillerPayRequest.java`
- Create: `mock-server/src/main/java/com/agentbanking/mock/controller/BillerMockController.java`
- Test: `mock-server/src/test/java/com/agentbanking/mock/controller/BillerMockControllerTest.java`

- [ ] **Step 1: Write failing tests**

```java
package com.agentbanking.mock.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BillerMockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldValidateJompayRef() throws Exception {
        mockMvc.perform(post("/mock/billers/jompay/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref1\":\"INV-12345\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.amount").isNumber());
    }

    @Test
    void shouldRejectInvalidRef() throws Exception {
        mockMvc.perform(post("/mock/billers/jompay/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref1\":\"INVALID-REF\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void shouldApproveBillerPayment() throws Exception {
        mockMvc.perform(post("/mock/billers/jompay/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ref1\":\"INV-12345\",\"amount\":150.00}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PAID"));
    }
}
```

- [ ] **Step 2: Run test — verify FAIL**

Run: `cd mock-server && ./gradlew test --tests BillerMockControllerTest`
Expected: FAIL

- [ ] **Step 3: Write BillerMockController.java**

```java
package com.agentbanking.mock.controller;

import com.agentbanking.mock.data.TestDataService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock/billers")
public class BillerMockController {

    private final TestDataService dataService;

    public BillerMockController(TestDataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/{billerCode}/validate")
    public Map<String, Object> validate(@PathVariable String billerCode, @RequestBody Map<String, String> request) {
        String ref1 = request.get("ref1");
        boolean valid = dataService.isValidBillerRef(billerCode.toUpperCase(), ref1);
        if (valid) {
            return Map.of("valid", true, "amount", new BigDecimal("150.00"), "customerName", "MOCK CUSTOMER");
        }
        return Map.of("valid", false, "reason", "Reference not found");
    }

    @PostMapping("/{billerCode}/pay")
    public Map<String, Object> pay(@PathVariable String billerCode, @RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "PAID",
            "receiptNo", billerCode.toUpperCase() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
            "amount", request.get("amount")
        );
    }

    @PostMapping("/celcom/topup")
    public Map<String, Object> celcomTopup(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "COMPLETED",
            "transactionId", "CEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }

    @PostMapping("/m1/topup")
    public Map<String, Object> m1Topup(@RequestBody Map<String, Object> request) {
        return Map.of(
            "status", "COMPLETED",
            "transactionId", "M1-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
```

- [ ] **Step 4: Run test — verify PASS**

Run: `cd mock-server && ./gradlew test --tests BillerMockControllerTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add mock-server/src/main/java/.../model/Biller*.java mock-server/src/main/java/.../controller/BillerMockController.java mock-server/src/test/java/.../controller/BillerMockControllerTest.java
git commit -m "feat(mock-server): add Biller mock controller for JomPAY, ASTRO, TM, EPF, telco top-up"
```

---

### Task 8: Latency Simulation

**BDD Scenarios:** BDD-G01-EC-03 (service unavailable), BDD-W01-EC-03 (network drop)
**BRD Requirements:** NFR-2.2 (circuit breaker), NFR-2.3 (Store & Forward)

**Files:**
- Modify: All controllers (add `Thread.sleep()` based on config latency)

- [ ] **Step 1: Add latency to PaynetMockController**

```java
private void simulateLatency() {
    try {
        Thread.sleep(config.getPaynet().getLatencyMs());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

Call `simulateLatency()` at start of each endpoint method.

- [ ] **Step 2: Add latency to BillerMockController**

Same pattern, using `config.getBillers().getLatencyMs()`.

- [ ] **Step 3: Verify latency works**

Run: `cd mock-server && ./gradlew bootRun` then `time curl -X POST http://localhost:8090/mock/paynet/iso8583/auth -H 'Content-Type: application/json' -d '{}'`
Expected: Response takes ~200ms

- [ ] **Step 4: Commit**

```bash
git add mock-server/src/main/java/.../controller/
git commit -m "feat(mock-server): add configurable latency simulation"
```

---

### Task 9: Integration Verification

**BDD Scenarios:** End-to-end mock server validation
**BRD Requirements:** All Phase 0 acceptance

- [ ] **Step 1: Start mock server**

Run: `cd mock-server && ./gradlew bootRun`

- [ ] **Step 2: Test all endpoints manually**

```bash
# JPN verify
curl -X POST http://localhost:8090/mock/jpn/verify -H 'Content-Type: application/json' -d '{"mykad":"123456789012"}'

# JPN biometric
curl -X POST http://localhost:8090/mock/jpn/biometric -H 'Content-Type: application/json' -d '{"verificationId":"KYC-001","biometricData":"blob"}'

# HSM PIN
curl -X POST http://localhost:8090/mock/hsm/verify-pin -H 'Content-Type: application/json' -d '{"pinBlock":"blob","pan":"4111111111111111"}'

# PayNet auth
curl -X POST http://localhost:8090/mock/paynet/iso8583/auth -H 'Content-Type: application/json' -d '{"pan":"4111111111111111","amount":500}'

# PayNet reversal
curl -X POST http://localhost:8090/mock/paynet/iso8583/reversal -H 'Content-Type: application/json' -d '{"originalReferenceId":"REF-123","amount":500}'

# DuitNow
curl -X POST http://localhost:8090/mock/paynet/iso20022/transfer -H 'Content-Type: application/json' -d '{"proxyType":"MOBILE","proxyValue":"0123456789","amount":1000}'

# JomPAY validate
curl -X POST http://localhost:8090/mock/billers/jompay/validate -H 'Content-Type: application/json' -d '{"ref1":"INV-12345"}'

# JomPAY pay
curl -X POST http://localhost:8090/mock/billers/jompay/pay -H 'Content-Type: application/json' -d '{"ref1":"INV-12345","amount":150}'
```

- [ ] **Step 3: Verify all responses match expected BDD behavior**

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "test(mock-server): verify all mock endpoints against BDD scenarios"
```

---

## Summary

| Task | Component | BDD Coverage |
|------|-----------|-------------|
| 1 | Project Setup | Foundation |
| 2 | Config Binding | Design §10 |
| 3 | Test Data Service | All scenarios with mock data |
| 4 | JPN Mock | BDD-O01, O01-EC-01, O01-EC-03, O02, O02-EC-01 |
| 5 | HSM Mock | BDD-W01, W01-EC-01 |
| 6 | PayNet Mock | BDD-W01, W01-EC-01, W01-EC-02, DN01 |
| 7 | Biller Mock | BDD-B01, B01-EC-01, T01 |
| 8 | Latency Simulation | BDD-G01-EC-03, W01-EC-03 |
| 9 | Integration Verification | All |
