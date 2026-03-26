package com.agentbanking.switchadapter.domain.service;

import com.agentbanking.switchadapter.domain.model.MessageType;
import com.agentbanking.switchadapter.domain.model.SwitchStatus;
import com.agentbanking.switchadapter.domain.model.SwitchTransactionRecord;
import com.agentbanking.switchadapter.domain.port.out.SwitchTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SwitchAdapterServiceTest {

    @Mock
    private SwitchTransactionRepository repository;

    private SwitchAdapterService service;

    @BeforeEach
    void setUp() {
        service = new SwitchAdapterService(repository);
    }

    @Test
    void processCardAuth_withValidRequest_createsApprovedTransaction() {
        UUID internalTxId = UUID.randomUUID();
        String pan = "4111111111111111";
        BigDecimal amount = new BigDecimal("100.50");

        SwitchTransactionRecord result = service.processCardAuth(internalTxId, pan, amount);

        assertNotNull(result);
        assertEquals(MessageType.MT0100, result.mtType());
        assertEquals("00", result.isoResponseCode());
        assertEquals(SwitchStatus.APPROVED, result.status());
        assertTrue(result.switchReference().startsWith("PAYNET-"));
        verify(repository).save(any(SwitchTransactionRecord.class));
    }

    @Test
    void processCardAuth_roundsAmountToTwoDecimals() {
        UUID internalTxId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.556");

        SwitchTransactionRecord result = service.processCardAuth(internalTxId, "pan", amount);

        assertEquals(new BigDecimal("100.56"), result.amount());
    }

    @Test
    void processReversal_withValidRequest_createsReversedTransaction() {
        UUID originalTxId = UUID.randomUUID();
        String originalRef = "PAYNET-ABC12345";
        BigDecimal amount = new BigDecimal("50.00");

        SwitchTransactionRecord result = service.processReversal(originalTxId, originalRef, amount);

        assertNotNull(result);
        assertEquals(MessageType.MT0400, result.mtType());
        assertEquals("00", result.isoResponseCode());
        assertEquals(SwitchStatus.REVERSED, result.status());
        assertEquals(originalRef, result.originalReference());
        assertEquals(1, result.reversalCount());
        verify(repository).save(any(SwitchTransactionRecord.class));
    }

    @Test
    void processDuitNowTransfer_withValidRequest_createsSettledTransaction() {
        UUID internalTxId = UUID.randomUUID();
        String proxyType = "MSISDN";
        String proxyValue = "+601112345678";
        BigDecimal amount = new BigDecimal("200.00");

        SwitchTransactionRecord result = service.processDuitNowTransfer(internalTxId, proxyType, proxyValue, amount);

        assertNotNull(result);
        assertEquals(MessageType.ISO20022, result.mtType());
        assertEquals("ACSC", result.isoResponseCode());
        assertEquals(SwitchStatus.APPROVED, result.status());
        assertTrue(result.switchReference().startsWith("DN-"));
        verify(repository).save(any(SwitchTransactionRecord.class));
    }

    @Test
    void processDuitNowTransfer_roundsAmountToTwoDecimals() {
        UUID internalTxId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.999");

        SwitchTransactionRecord result = service.processDuitNowTransfer(internalTxId, "type", "value", amount);

        assertEquals(new BigDecimal("201.00"), result.amount());
    }
}
