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