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