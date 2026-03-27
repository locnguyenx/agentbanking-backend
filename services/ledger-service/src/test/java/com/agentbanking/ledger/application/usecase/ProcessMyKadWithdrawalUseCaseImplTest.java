package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.domain.model.TransactionResult;
import com.agentbanking.ledger.domain.port.in.ProcessMyKadWithdrawalUseCase;
import com.agentbanking.ledger.domain.port.out.IdempotencyCache;
import com.agentbanking.ledger.domain.port.out.RulesServicePort;
import com.agentbanking.ledger.domain.port.out.SwitchServicePort;
import com.agentbanking.ledger.domain.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessMyKadWithdrawalUseCaseImplTest {

    @Mock
    private LedgerService ledgerService;

    @Mock
    private RulesServicePort rulesService;

    @Mock
    private SwitchServicePort switchService;

    @Mock
    private IdempotencyCache idempotencyCache;

    @InjectMocks
    private ProcessMyKadWithdrawalUseCaseImpl processMyKadWithdrawalUseCase;

    private UUID agentId;
    private String idempotencyKey;
    private String customerMykad;
    private BigDecimal amount;
    private BigDecimal geofenceLat;
    private BigDecimal geofenceLng;

    @BeforeEach
    void setUp() {
        agentId = UUID.randomUUID();
        idempotencyKey = "test-key-123";
        customerMykad = "123456789012";
        amount = new BigDecimal("500.00");
        geofenceLat = new BigDecimal("3.1390");
        geofenceLng = new BigDecimal("101.6869");
    }

    @Test
    void shouldProcessMyKadWithdrawalSuccessfully() {
        // Arrange
        when(idempotencyCache.exists(idempotencyKey)).thenReturn(false);
        
        // Mock rules service responses
        var velocityResult = new RulesServicePort.VelocityCheckResult(true, null, null, null);
        when(rulesService.checkVelocity(customerMykad, "MYKAD_WITHDRAWAL", amount))
                .thenReturn(velocityResult);
        
        var feeResult = new RulesServicePort.FeeCheckResult(true, 
                new BigDecimal("1.00"), new BigDecimal("0.20"), new BigDecimal("0.80"));
        when(rulesService.calculateFee("MYKAD_WITHDRAWAL", "MICRO", amount))
                .thenReturn(feeResult);
        
        // Mock ledger service
        var agentFloat = new com.agentbanking.ledger.domain.model.AgentFloatRecord(
                UUID.randomUUID(), agentId, new BigDecimal("1000.00"), 
                BigDecimal.ZERO, "MYR", 1L, 
                new BigDecimal("3.1390"), new BigDecimal("101.6869"));
        when(ledgerService.getAgentFloat(agentId)).thenReturn(agentFloat);
        
        // Mock switch service
        var switchResult = new com.agentbanking.common.switchport.AuthorizationResult(true, "SWITCH-REF-123", null);
        when(switchService.authorizeWithdrawal(customerMykad, amount, agentId.toString()))
                .thenReturn(switchResult);
        
        // Act
        TransactionResult result = processMyKadWithdrawalUseCase.processMyKadWithdrawal(
                new ProcessMyKadWithdrawalUseCase.MyKadWithdrawalCommand(
                        agentId, amount, "MYR", idempotencyKey, customerMykad,
                        geofenceLat, geofenceLng
                )
        );

        // Assert
        assertEquals("COMPLETED", result.status());
        assertNotNull(result.transactionId());
        assertEquals(amount, result.amount());
        assertEquals(new BigDecimal("1.00"), result.customerFee());
        
        // Verify idempotency caching
        verify(idempotencyCache).save(eq(idempotencyKey), any(TransactionResult.class), eq(86400));
    }

    @Test
    void shouldReturnCachedResultForIdempotentRequest() {
        // Arrange
        var cachedResult = new TransactionResult("COMPLETED", UUID.randomUUID(), 
                new BigDecimal("500.00"), new BigDecimal("1.00"), "SWITCH-REF-123");
        when(idempotencyCache.exists(idempotencyKey)).thenReturn(true);
        when(idempotencyCache.get(idempotencyKey, TransactionResult.class))
                .thenReturn(cachedResult);

        // Act
        TransactionResult result = processMyKadWithdrawalUseCase.processMyKadWithdrawal(
                new ProcessMyKadWithdrawalUseCase.MyKadWithdrawalCommand(
                        agentId, amount, "MYR", idempotencyKey, customerMykad,
                        geofenceLat, geofenceLng
                )
        );

        // Assert
        assertEquals(cachedResult, result);
        // Verify no downstream calls were made
        verifyNoInteractions(ledgerService, rulesService, switchService);
    }

    @Test
    void shouldFailWhenInsufficientFloat() {
        // Arrange
        when(idempotencyCache.exists(idempotencyKey)).thenReturn(false);
        
        var velocityResult = new RulesServicePort.VelocityCheckResult(true, null, null, null);
        when(rulesService.checkVelocity(customerMykad, "MYKAD_WITHDRAWAL", amount))
                .thenReturn(velocityResult);
        
        var feeResult = new RulesServicePort.FeeCheckResult(true, 
                new BigDecimal("1.00"), new BigDecimal("0.20"), new BigDecimal("0.80"));
        when(rulesService.calculateFee("MYKAD_WITHDRAWAL", "MICRO", amount))
                .thenReturn(feeResult);
        
        // Mock agent with insufficient float
        var agentFloat = new com.agentbanking.ledger.domain.model.AgentFloatRecord(
                UUID.randomUUID(), agentId, new BigDecimal("100.00"), 
                BigDecimal.ZERO, "MYR", 1L, 
                new BigDecimal("3.1390"), new BigDecimal("101.6869"));
        when(ledgerService.getAgentFloat(agentId)).thenReturn(agentFloat);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            processMyKadWithdrawalUseCase.processMyKadWithdrawal(
                    new ProcessMyKadWithdrawalUseCase.MyKadWithdrawalCommand(
                            agentId, amount, "MYR", idempotencyKey, customerMykad,
                            geofenceLat, geofenceLng
                    )
            );
        });
    }
}